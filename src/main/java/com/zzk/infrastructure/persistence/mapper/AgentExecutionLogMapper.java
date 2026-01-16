package com.zzk.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zzk.infrastructure.persistence.po.AgentExecutionLogPO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

/**
 * Agent 执行日志 Mapper
 * 
 * @author zzk
 * @since 1.0.0
 */
@Mapper
public interface AgentExecutionLogMapper extends BaseMapper<AgentExecutionLogPO> {

        /**
         * 统计由 Skill 造成的 Token 消耗（按 Skill 分组）
         */
        @Select("SELECT executor_name, SUM(total_tokens) as total_tokens, COUNT(*) as call_count " +
                        "FROM agent_execution_logs " +
                        "GROUP BY executor_name " +
                        "ORDER BY total_tokens DESC")
        List<Map<String, Object>> statTokenUsageBySkill();

        /**
         * 统计 Skill 失败率
         */
        @Select("SELECT executor_name, " +
                        "SUM(CASE WHEN status = 'FAILURE' THEN 1 ELSE 0 END) as fail_count, " +
                        "COUNT(*) as total_count " +
                        "FROM agent_execution_logs " +
                        "WHERE status = 'FAILURE' " +
                        "GROUP BY executor_name")
        List<Map<String, Object>> statSkillFailureRate();
}
