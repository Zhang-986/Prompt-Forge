package com.zzk.infrastructure.persistence.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Agent Skill 持久化对象
 * 
 * 基于 Claude Agent Skills 架构设计，支持三层渐进式加载：
 * - Level 1: name, description, triggerKeywords (启动时加载)
 * - Level 2: instructions (匹配时加载)
 * - Level 3: executorBean (执行时调用)
 * 
 * @author zzk
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("agent_skills")
public class AgentSkillPO {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 技能唯一标识，如 "prompt-library"
     */
    private String name;

    /**
     * 显示名称，如 "Prompt 库搜索"
     */
    private String displayName;

    /**
     * 技能描述（给 LLM 理解用）
     */
    private String description;

    /**
     * 触发关键词 JSON 数组，如 ["参考", "模板", "搜索"]
     */
    private String triggerKeywords;

    /**
     * 技能分类：CORE / DATA / CODE / CONTENT
     */
    private String category;

    /**
     * 详细使用指令（Markdown 格式）
     */
    private String instructions;

    /**
     * Spring Bean 名称，如 "promptLibraryExecutor"
     */
    private String executorBean;

    /**
     * 参数 JSON Schema（用于 Function Calling）
     */
    private String parameterSchema;

    /**
     * 是否启用
     */
    private Integer enabled;

    /**
     * 排序顺序
     */
    private Integer sortOrder;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
