package com.zzk.domain.model.valueobject;

/**
 * Prompt Coach 对话阶段
 * 
 * <p>定义多轮对话引导的各个阶段，后端控制阶段流转。
 * 
 * @author zzk
 * @since 1.0.0
 */
public enum CoachPhase {

    /**
     * 目标澄清 - 明确用户想要实现什么
     */
    GOAL_CLARIFICATION("目标澄清", "明确用户想要实现什么目标"),

    /**
     * 场景定义 - 确定使用场景和上下文
     */
    SCENARIO_DEFINITION("场景定义", "确定使用场景、技术栈等上下文"),

    /**
     * 细节收集 - 收集具体需求细节
     */
    DETAIL_COLLECTION("细节收集", "收集功能需求、约束条件等细节"),

    /**
     * 格式偏好 - 确定输出格式要求
     */
    FORMAT_PREFERENCE("格式偏好", "确定期望的输出格式和风格"),

    /**
     * 生成 Prompt - 根据收集的信息生成最终 Prompt
     */
    PROMPT_GENERATION("生成Prompt", "根据收集的信息生成高质量Prompt"),

    /**
     * 迭代优化 - 用户反馈后进一步优化
     */
    ITERATION("迭代优化", "根据用户反馈进一步调整优化");

    private final String displayName;
    private final String description;

    CoachPhase(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }

    /**
     * 获取下一个阶段
     */
    public CoachPhase next() {
        CoachPhase[] phases = values();
        int currentIndex = this.ordinal();
        if (currentIndex < phases.length - 1) {
            return phases[currentIndex + 1];
        }
        return this; // 已经是最后阶段
    }

    /**
     * 是否是最终阶段
     */
    public boolean isFinal() {
        return this == PROMPT_GENERATION || this == ITERATION;
    }
}
