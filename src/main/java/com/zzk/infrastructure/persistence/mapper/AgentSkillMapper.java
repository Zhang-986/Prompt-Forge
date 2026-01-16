package com.zzk.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zzk.infrastructure.persistence.po.AgentSkillPO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * Agent Skill Mapper
 * 
 * @author zzk
 * @since 1.0.0
 */
@Mapper
public interface AgentSkillMapper extends BaseMapper<AgentSkillPO> {

    /**
     * 查询所有启用的 Skill
     */
    @Select("SELECT * FROM agent_skills WHERE enabled = 1 ORDER BY sort_order")
    List<AgentSkillPO> selectEnabled();

    /**
     * 根据名称查询 Skill
     */
    @Select("SELECT * FROM agent_skills WHERE name = #{name} AND enabled = 1")
    AgentSkillPO selectByName(String name);

    /**
     * 查询所有启用的 Skill（仅 Level 1 字段，用于启动时加载）
     */
    @Select("SELECT id, name, display_name, description, trigger_keywords, category, executor_bean, parameter_schema " +
            "FROM agent_skills WHERE enabled = 1 ORDER BY sort_order")
    List<AgentSkillPO> selectMetadataOnly();
}
