package com.zzk.interfaces.controller;

import com.zzk.infrastructure.persistence.mapper.AgentExecutionLogMapper;
import com.zzk.infrastructure.persistence.po.AgentExecutionLogPO;
import com.zzk.interfaces.dto.response.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Agent 监控控制器
 * 
 * <p>
 * 提供 Agent 执行状态、Token 消耗和技能健康度的监控数据
 * 
 * @author zzk
 * @since 1.0.0
 */
@RestController
@RequestMapping("/api/monitor")
@RequiredArgsConstructor
@Tag(name = "Agent Monitor", description = "Agent 监控与统计")
public class AgentMonitorController {

    private final AgentExecutionLogMapper logMapper;

    @GetMapping("/stats/token")
    @Operation(summary = "Token 消耗统计", description = "按 Skill 统计 Token 消耗量")
    public Result<List<Map<String, Object>>> getTokenUsageStats() {
        return Result.success(logMapper.statTokenUsageBySkill());
    }

    @GetMapping("/stats/failure")
    @Operation(summary = "技能失败率统计", description = "统计各 Skill 的调用失败次数")
    public Result<List<Map<String, Object>>> getFailureStats() {
        return Result.success(logMapper.statSkillFailureRate());
    }

    @GetMapping("/logs")
    @Operation(summary = "最近执行日志", description = "获取最近 50 条 Agent 执行日志")
    public Result<List<AgentExecutionLogPO>> getRecentLogs() {
        // 简单实现：直接查最新的 50 条
        // 实际生产中应支持分页过滤
        List<AgentExecutionLogPO> logs = logMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<AgentExecutionLogPO>()
                        .orderByDesc(AgentExecutionLogPO::getId)
                        .last("LIMIT 50"));
        return Result.success(logs);
    }
}
