package com.zzk.infrastructure.ai.skill.executors;

import com.zzk.domain.model.entity.UserModelConfig;
import com.zzk.domain.repository.UserModelConfigRepository;
import com.zzk.infrastructure.ai.factory.DynamicLlmClientFactory;
import com.zzk.infrastructure.ai.skill.core.SkillExecutor;
import com.zzk.infrastructure.ai.skill.core.SkillResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * Prompt 质量评估 Skill
 * 
 * <p>
 * 评估 Prompt 的质量，从多个维度给出评分和改进建议。
 * 这是 Agent 的"反思"能力体现 —— AI 可以自我评估生成的内容。
 * 
 * <p>
 * 评估维度：
 * <ul>
 * <li>清晰度 (Clarity) - 指令是否明确无歧义</li>
 * <li>完整性 (Completeness) - 上下文和约束是否充分</li>
 * <li>可执行性 (Executability) - AI 能否理解并执行</li>
 * <li>专业性 (Professionalism) - 是否使用恰当的专业术语</li>
 * </ul>
 * 
 * @author zzk
 * @since 1.0.0
 */
@Slf4j
@Component("evaluationExecutor")
@RequiredArgsConstructor
public class PromptEvaluatorSkill implements SkillExecutor {

    private final DynamicLlmClientFactory llmFactory;
    private final UserModelConfigRepository userConfigRepository;

    private static final String EVALUATION_SYSTEM_PROMPT = """
            你是一位专业的 Prompt Engineer，擅长评估 Prompt 质量。

            请从以下维度评估给定的 Prompt，每个维度给出 1-10 分，并提供具体的改进建议：

            ## 评估维度
            1. **清晰度 (Clarity)**: 指令是否明确无歧义？用户意图是否清楚？
            2. **完整性 (Completeness)**: 上下文信息是否充分？约束条件是否明确？
            3. **可执行性 (Executability)**: AI 能否理解并准确执行？是否有可操作的步骤？
            4. **专业性 (Professionalism)**: 是否使用恰当的术语？表达是否规范？

            ## 输出格式
            请使用以下 Markdown 格式输出：

            ### 📊 评估结果

            | 维度 | 评分 | 说明 |
            |------|------|------|
            | 清晰度 | X/10 | 简短说明 |
            | 完整性 | X/10 | 简短说明 |
            | 可执行性 | X/10 | 简短说明 |
            | 专业性 | X/10 | 简短说明 |

            **综合评分**: X/10

            ### 💡 改进建议
            1. 建议一
            2. 建议二
            3. ...

            ### ✨ 优化后的 Prompt（可选）
            如果评分低于 7 分，请提供优化后的版本。
            """;

    @Override
    public String getName() {
        return "evaluation";
    }

    @Override
    public SkillResult execute(Map<String, Object> params, Map<String, Object> context) {
        String prompt = (String) params.get("prompt");

        if (prompt == null || prompt.isBlank()) {
            return SkillResult.error("Prompt 内容不能为空");
        }

        log.info("[PromptEvaluatorSkill] 开始评估 Prompt，长度: {} 字符", prompt.length());

        try {
            // 获取用户的 AI 配置（使用系统默认或用户偏好）
            Long userId = getUserId(context);
            UserModelConfig modelConfig = getModelConfig(userId);

            if (modelConfig == null) {
                log.warn("[PromptEvaluatorSkill] 无法获取模型配置，使用规则评估");
                return SkillResult.success(ruleBasedEvaluation(prompt));
            }

            // 构建评估请求
            String userPrompt = "请评估以下 Prompt：\n\n```\n" + prompt + "\n```";
            String fullPrompt = EVALUATION_SYSTEM_PROMPT + "\n\n" + userPrompt;

            // 调用 AI 进行评估
            StringBuilder result = new StringBuilder();
            llmFactory.generateStream(modelConfig, fullPrompt)
                    .toIterable()
                    .forEach(result::append);

            String evaluation = result.toString().trim();
            log.info("[PromptEvaluatorSkill] 评估完成，结果长度: {} 字符", evaluation.length());

            return SkillResult.success(evaluation);

        } catch (Exception e) {
            log.error("[PromptEvaluatorSkill] 评估失败: {}", e.getMessage(), e);
            // 降级到规则评估
            return SkillResult.success(ruleBasedEvaluation(prompt));
        }
    }

    /**
     * 规则评估（降级方案）
     * 当无法调用 AI 时，使用简单规则进行评估
     */
    private String ruleBasedEvaluation(String prompt) {
        int length = prompt.length();
        int lines = prompt.split("\n").length;
        boolean hasRole = prompt.contains("你是") || prompt.contains("作为") || prompt.contains("扮演");
        boolean hasConstraint = prompt.contains("不要") || prompt.contains("必须") || prompt.contains("请确保");
        boolean hasFormat = prompt.contains("格式") || prompt.contains("输出") || prompt.contains("返回");
        boolean hasExample = prompt.contains("例如") || prompt.contains("示例") || prompt.contains("比如");

        int clarity = Math.min(10, 5 + (hasRole ? 2 : 0) + (length > 50 ? 1 : 0) + (lines > 3 ? 1 : 0));
        int completeness = Math.min(10, 4 + (hasConstraint ? 2 : 0) + (hasExample ? 2 : 0) + (length > 200 ? 2 : 0));
        int executability = Math.min(10, 5 + (hasFormat ? 2 : 0) + (hasConstraint ? 1 : 0) + (lines > 5 ? 2 : 0));
        int professionalism = Math.min(10, 5 + (hasRole ? 1 : 0) + (hasFormat ? 2 : 0) + (length > 100 ? 2 : 0));

        int overall = (clarity + completeness + executability + professionalism) / 4;

        StringBuilder sb = new StringBuilder();
        sb.append("### 📊 评估结果（规则评估）\n\n");
        sb.append("| 维度 | 评分 | 说明 |\n");
        sb.append("|------|------|------|\n");
        sb.append("| 清晰度 | ").append(clarity).append("/10 | ").append(hasRole ? "包含角色定义" : "缺少角色定义").append(" |\n");
        sb.append("| 完整性 | ").append(completeness).append("/10 | ").append(hasExample ? "包含示例" : "可添加示例")
                .append(" |\n");
        sb.append("| 可执行性 | ").append(executability).append("/10 | ").append(hasFormat ? "包含输出格式" : "缺少输出格式")
                .append(" |\n");
        sb.append("| 专业性 | ").append(professionalism).append("/10 | ").append(length > 100 ? "内容详尽" : "内容较简短")
                .append(" |\n");
        sb.append("\n**综合评分**: ").append(overall).append("/10\n\n");

        sb.append("### 💡 改进建议\n");
        if (!hasRole)
            sb.append("1. 添加明确的角色定义（如：\"你是一位...\"）\n");
        if (!hasConstraint)
            sb.append("2. 添加约束条件（如：\"不要...\"、\"必须...\"）\n");
        if (!hasFormat)
            sb.append("3. 明确输出格式要求\n");
        if (!hasExample)
            sb.append("4. 添加具体示例帮助理解\n");

        sb.append("\n> 💡 此评估基于规则匹配，如需更精准的评估，请确保 AI 服务可用。");

        return sb.toString();
    }

    private Long getUserId(Map<String, Object> context) {
        if (context != null && context.get("userId") instanceof Long) {
            return (Long) context.get("userId");
        }
        return 0L;
    }

    private UserModelConfig getModelConfig(Long userId) {
        try {
            List<UserModelConfig> configs = userConfigRepository.findEnabledByUserId(userId);
            if (!configs.isEmpty()) {
                return configs.get(0);
            }
            // 尝试获取系统默认配置
            List<UserModelConfig> systemConfigs = userConfigRepository.findEnabledByUserId(0L);
            return systemConfigs.isEmpty() ? null : systemConfigs.get(0);
        } catch (Exception e) {
            log.warn("[PromptEvaluatorSkill] 获取模型配置失败: {}", e.getMessage());
            return null;
        }
    }
}
