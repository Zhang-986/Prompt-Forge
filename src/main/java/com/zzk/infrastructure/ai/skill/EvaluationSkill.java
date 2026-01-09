package com.zzk.infrastructure.ai.skill;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;
import lombok.extern.slf4j.Slf4j;

/**
 * Prompt 评估技能
 * 
 * <p>
 * 评估 Prompt 质量并给出改进建议
 * 
 * @author zzk
 * @since 1.0.0
 */
@Slf4j
@Component
public class EvaluationSkill {

    /**
     * 评估 Prompt 质量
     * 
     * @param prompt 待评估的 Prompt
     * @return 评估报告
     */
    @Tool(description = "评估 Prompt 的质量，从清晰度、完整性、可执行性等维度打分，并给出改进建议。")
    public String evaluate(String prompt) {
        log.info("[EvaluationSkill] 评估 Prompt, 长度: {}", prompt.length());

        // 简单的规则评分
        int clarityScore = evaluateClarity(prompt);
        int completenessScore = evaluateCompleteness(prompt);
        int specificityScore = evaluateSpecificity(prompt);
        int overallScore = (clarityScore + completenessScore + specificityScore) / 3;

        return """
                # Prompt 质量评估报告

                ## 总体评分: %d/100

                ### 维度分析:

                | 维度 | 分数 | 说明 |
                |------|------|------|
                | 清晰度 | %d/100 | %s |
                | 完整性 | %d/100 | %s |
                | 具体性 | %d/100 | %s |

                ### 改进建议:
                %s

                ### 优化后参考:
                [建议根据上述分析进行优化]
                """.formatted(
                overallScore,
                clarityScore, getClarityComment(clarityScore),
                completenessScore, getCompletenessComment(completenessScore),
                specificityScore, getSpecificityComment(specificityScore),
                generateSuggestions(clarityScore, completenessScore, specificityScore));
    }

    private int evaluateClarity(String prompt) {
        int score = 60;
        if (prompt.contains("请") || prompt.contains("帮我"))
            score += 10;
        if (prompt.length() > 50)
            score += 10;
        if (prompt.contains("\n"))
            score += 10;
        return Math.min(score, 100);
    }

    private int evaluateCompleteness(String prompt) {
        int score = 50;
        if (prompt.contains("角色") || prompt.contains("Role"))
            score += 15;
        if (prompt.contains("输出") || prompt.contains("格式"))
            score += 15;
        if (prompt.contains("约束") || prompt.contains("限制"))
            score += 10;
        if (prompt.contains("示例") || prompt.contains("Example"))
            score += 10;
        return Math.min(score, 100);
    }

    private int evaluateSpecificity(String prompt) {
        int score = 50;
        if (prompt.matches(".*\\d+.*"))
            score += 10; // 包含数字
        if (prompt.contains("必须") || prompt.contains("不要"))
            score += 15;
        if (prompt.length() > 100)
            score += 15;
        if (prompt.contains("```"))
            score += 10; // 包含代码块
        return Math.min(score, 100);
    }

    private String getClarityComment(int score) {
        if (score >= 80)
            return "表达清晰，意图明确";
        if (score >= 60)
            return "基本清晰，可进一步优化";
        return "表达模糊，需要更明确的指令";
    }

    private String getCompletenessComment(int score) {
        if (score >= 80)
            return "结构完整，要素齐全";
        if (score >= 60)
            return "基本完整，缺少部分要素";
        return "结构不完整，建议添加角色/输出格式";
    }

    private String getSpecificityComment(int score) {
        if (score >= 80)
            return "描述具体，约束明确";
        if (score >= 60)
            return "较为具体，可添加更多细节";
        return "过于笼统，需要更具体的要求";
    }

    private String generateSuggestions(int clarity, int completeness, int specificity) {
        StringBuilder sb = new StringBuilder();
        if (clarity < 70)
            sb.append("- 使用更明确的动词和指令\n");
        if (completeness < 70)
            sb.append("- 添加角色定义和输出格式要求\n");
        if (specificity < 70)
            sb.append("- 增加具体的约束条件和数量限制\n");
        if (sb.isEmpty())
            sb.append("- Prompt 质量良好，可考虑添加 Few-Shot 示例进一步提升\n");
        return sb.toString();
    }
}
