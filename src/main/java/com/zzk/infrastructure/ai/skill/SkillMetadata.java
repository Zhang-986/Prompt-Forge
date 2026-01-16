package com.zzk.infrastructure.ai.skill;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * Skill 元数据（Level 1）
 * 
 * 启动时从数据库加载到内存，用于意图匹配和 tools JSON 生成。
 * 不包含 instructions（Level 2），节省内存。
 * 
 * @author zzk
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SkillMetadata {

    /**
     * 技能唯一标识
     */
    private String name;

    /**
     * 显示名称
     */
    private String displayName;

    /**
     * 技能描述（给 LLM 看的）
     */
    private String description;

    /**
     * 触发关键词列表
     */
    private List<String> triggerKeywords;

    /**
     * 技能分类：CORE / DATA / CODE / CONTENT
     */
    private String category;

    /**
     * Spring Bean 名称
     */
    private String executorBean;

    /**
     * 参数 JSON Schema（已解析为 Map）
     */
    private Map<String, Object> parameterSchema;
}
