package com.zzk.application.service;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.zzk.domain.model.entity.PromptCoachSession;
import com.zzk.domain.model.entity.UserModelConfig;
import com.zzk.domain.model.entity.UserPreference;
import com.zzk.domain.model.valueobject.CoachPhase;
import com.zzk.domain.model.valueobject.DialogTurn;
import com.zzk.domain.repository.PromptCoachSessionRepository;
import com.zzk.domain.repository.UserModelConfigRepository;
import com.zzk.domain.repository.UserPreferenceRepository;
import com.zzk.infrastructure.ai.factory.DynamicLlmClientFactory;
import com.zzk.infrastructure.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Prompt Coach 应用服务
 * 
 * <p>实现多轮对话式 Prompt 引导优化：
 * <ul>
 *   <li>通过 3-5 轮引导性提问帮助用户明确需求</li>
 *   <li>后端控制大阶段，AI 负责具体提问</li>
 *   <li>收集用户偏好构建画像</li>
 * </ul>
 * 
 * @author zzk
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PromptCoachAppService {

    private final PromptCoachSessionRepository sessionRepository;
    private final UserPreferenceRepository preferenceRepository;
    private final UserModelConfigRepository userConfigRepository;
    private final DynamicLlmClientFactory llmFactory;

    /**
     * 开始新的 Coach 会话
     * 
     * @param userId 用户ID
     * @param initialInput 初始输入（用户想法）
     * @param provider AI 模型提供商（可选）
     * @return 会话信息和首轮 AI 回复
     */
    public PromptCoachSession startSession(Long userId, String initialInput, String provider) {
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
        
        // 4. 生成首轮 AI 回复（同步）
        String aiResponse = generateCoachResponse(session, modelConfig);
        session.addAssistantMessage(aiResponse);
        
        // 5. 保存会话
        sessionRepository.save(session);
        
        // 6. 更新用户画像
        updateUserPreference(userId, initialInput);
        
        log.info("创建 Coach 会话: sessionId={}, userId={}, provider={}", sessionId, userId, modelConfig.getProvider());
        return session;
    }

    /**
     * 发送消息并获取流式回复
     * 
     * @param sessionId 会话ID
     * @param userMessage 用户消息
     * @return AI 回复的流
     */
    public Flux<String> chat(String sessionId, String userMessage) {
        // 1. 获取会话
        PromptCoachSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new BusinessException("会话不存在或已过期"));
        
        // 2. 检查是否达到最大轮数
        if (session.isMaxTurnsReached()) {
            return Flux.just("抱歉，对话轮数已达上限。请根据当前生成的 Prompt 进行调整，或开启新会话。");
        }
        
        // 3. 添加用户消息
        session.addUserMessage(userMessage);
        
        // 4. 获取模型配置
        UserModelConfig modelConfig = selectModel(session.getUserId(), session.getProvider());
        
        // 5. 生成 AI 回复（流式）
        String systemPrompt = buildCoachSystemPrompt(session);
        String userPrompt = buildUserPrompt(session);
        
        StringBuilder fullResponse = new StringBuilder();
        
        return llmFactory.generateStream(modelConfig, systemPrompt + "\n\n" + userPrompt)
                .doOnNext(chunk -> fullResponse.append(chunk))
                .doOnComplete(() -> {
                    // 保存 AI 回复
                    String response = fullResponse.toString();
                    session.addAssistantMessage(response);
                    
                    // 尝试解析 AI 返回的结构化信息
                    parseAndUpdateSession(session, response);
                    
                    // 保存会话
                    sessionRepository.save(session);
                    
                    // 更新用户画像
                    updateUserPreference(session.getUserId(), userMessage);
                })
                .doOnError(e -> log.error("Coach 对话失败: sessionId={}", sessionId, e));
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
     * @param sessionId 会话ID
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

    // ==================== 私有方法 ====================

    /**
     * 选择 AI 模型
     */
    private UserModelConfig selectModel(Long userId, String provider) {
        List<UserModelConfig> configs = userConfigRepository.findEnabledByUserId(userId);
        if (configs.isEmpty()) {
            throw new BusinessException("请先在设置中配置至少一个 AI 模型");
        }
        
        if (provider != null && !provider.isBlank()) {
            return configs.stream()
                    .filter(c -> c.getProvider().equalsIgnoreCase(provider))
                    .findFirst()
                    .orElseThrow(() -> new BusinessException("未找到指定的模型配置: " + provider));
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
        
        return configs.get(0);
    }

    /**
     * 生成 Coach 回复（同步版本，用于首轮）
     */
    private String generateCoachResponse(PromptCoachSession session, UserModelConfig modelConfig) {
        String systemPrompt = buildCoachSystemPrompt(session);
        String userPrompt = buildUserPrompt(session);
        
        StringBuilder result = new StringBuilder();
        llmFactory.generateStream(modelConfig, systemPrompt + "\n\n" + userPrompt)
                .toIterable()
                .forEach(result::append);
        
        return result.toString().trim();
    }

    /**
     * 构建 Coach 系统提示词
     */
    private String buildCoachSystemPrompt(PromptCoachSession session) {
        return """
                你是一位专业的 Prompt 工程师教练。你的任务是通过引导性提问，帮助用户明确他们的需求，最终生成高质量的 Prompt。
                
                当前阶段: %s (%s)
                已收集信息:
                %s
                
                引导原则:
                1. 每次只问 1-2 个问题，不要一次问太多
                2. 提供 2-4 个选项让用户选择，降低用户思考成本
                3. 如果用户回答模糊，温和地追问细节
                4. 当信息足够时，主动进入下一阶段
                5. 如果是最终阶段，直接生成完整的 Prompt
                
                阶段说明:
                - GOAL_CLARIFICATION: 明确用户想要实现什么目标
                - SCENARIO_DEFINITION: 确定技术栈、使用场景等上下文
                - DETAIL_COLLECTION: 收集具体功能需求、约束条件
                - FORMAT_PREFERENCE: 确定输出格式要求
                - PROMPT_GENERATION: 根据收集的信息生成最终 Prompt
                
                注意：
                - 如果是 PROMPT_GENERATION 阶段，请直接输出生成的 Prompt，并用 "---最终Prompt---" 标记
                - 保持友好、专业的语气
                - 使用中文回复
                """.formatted(
                session.getCurrentPhase().name(),
                session.getCurrentPhase().getDescription(),
                session.getFormattedExtractedInfo()
        );
    }

    /**
     * 构建用户提示词（包含对话历史）
     */
    private String buildUserPrompt(PromptCoachSession session) {
        StringBuilder sb = new StringBuilder();
        sb.append("对话历史:\n");
        sb.append(session.getFormattedHistory());
        sb.append("\n请根据用户最新的回复，继续引导或生成最终 Prompt。");
        return sb.toString();
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
        String[] markers = {"---最终Prompt---", "```", "【最终Prompt】"};
        for (String marker : markers) {
            int startIndex = response.indexOf(marker);
            if (startIndex != -1) {
                String afterMarker = response.substring(startIndex + marker.length()).trim();
                // 找到结束位置
                int endIndex = afterMarker.indexOf("---");
                if (endIndex == -1) endIndex = afterMarker.indexOf("```");
                if (endIndex == -1) endIndex = afterMarker.length();
                return afterMarker.substring(0, endIndex).trim();
            }
        }
        return null;
    }

    /**
     * 更新用户画像
     */
    private void updateUserPreference(Long userId, String userInput) {
        try {
            UserPreference pref = preferenceRepository.getOrCreate(userId);
            pref.incrementSessionCount();
            
            // 简单的关键词检测
            if (userInput.toLowerCase().contains("java") || userInput.contains("spring")) {
                pref.incrementTopicFrequency("Java");
                pref.updateTechStack("Java/Spring");
            }
            if (userInput.toLowerCase().contains("python") || userInput.contains("django")) {
                pref.incrementTopicFrequency("Python");
            }
            if (userInput.contains("管理系统") || userInput.contains("后台")) {
                pref.addDomain("管理系统");
            }
            if (userInput.contains("API") || userInput.contains("接口")) {
                pref.addDomain("API开发");
            }
            
            preferenceRepository.save(pref);
        } catch (Exception e) {
            log.warn("更新用户画像失败: userId={}", userId, e);
        }
    }
}
