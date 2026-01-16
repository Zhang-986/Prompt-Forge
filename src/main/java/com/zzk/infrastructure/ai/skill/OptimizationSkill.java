package com.zzk.infrastructure.ai.skill;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;

/**
 * Prompt 优化技能
 * 
 * <p>
 * 应用 Chain-of-Thought、Few-Shot 等高级技巧优化 Prompt
 * 
 * @author zzk
 * @since 1.0.0
 */
@Slf4j
@Component("optimizationExecutor")
public class OptimizationSkill implements SkillExecutor {

    // ========== SkillExecutor 接口实现 ==========

    @Override
    public String getName() {
        return "optimization";
    }

    @Override
    public SkillResult execute(Map<String, Object> params, Map<String, Object> context) {
        String originalPrompt = (String) params.get("originalPrompt");
        String technique = (String) params.get("technique");
        if (originalPrompt == null || originalPrompt.isBlank()) {
            return SkillResult.error("原始 Prompt 不能为空");
        }
        if (technique == null || technique.isBlank()) {
            technique = "clarity"; // 默认优化技巧
        }
        String result = optimize(originalPrompt, technique);
        return SkillResult.success(result);
    }

    /**
     * 优化 Prompt
     * 
     * @param originalPrompt 原始 Prompt
     * @param technique      优化技巧 (cot/few-shot/clarity)
     * @return 优化后的 Prompt
     */
    @Tool(description = "应用高级技巧优化 Prompt 质量。支持 Chain-of-Thought (cot)、Few-Shot 示例 (few-shot)、清晰度优化 (clarity)。")
    public String optimize(String originalPrompt, String technique) {
        log.info("[OptimizationSkill] 优化 Prompt, 技巧: {}", technique);

        return switch (technique.toLowerCase()) {
            case "cot" -> applyCOT(originalPrompt);
            case "few-shot" -> applyFewShot(originalPrompt);
            case "clarity" -> applyClarity(originalPrompt);
            default -> "不支持的优化技巧: " + technique + "。请使用 cot, few-shot 或 clarity。";
        };
    }

    private String applyCOT(String prompt) {
        return """
                %s

                ---
                🧠 [Chain-of-Thought 优化已应用]

                请按以下步骤思考：
                1. 首先，分析问题的核心需求
                2. 然后，列出解决方案的关键步骤
                3. 接着，逐步执行每个步骤
                4. 最后，检查结果是否满足需求

                让我们一步一步来思考...
                """.formatted(prompt);
    }

    private String applyFewShot(String prompt) {
        return """
                %s

                ---
                📚 [Few-Shot 优化已应用]

                以下是一些示例：

                示例 1:
                输入: [示例输入1]
                输出: [示例输出1]

                示例 2:
                输入: [示例输入2]
                输出: [示例输出2]

                现在，请按照上述示例的模式处理新的输入。
                """.formatted(prompt);
    }

    private String applyClarity(String prompt) {
        return """
                %s

                ---
                ✨ [清晰度优化已应用]

                优化建议：
                - 使用更具体的动词（如 "生成" 而非 "做"）
                - 明确输出格式要求
                - 添加约束条件限制范围
                - 提供背景上下文
                """.formatted(prompt);
    }
}
