package com.zzk.application.service;

import com.zzk.domain.model.entity.PromptCoachSession;
import com.zzk.domain.model.entity.UserModelConfig;
import com.zzk.domain.model.valueobject.CoachPhase;
import com.zzk.domain.repository.PromptCoachSessionRepository;
import com.zzk.domain.repository.UserModelConfigRepository;
import com.zzk.domain.repository.UserPreferenceRepository;
import com.zzk.infrastructure.ai.factory.DynamicLlmClientFactory;
import com.zzk.domain.model.entity.UserPreference;
import com.zzk.infrastructure.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.UUID;

/**
 * Agent 增强版 Coach 服务
 * 
 * <p>
 * 复用 DynamicLlmClientFactory 实现流式输出，
 * 支持所有已适配的 Provider（Google, Claude, OpenAI 兼容等）。
 * Tool Calling 通过 System Prompt 嵌入工具描述实现。
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
    private final List<ToolCallback> agentTools;

    public AgentCoachService(
            PromptCoachSessionRepository sessionRepository,
            UserPreferenceRepository preferenceRepository,
            UserModelConfigRepository userConfigRepository,
            DynamicLlmClientFactory llmFactory,
            List<ToolCallback> agentTools) {
        this.sessionRepository = sessionRepository;
        this.preferenceRepository = preferenceRepository;
        this.userConfigRepository = userConfigRepository;
        this.llmFactory = llmFactory;
        this.agentTools = agentTools;

        log.info("AgentCoachService 初始化完成，已加载 {} 个工具", agentTools.size());
        agentTools.forEach(tool -> log.info("  - 工具: {}", tool.getToolDefinition().name()));
    }

    /**
     * 开始新的 Agent Coach 会话（流式输出）
     */
    public Flux<String> startAgentSessionStream(Long userId, String initialInput, String provider) {
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

        // 4. 保存会话（先保存，获取 sessionId）
        sessionRepository.save(session);

        // 5. 构建完整 Prompt（包含工具描述）
        String fullPrompt = buildAgentPrompt(session);

        StringBuilder fullResponse = new StringBuilder();

        log.info("创建 Agent Coach 会话: sessionId={}, userId={}, provider={}",
                sessionId, userId, modelConfig.getProvider());

        // 6. 使用 DynamicLlmClientFactory 流式生成
        return llmFactory.generateStream(modelConfig, fullPrompt)
                .doOnNext(chunk -> fullResponse.append(chunk))
                .doOnComplete(() -> {
                    String response = fullResponse.toString();
                    session.addAssistantMessage(response);

                    try {
                        // 解析并更新状态
                        parseAndUpdateSession(session, response);
                    } catch (Exception e) {
                        log.warn("解析状态失败", e);
                    }

                    sessionRepository.save(session);
                    log.info("Agent 首轮回复完成，长度: {}", response.length());
                })
                .doOnError(e -> log.error("Agent 首轮回复失败: {}", e.getMessage(), e));
    }

    /**
     * 开始新的 Agent Coach 会话（同步版本，返回 PromptCoachSession）
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

        // 4. 构建完整 Prompt
        String fullPrompt = buildAgentPrompt(session);

        // 5. 同步调用（阻塞等待流完成）
        StringBuilder result = new StringBuilder();
        llmFactory.generateStream(modelConfig, fullPrompt)
                .toIterable()
                .forEach(result::append);

        String aiResponse = result.toString().trim();
        session.addAssistantMessage(aiResponse);

        // 6. 保存会话
        sessionRepository.save(session);

        log.info("创建 Agent Coach 会话（同步）: sessionId={}, userId={}, tools={}",
                sessionId, userId, agentTools.size());
        return session;
    }

    /**
     * Agent 对话（流式输出）
     */
    public Flux<String> agentChat(String sessionId, String userMessage) {
        PromptCoachSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new BusinessException("会话不存在或已过期"));

        if (session.isMaxTurnsReached()) {
            return Flux.just("抱歉，对话轮数已达上限。请根据当前生成的 Prompt 进行调整，或开启新会话。");
        }

        session.addUserMessage(userMessage);

        UserModelConfig modelConfig = selectModel(session.getUserId(), session.getProvider());
        String fullPrompt = buildAgentPrompt(session);

        StringBuilder fullResponse = new StringBuilder();

        // 使用 DynamicLlmClientFactory 流式生成
        return llmFactory.generateStream(modelConfig, fullPrompt)
                .doOnNext(chunk -> fullResponse.append(chunk))
                .doOnComplete(() -> {
                    String response = fullResponse.toString();
                    session.addAssistantMessage(response);

                    try {
                        // 解析并更新状态
                        parseAndUpdateSession(session, response);
                    } catch (Exception e) {
                        log.warn("解析状态失败", e);
                    }

                    sessionRepository.save(session);
                    log.info("Agent 流式回复完成，长度: {}", response.length());
                })
                .doOnError(e -> log.error("Agent 流式回复失败: {}", e.getMessage(), e));
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
     * 构建 Agent 系统提示词（包含工具说明）
     */
    private String buildAgentSystemPrompt(PromptCoachSession session) {
        StringBuilder toolsDescription = new StringBuilder();
        toolsDescription.append("你可以使用以下工具来帮助用户：\n");
        for (ToolCallback tool : agentTools) {
            toolsDescription.append(String.format("- %s: %s\n",
                    tool.getToolDefinition().name(),
                    tool.getToolDefinition().description()));
        }

        return """
                你是一位具备超能力的精英产品专家（Product Expert），拥有多种工具来帮助用户。

                %s

                当你需要获取最新信息、分析代码仓库或评估 Prompt 质量时，请主动调用相应工具。

                当前阶段: %s (%s)
                已收集信息:
                %s

                引导策略：
                1. **深度挖掘**：追问到底，至少 5-8 轮对话深度。
                2. **提供方案**：给出 2-4 个方案让用户选择，而不是开放式提问。
                3. **主动使用工具**：如果用户提到技术栈、GitHub 链接，主动调用相应工具获取信息。
                4. **最终输出**：在 PROMPT_GENERATION 阶段，用 "---最终Prompt---" 标记输出最终 Prompt。

                注意：使用中文回复。
                """.formatted(
                toolsDescription.toString(),
                session.getCurrentPhase().name(),
                session.getCurrentPhase().getDescription(),
                session.getFormattedExtractedInfo());
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
        // 检测是否生成了最终 Prompt
        if (response.contains("---最终Prompt---") || response.contains("最终Prompt") ||
                response.contains("生成的Prompt") || response.contains("最终的Prompt")) {
            session.setCurrentPhase(CoachPhase.PROMPT_GENERATION);

            // 提取最终 Prompt
            String prompt = extractFinalPrompt(response);
            if (prompt != null) {
                session.setGeneratedPrompt(prompt);
            }
        }

        // 检测阶段转换信号
        if (response.contains("技术栈") || response.contains("使用什么技术")) {
            if (session.getCurrentPhase() == CoachPhase.GOAL_CLARIFICATION) {
                session.setCurrentPhase(CoachPhase.SCENARIO_DEFINITION);
            }
        }
        if (response.contains("具体功能") || response.contains("核心功能")) {
            if (session.getCurrentPhase() == CoachPhase.SCENARIO_DEFINITION) {
                session.setCurrentPhase(CoachPhase.DETAIL_COLLECTION);
            }
        }
        if (response.contains("输出格式") || response.contains("格式要求")) {
            if (session.getCurrentPhase() == CoachPhase.DETAIL_COLLECTION) {
                session.setCurrentPhase(CoachPhase.FORMAT_PREFERENCE);
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
