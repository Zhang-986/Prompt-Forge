package com.zzk.application.service;

import com.zzk.domain.model.entity.PromptCoachSession;
import com.zzk.domain.model.entity.UserModelConfig;
import com.zzk.domain.model.entity.UserPreference;
import com.zzk.domain.model.valueobject.CoachPhase;
import com.zzk.domain.repository.PromptCoachSessionRepository;
import com.zzk.domain.repository.UserModelConfigRepository;
import com.zzk.domain.repository.UserPreferenceRepository;
import com.zzk.infrastructure.ai.client.FunctionCallingClient;
import com.zzk.infrastructure.ai.client.GoAiGatewayGrpcClient;
import com.zzk.infrastructure.ai.factory.DynamicLlmClientFactory;
import com.zzk.infrastructure.ai.skill.core.SkillMetadata;
import com.zzk.infrastructure.ai.skill.registry.SkillRegistry;
import com.zzk.infrastructure.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Agent 增强版 Coach 服务
 *
 * <p>
 * 基于 Claude Agent Skills 架构重构：
 * - 三层渐进式加载（Metadata → Instructions → Execute）
 * - 手搓 Function Calling 支持所有 AI 厂商
 * - 按需加载 Skill 节省 Token
 *
 * @author zzk
 * @since 1.0.0
 */
@Slf4j
@Service
public class AgentCoachService {

    private final PromptCoachSessionRepository sessionRepository;
    private final UserPreferenceRepository preferenceRepository;
    private final UserModelConfigRepository userConfigRepository;
    private final DynamicLlmClientFactory llmFactory;
    private final GoAiGatewayGrpcClient goGrpcClient;
    private final SkillRegistry skillRegistry;
    private final FunctionCallingClient functionCallingClient;
    private final SkillSelectorService skillSelectorService;

    public AgentCoachService(PromptCoachSessionRepository sessionRepository,
            UserPreferenceRepository preferenceRepository,
            UserModelConfigRepository userConfigRepository,
            DynamicLlmClientFactory llmFactory,
            GoAiGatewayGrpcClient goGrpcClient,
            SkillRegistry skillRegistry,
            FunctionCallingClient functionCallingClient,
            SkillSelectorService skillSelectorService) {
        this.sessionRepository = sessionRepository;
        this.preferenceRepository = preferenceRepository;
        this.userConfigRepository = userConfigRepository;
        this.llmFactory = llmFactory;
        this.goGrpcClient = goGrpcClient;
        this.skillRegistry = skillRegistry;
        this.functionCallingClient = functionCallingClient;
        this.skillSelectorService = skillSelectorService;

        log.info("[AgentCoachService] 初始化完成，SkillRegistry 已加载 {} 个技能",
                skillRegistry.getAllSkillNames().size());
    }

    /**
     * 快速创建会话（不调用 LLM）
     * <p>
     * 优化首次加载性能：只创建会话对象，LLM 调用延迟到 chat 接口
     *
     * @param userId       用户ID
     * @param initialInput 初始输入
     * @param provider     AI 模型提供商
     * @return 会话对象
     */
    public PromptCoachSession createSession(Long userId, String initialInput, String provider) {
        // 1. 获取用户模型配置
        UserModelConfig modelConfig = selectModel(userId, provider);

        // 2. 创建会话
        String sessionId = UUID.randomUUID().toString().replace("-", "");
        PromptCoachSession session = PromptCoachSession.builder()
                .sessionId(sessionId)
                .userId(userId)
                .provider(provider) // 保存完整的 provider:model 信息 ✅
                .currentPhase(CoachPhase.GOAL_CLARIFICATION)
                .build();

        // 3. 添加用户初始输入（但不调用 LLM）
        session.addUserMessage(initialInput);

        // 4. 保存会话
        sessionRepository.save(session);

        log.info("[createSession] 快速创建会话: sessionId={}, userId={}", sessionId, userId);
        return session;
    }

    /**
     * Agent 对话（带工具调用）
     * 
     * <p>
     * 性能优化版：
     * - 快速加载会话并返回 Flux（< 50ms）
     * - 所有耗时操作（Skill 推断、LLM 调用）在异步线程中执行
     * - 不阻塞 Tomcat 主线程，提高并发能力
     * 
     * <p>
     * 使用 LLM 自动推断需要的 Skills 进行 Function Calling
     * 支持首次对话（处理 initialInput）和后续对话
     *
     * @param sessionId   会话ID
     * @param userMessage 用户消息（首次对话传 null，使用会话中的 initialInput）
     * @return 响应式流，实时推送 AI 回复和工具执行进度
     */
    public Flux<String> agentChat(String sessionId, String userMessage) {
        long startTime = System.currentTimeMillis();
        String threadName = Thread.currentThread().getName();
        log.info("┏━━ [{}] 阶段1: Tomcat线程接收请求", threadName);

        // ====== 阶段1：快速加载会话（同步，< 50ms）======
        PromptCoachSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new BusinessException("会话不存在或已过期"));

        if (session.isMaxTurnsReached()) {
            return Flux.just("抱歉，对话轮数已达上限。请根据当前生成的 Prompt 进行调整，或开启新会话。");
        }

        // 处理用户消息（判断是首次对话还是后续对话）
        boolean isFirstChat = isFirstChat(userMessage, session);
        if (!isFirstChat) {
            session.addUserMessage(userMessage);
        }

        log.info("┗━━ [{}] 阶段1完成: 创建Flux准备返回 (耗时 {}ms)",
                threadName, System.currentTimeMillis() - startTime);

        // ====== 阶段2：创建异步流（立即返回，不阻塞 Tomcat 线程）======
        return Flux.create(sink -> {
            CompletableFuture.runAsync(() -> {
                String asyncThread = Thread.currentThread().getName();
                long asyncStart = System.currentTimeMillis();
                log.info("┏━━ [{}] 阶段2: 异步线程接手业务逻辑", asyncThread);

                try {
                    // 所有耗时操作都在这个异步线程中执行
                    executeAgentChat(session, userMessage, sessionId, isFirstChat, sink);

                    log.info("┗━━ [{}] 阶段2完成: 业务逻辑执行完毕 (耗时 {}ms)",
                            asyncThread, System.currentTimeMillis() - asyncStart);
                } catch (Exception e) {
                    log.error("[agentChat] 对话失败: {}", e.getMessage(), e);
                    sink.error(e);
                }
            });
        });
        // Tomcat 线程在这里立即返回，可以处理其他请求 ✅
    }

    /**
     * 执行 Agent 对话的核心逻辑（在异步线程中执行）
     * 
     * @param session     会话对象
     * @param userMessage 用户消息
     * @param sessionId   会话ID
     * @param isFirstChat 是否首次对话
     * @param sink        Flux 发射器，用于推送数据
     */
    private void executeAgentChat(PromptCoachSession session, String userMessage,
            String sessionId, boolean isFirstChat,
            reactor.core.publisher.FluxSink<String> sink) {
        // ====== 步骤1：获取实际消息内容 ======
        String actualMessage = getActualMessage(session, userMessage, isFirstChat);

        // ====== 步骤2：选择 AI 模型 ======
        UserModelConfig modelConfig = selectModel(session.getUserId(), session.getProvider());

        // ====== 步骤3：推断所需 Skill（耗时操作，但在异步线程中执行）======
        List<String> inferredSkillNames = skillSelectorService.inferRequiredSkills(actualMessage, modelConfig);
        List<SkillMetadata> selectedSkills = skillRegistry.getSkillsByNames(inferredSkillNames);
        log.info("[agentChat] LLM 自动推断需要 {} 个 Skill: {}", selectedSkills.size(), inferredSkillNames);

        // ====== 步骤4：构建消息列表 ======
        List<Map<String, Object>> messages = buildMessages(session, actualMessage, isFirstChat);

        // ====== 步骤5：构建执行上下文 ======
        Map<String, Object> context = buildContext(session, sessionId);

        // ====== 步骤6：执行对话（全功能模式或降级模式）======
        String response = executeChat(modelConfig, messages, selectedSkills, context, session, sink);

        // ====== 步骤7：保存会话状态 ======
        saveSessionResult(session, response);

        // ====== 步骤8：推送最终结果并结束流 ======
        String currentThread = Thread.currentThread().getName();
        if (skillRegistry.hasSkills() && !selectedSkills.isEmpty()) {
            log.info("    [{}] 步骤8: 推送最终回复 ({} 字符)", currentThread, response.length());
            // 全功能模式：推送最终完整回复
            sink.next(response);
        }
        // 降级模式已经在 executeChat 中流式推送过了

        log.info("    [{}] 步骤9: 调用 sink.complete() 结束流", currentThread);
        sink.complete();
        log.info("[agentChat] 对话完成，总长度: {} 字符", response.length());
    }

    /**
     * 判断是否为首次对话
     */
    private boolean isFirstChat(String userMessage, PromptCoachSession session) {
        return (userMessage == null || userMessage.isBlank()) && session.getHistory().size() == 1;
    }

    /**
     * 获取实际要发送给 AI 的消息内容
     */
    private String getActualMessage(PromptCoachSession session, String userMessage, boolean isFirstChat) {
        if (isFirstChat) {
            String initialInput = session.getHistory().get(0).content();
            log.info("[agentChat] 首次对话，使用 initialInput: {}", initialInput);
            return initialInput;
        } else {
            return userMessage;
        }
    }

    /**
     * 构建发送给 AI 的消息列表
     */
    private List<Map<String, Object>> buildMessages(PromptCoachSession session,
            String actualMessage,
            boolean isFirstChat) {
        List<Map<String, Object>> messages = new ArrayList<>();

        // 添加系统提示词
        String systemPrompt = buildAgentSystemPrompt(session);
        messages.add(Map.of("role", "system", "content", systemPrompt));

        // 添加用户消息
        if (isFirstChat) {
            // 首次对话：只发送 initialInput
            messages.add(Map.of("role", "user", "content", actualMessage));
        } else {
            // 后续对话：发送完整历史
            messages.add(Map.of("role", "user", "content", buildUserMessageWithHistory(session)));
        }

        return messages;
    }

    /**
     * 构建执行上下文
     */
    private Map<String, Object> buildContext(PromptCoachSession session, String sessionId) {
        return Map.of(
                "userId", session.getUserId(),
                "sessionId", sessionId,
                "phase", session.getCurrentPhase().name());
    }

    /**
     * 执行对话（根据是否有 Skill 选择全功能模式或降级模式）
     * 
     * @return AI 的完整回复文本
     */
    private String executeChat(UserModelConfig modelConfig,
            List<Map<String, Object>> messages,
            List<SkillMetadata> selectedSkills,
            Map<String, Object> context,
            PromptCoachSession session,
            reactor.core.publisher.FluxSink<String> sink) {
        if (skillRegistry.hasSkills() && !selectedSkills.isEmpty()) {
            // 全功能模式：使用 Function Calling
            return executeFunctionCallingMode(modelConfig, messages, selectedSkills, context, sink);
        } else {
            // 降级模式：纯文本生成
            return executeFallbackMode(modelConfig, session, sink);
        }
    }

    /**
     * 全功能模式：使用 Function Calling，支持工具调用
     */
    private String executeFunctionCallingMode(UserModelConfig modelConfig,
            List<Map<String, Object>> messages,
            List<SkillMetadata> selectedSkills,
            Map<String, Object> context,
            reactor.core.publisher.FluxSink<String> sink) {
        return functionCallingClient.chat(modelConfig, messages, selectedSkills, context, (event) -> {
            // 实时推送工具执行进度事件
            // 事件格式：__SSE_EVENT__:THOUGHT:正在思考...
            // __SSE_EVENT__:TOOL_START:开始执行工具
            // __SSE_EVENT__:TOOL_END:工具执行完成
            sink.next(event);
        });
    }

    /**
     * 降级模式：纯文本流式生成（无工具调用能力）
     * 通过 gRPC 调用 Go AI Gateway 获取 AI 流式结果
     */
    private String executeFallbackMode(UserModelConfig modelConfig,
            PromptCoachSession session,
            reactor.core.publisher.FluxSink<String> sink) {
        String threadName = Thread.currentThread().getName();
        log.info("    [{}] 降级模式：通过 gRPC 调用 Go AI Gateway", threadName);

        StringBuilder result = new StringBuilder();
        goGrpcClient.generateStream(modelConfig.getId(), buildAgentPrompt(session))
                .toIterable()
                .forEach(chunk -> {
                    result.append(chunk);
                    sink.next(chunk); // 流式推送每个文本块
                });

        log.info("    [{}] 降级模式完成（via gRPC），总计 {} 字符", threadName, result.length());
        return result.toString();
    }

    /**
     * 保存会话结果（包括 AI 回复和状态更新）
     */
    private void saveSessionResult(PromptCoachSession session, String response) {
        // 添加 AI 回复到历史记录
        session.addAssistantMessage(response);

        // 解析并更新会话状态（检测 "---初版Prompt---" 等标记）
        try {
            parseAndUpdateSession(session, response);
        } catch (Exception e) {
            log.warn("[agentChat] 解析状态失败", e);
        }

        // 持久化到数据库
        sessionRepository.save(session);
    }

    /**
     * 构建 Agent Prompt（包含系统提示词 + 工具描述 + 对话历史）
     */
    private String buildAgentPrompt(PromptCoachSession session) {
        StringBuilder sb = new StringBuilder();

        // 系统提示词
        sb.append(buildAgentSystemPrompt(session));
        sb.append("\n\n");

        // 对话历史
        sb.append("对话历史:\n");
        sb.append(session.getFormattedHistory());
        sb.append("\n请根据用户最新的回复，继续引导或生成最终 Prompt。");

        return sb.toString();
    }

    /**
     * 构建 Agent 系统提示词
     * 工具信息由 Spring AI ChatClient 自动注入，不再手动拼接
     */
    private String buildAgentSystemPrompt(PromptCoachSession session) {
        return """
                你是一位 Prompt 工程专家，帮助用户快速生成高质量的 AI Prompt。

                当你需要获取最新信息、分析代码仓库时，请主动调用相应工具。

                当前阶段: %s
                对话轮数: %d
                已收集信息:
                %s

                **核心策略：先给出可用的 Prompt，再根据反馈优化**

                工作流程：
                1. **首轮对话**：根据用户描述，直接生成一个"初版 Prompt"（用 ---初版Prompt--- 标记）
                   - 不要问太多问题，先给用户一个可用的东西
                   - 初版可以不完美，但要能用

                2. **后续对话**：根据用户反馈进行优化
                   - 如果用户说"挺好/可以"，确认是否需要调整，如不需要则输出最终版
                   - 如果用户提出具体修改意见，针对性调整
                   - 如果用户表示不满意但没说具体问题，追问 1-2 个关键点

                3. **最终输出**：当用户确认满意后，用 ---最终Prompt--- 标记输出

                输出格式示例：
                ```
                根据你的描述，我为你生成了初版 Prompt：

                ---初版Prompt---
                [你生成的 Prompt 内容]
                ---

                如果需要调整，请告诉我：
                - 想修改哪些地方？
                - 有没有漏掉的要求？
                ```

                **重要规则：**
                - 使用中文回复
                - 直接输出最终答案，不要输出你的思考过程
                - 不要输出英文的推理步骤（如 "The user says..."）
                - 不要输出中文的自我分析（如 "好的，用户让我..."）
                - 如果需要调用工具，直接调用即可，不要先描述你的计划
                """.formatted(
                session.getCurrentPhase().getDescription(),
                session.getHistory().size() / 2, // 对话轮数
                session.getFormattedExtractedInfo());
    }

    /**
     * 构建包含对话历史的用户消息
     */
    private String buildUserMessageWithHistory(PromptCoachSession session) {
        StringBuilder sb = new StringBuilder();
        sb.append("对话历史:\n");
        sb.append(session.getFormattedHistory());
        sb.append("\n请根据用户最新的回复，继续引导或生成最终 Prompt。");
        return sb.toString();
    }

    /**
     * 获取会话状态
     */
    public PromptCoachSession getSession(String sessionId) {
        return sessionRepository.findById(sessionId)
                .orElseThrow(() -> new BusinessException("会话不存在或已过期"));
    }

    /**
     * 确认并保存最终 Prompt
     *
     * @param sessionId        会话ID
     * @param promptTemplateId 关联的模板ID（新版本将保存到此模板）
     * @return 生成的 Prompt 内容
     */
    public String confirmAndSave(String sessionId, Long promptTemplateId) {
        PromptCoachSession session = getSession(sessionId);

        if (session.getGeneratedPrompt() == null || session.getGeneratedPrompt().isBlank()) {
            throw new BusinessException("尚未生成最终 Prompt，请继续对话");
        }

        session.setPromptTemplateId(promptTemplateId);
        sessionRepository.save(session);

        log.info("确认保存 Prompt: sessionId={}, promptTemplateId={}", sessionId, promptTemplateId);
        return session.getGeneratedPrompt();
    }

    /**
     * 解析 AI 回复并更新会话状态
     */
    private void parseAndUpdateSession(PromptCoachSession session, String response) {
        // 检测初版 Prompt（用于记录进度，但不触发"保存"按钮）
        if (response.contains("---初版Prompt---")) {
            session.setCurrentPhase(CoachPhase.DETAIL_COLLECTION); // 进入细化阶段
            log.info("[parseAndUpdateSession] 检测到初版 Prompt，进入细化阶段");
        }

        // 检测最终 Prompt（用户确认满意后才触发）
        if (response.contains("---最终Prompt---") || response.contains("【最终Prompt】")) {
            String prompt = extractFinalPrompt(response);
            if (prompt != null && prompt.length() > 50) {
                session.setCurrentPhase(CoachPhase.PROMPT_GENERATION);
                session.setGeneratedPrompt(prompt);
                log.info("[parseAndUpdateSession] 检测到最终 Prompt，长度: {}", prompt.length());
            }
        }
    }

    /**
     * 从回复中提取最终 Prompt
     */
    private String extractFinalPrompt(String response) {
        // 尝试提取标记后的内容
        String[] markers = { "---最终Prompt---", "```", "【最终Prompt】" };
        for (String marker : markers) {
            int startIndex = response.indexOf(marker);
            if (startIndex != -1) {
                String afterMarker = response.substring(startIndex + marker.length()).trim();
                // 找到结束位置
                int endIndex = afterMarker.indexOf("---");
                if (endIndex == -1)
                    endIndex = afterMarker.indexOf("```");
                if (endIndex == -1)
                    endIndex = afterMarker.length();
                return afterMarker.substring(0, endIndex).trim();
            }
        }
        return null;
    }

    /**
     * 选择 AI 模型
     * 复用 PromptCoachAppService 的逻辑
     */
    private UserModelConfig selectModel(Long userId, String providerOrModelId) {
        List<UserModelConfig> configs = userConfigRepository.findEnabledByUserId(userId);
        if (configs.isEmpty()) {
            throw new BusinessException("请先在设置中配置至少一个 AI 模型");
        }

        if (providerOrModelId != null && !providerOrModelId.isBlank()) {
            // 检查是否包含具体模型 (format: provider:model)
            String targetProvider;
            String targetModel = null;

            if (providerOrModelId.contains(":")) {
                String[] parts = providerOrModelId.split(":", 2);
                targetProvider = parts[0];
                targetModel = parts[1];
            } else {
                targetProvider = providerOrModelId;
            }

            String finalTargetProvider = targetProvider;
            String finalTargetModel = targetModel;

            UserModelConfig config = configs.stream()
                    .filter(c -> c.getProvider().equalsIgnoreCase(finalTargetProvider))
                    .findFirst()
                    .orElseThrow(() -> new BusinessException("未找到指定的模型配置: " + finalTargetProvider));

            // 如果指定了具体模型，覆盖配置中的模型名
            if (finalTargetModel != null && !finalTargetModel.isEmpty()) {
                log.info("[selectModel] 使用指定模型: provider={}, model={}", finalTargetProvider, finalTargetModel);
                return UserModelConfig.builder()
                        .id(config.getId())
                        .userId(config.getUserId())
                        .provider(config.getProvider())
                        .apiKey(config.getApiKey())
                        .baseUrl(config.getBaseUrl())
                        .modelName(finalTargetModel) // 使用指定的具体模型
                        .enabled(config.getEnabled())
                        .availableModels(config.getAvailableModels())
                        .build();
            }
            log.info("[selectModel] 使用 provider 默认模型: {}", config.getProvider());
            return config;
        }

        // 自动选择（优先使用用户偏好）
        UserPreference pref = preferenceRepository.findByUserId(userId).orElse(null);
        if (pref != null && pref.getPreferredProvider() != null) {
            String preferredProvider = pref.getPreferredProvider();
            return configs.stream()
                    .filter(c -> c.getProvider().equalsIgnoreCase(preferredProvider))
                    .findFirst()
                    .orElse(configs.get(0));
        }

        log.info("[selectModel] 使用默认第一个模型: {}", configs.get(0).getProvider());
        return configs.get(0);
    }
}
