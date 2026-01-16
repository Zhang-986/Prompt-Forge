package com.zzk.infrastructure.ai.skill;

import java.util.Map;

/**
 * Skill 执行器接口
 * 
 * 每个 Agent Skill 对应一个实现类，注册为 Spring Bean。
 * 命名规范：xxxExecutor，如 promptLibraryExecutor、webSearchExecutor
 * 
 * @author zzk
 * @since 1.0.0
 */
public interface SkillExecutor {

    /**
     * 获取技能名称
     * 必须与数据库 agent_skills.name 字段一致
     * 
     * @return 技能名称，如 "prompt-library"
     */
    String getName();

    /**
     * 执行技能
     * 
     * @param params  LLM 传入的参数（已从 JSON 解析为 Map）
     * @param context 执行上下文，包含 userId, workspaceId, sessionId 等
     * @return 执行结果
     */
    SkillResult execute(Map<String, Object> params, Map<String, Object> context);
}
