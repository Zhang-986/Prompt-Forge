package com.zzk.infrastructure.ai.skill;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;
import lombok.extern.slf4j.Slf4j;

/**
 * Prompt 结构化技能
 * 
 * <p>
 * 将用户的原始想法格式化为标准结构的 Prompt
 * 
 * @author zzk
 * @since 1.0.0
 */
@Slf4j
@Component
public class StructurizationSkill {

    /**
     * 将原始内容格式化为标准 Prompt 结构
     * 
     * @param rawContent 原始 Prompt 内容
     * @return 结构化后的 Prompt
     */
    @Tool(description = "将原始 Prompt 内容格式化为标准结构（角色、约束、工作流、输出格式）。当用户的 Prompt 缺乏结构或需要整理时使用。")
    public String structurize(String rawContent) {
        log.info("[StructurizationSkill] 结构化 Prompt, 长度: {}", rawContent.length());

        // 应用标准结构化模板
        return """
                # 结构化 Prompt

                ## 角色定义 (Role)
                你是一个专业的 [待填充] 专家。

                ## 任务目标 (Goal)
                %s

                ## 约束条件 (Constraints)
                - 约束1: [待补充]
                - 约束2: [待补充]

                ## 工作流程 (Workflow)
                1. 首先...
                2. 然后...
                3. 最后...

                ## 输出格式 (Output Format)
                请按以下格式输出：
                ```
                [格式说明]
                ```

                ---
                原始内容已被结构化，请根据实际需求补充完善。
                """.formatted(rawContent);
    }
}
