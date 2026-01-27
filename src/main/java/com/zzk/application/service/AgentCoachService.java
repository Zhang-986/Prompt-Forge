package com.zzk.application.service;

import com.zzk.domain.model.entity.PromptCoachSession;
import com.zzk.domain.model.entity.UserModelConfig;
import com.zzk.domain.model.entity.UserPreference;
import com.zzk.domain.model.valueobject.CoachPhase;
import com.zzk.domain.repository.PromptCoachSessionRepository;
import com.zzk.domain.repository.UserModelConfigRepository;
import com.zzk.domain.repository.UserPreferenceRepository;
import com.zzk.infrastructure.ai.client.FunctionCallingClient;
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
    private final SkillRegistry skillRegistry;
    private final FunctionCallingClient functionCallingClient;
    private final SkillSelectorService skillSelectorService;

    public AgentCoachService(PromptCoachSessionRepository sessionRepository,
                             UserPreferenceRepository preferenceRepository,
                             UserModelConfigRepository userConfigRepository,
                             DynamicLlmClientFactory llmFactory,
                             SkillRegistry skillRegistry,
                             FunctionCallingClient functionCallingClient,
                             SkillSelectorService skillSelectorService) {
        this.sessionRepository = sessionRepository;
        this.preferenceRepository = preferenceRepository;
        this.userConfigRepository = userConfigRepository;
        this.llmFactory = llmFactory;
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
                .provider(modelConfig.getProvider())
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
     * 开始 Agent 会话（原方法保留，向后兼容）
     * <p>
     * 使用 LLM 自动推断需要的 Skills 进行 Function Calling
     *
     * @param userId       用户ID
     * @param initialInput 初始输入
     * @param provider     AI 模型提供商
     */
    public PromptCoachSession startAgentSession(Long userId, String initialInput, String provider) {
        // 1. 获取用户模型配置
        UserModelConfig modelConfig = selectModel(userId, provider);

        // 2. 创建会话
        String sessionId = UUID.randomUUID().toString().replace("-", "");
        PromptCoachSession session = PromptCoachSession.builder()
                .sessionId(sessionId)
                .userId(userId)
                .provider(modelConfig.getProvider())
                .currentPhase(CoachPhase.GOAL_CLARIFICATION)
                .build();

        // 3. 添加用户初始输入
        session.addUserMessage(initialInput);

        // 4. LLM 自动推断需要的 Skills
        // 智能推断
        List<String> inferredSkillNames = skillSelectorService.inferRequiredSkills(initialInput, modelConfig);
        // 按需加载
        List<SkillMetadata> selectedSkills = skillRegistry.getSkillsByNames(inferredSkillNames);
        log.info("[startAgentSession] LLM 自动推断需要 {} 个 Skill: {}",
                selectedSkills.size(),
                inferredSkillNames);

        // 5. 构建消息列表
        String systemPrompt = buildAgentSystemPrompt(session);
        List<Map<String, Object>> messages = new ArrayList<>();
        messages.add(Map.of("role", "system", "content", systemPrompt));
        messages.add(Map.of("role", "user", "content", initialInput));

        // 6. 构建执行上下文
        Map<String, Object> context = Map.of(
                "userId", userId,
                "sessionId", sessionId,
                "phase", session.getCurrentPhase().name());

        // 7. 调用 AI（使用手搓 Function Calling）
        String aiResponse;
        if (skillRegistry.hasSkills() && !selectedSkills.isEmpty()) {
            // 全功能模式：使用 FunctionCallingClient
            aiResponse = functionCallingClient.chat(modelConfig, messages, selectedSkills, context);
        } else {
            // 降级模式：纯文本生成
            log.info("[startAgentSession] 无可用 Skill，使用降级模式");
            StringBuilder result = new StringBuilder();
            llmFactory.generateStream(modelConfig, buildAgentPrompt(session))
                    .toIterable()
                    .forEach(result::append);
            aiResponse = result.toString();
        }

        // 8. 更新会话状态
        session.addAssistantMessage(aiResponse);
        try {
            parseAndUpdateSession(session, aiResponse);
        } catch (Exception e) {
            log.warn("[startAgentSession] 解析状态失败", e);
        }

        // 9. 保存会话
        sessionRepository.save(session);
        return session;
    }

    /**
     * Agent 对话（带工具调用）
     * <p>
     * 使用 LLM 自动推断需要的 Skills 进行 Function Calling
     * 支持首次对话（处理 initialInput）和后续对话
     *
     * @param sessionId   会话ID
     * @param userMessage 用户消息（首次对话传 null，使用会话中的 initialInput）
     */
    public Flux<String> agentChat(String sessionId, String userMessage) {
        PromptCoachSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new BusinessException("会话不存在或已过期"));

        if (session.isMaxTurnsReached()) {
            return Flux.just("抱歉，对话轮数已达上限。请根据当前生成的 Prompt 进行调整，或开启新会话。");
        }

        // 处理首次对话：如果是空消息且会话只有 1 条用户消息（initialInput），直接使用它
        boolean isFirstChat = (userMessage == null || userMessage.isBlank()) 
                && session.getHistory().size() == 1;
        
        String actualMessage;
        if (isFirstChat) {
            // 首次对话：使用 initialInput
            actualMessage = session.getHistory().get(0).content();
            log.info("[agentChat] 首次对话，使用 initialInput: {}", actualMessage);
        } else {
            // 后续对话：使用新消息
            actualMessage = userMessage;
            session.addUserMessage(userMessage);
        }

        UserModelConfig modelConfig = selectModel(session.getUserId(), session.getProvider());

        // LLM 自动推断需要的 Skills（核心改动：从手动选择改为自动推断）
        List<String> inferredSkillNames = skillSelectorService.inferRequiredSkills(actualMessage, modelConfig);
        List<SkillMetadata> selectedSkills = skillRegistry.getSkillsByNames(inferredSkillNames);
        log.info("[agentChat] LLM 自动推断需要 {} 个 Skill: {}",
                selectedSkills.size(),
                inferredSkillNames);

        // 构建消息列表
        String systemPrompt = buildAgentSystemPrompt(session);
        List<Map<String, Object>> messages = new ArrayList<>();
        messages.add(Map.of("role", "system", "content", systemPrompt));
        
        // 首次对话：只发送 initialInput；后续对话：发送完整历史
        if (isFirstChat) {
            messages.add(Map.of("role", "user", "content", actualMessage));
        } else {
            messages.add(Map.of("role", "user", "content", buildUserMessageWithHistory(session)));
        }

        // 构建执行上下文
        Map<String, Object> context = Map.of(
                "userId", session.getUserId(),
                "sessionId", sessionId,
                "phase", session.getCurrentPhase().name());

        return Flux.create(sink -> {
            CompletableFuture.runAsync(() -> {
                try {
                    String response;
                    if (skillRegistry.hasSkills() && !selectedSkills.isEmpty()) {
                        // 全功能模式：使用 FunctionCallingClient，并传递事件回调
                        response = functionCallingClient.chat(modelConfig, messages, selectedSkills, context,
                                (event) -> {
                                    // event 格式: "event: TYPE\ndata: CONTENT\n\n"
                                    // 我们把它作为特殊消息推送给前端
                                    // 前端收到后会是: data: event: TYPE\ndata: CONTENT\n\n\n\n
                                    sink.next(event);
                                });
                    } else {
                        // 降级模式：纯文本生成
                        log.info("[agentChat] 无可用 Skill，使用降级模式");
                        StringBuilder result = new StringBuilder();
                        llmFactory.generateStream(modelConfig, buildAgentPrompt(session))
                                .toIterable()
                                .forEach(chunk -> {
                                    result.append(chunk);
                                    // 流式返回文本内容
                                    sink.next(chunk);
                                });
                        response = result.toString();
                    }

                    // 最终保存
                    session.addAssistantMessage(response);
                    try {
                        parseAndUpdateSession(session, response);
                    } catch (Exception e) {
                        log.warn("[agentChat] 解析状态失败", e);
                    }
                    sessionRepository.save(session);
                    log.info("[agentChat] 对话完成，长度: {}", response.length());

                    // 发送结束信号 (Agent模式下 FunctionCallingClient 返回的是完整文本，所以这里发最后一次)
                    // 如果是降级模式，前面已经流式发过chunk了。
                    // 为了统一，如果 response 不为空且是 Agent 模式（sink还没发过文本内容，只发过事件），
                    // 我们应该把 response 发出去。
                    // 但 FunctionCallingClient 返回的是最终完整回复。
                    // 我们约定：事件以 event: 开头，普通回复直接发文本。

                    if (skillRegistry.hasSkills() && !selectedSkills.isEmpty()) {
                        sink.next(response);
                    }

                    sink.complete();
                } catch (Exception e) {
                    log.error("[agentChat] 对话失败: {}", e.getMessage(), e);
                    sink.error(e);
                }
            });
        });
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
                
                注意：使用中文回复。
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
        String[] markers = {"---最终Prompt---", "```", "【最终Prompt】"};
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
