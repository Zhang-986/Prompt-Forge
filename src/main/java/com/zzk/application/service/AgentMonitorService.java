package com.zzk.application.service;

import com.zzk.infrastructure.persistence.mapper.AgentExecutionLogMapper;
import com.zzk.infrastructure.persistence.po.AgentExecutionLogPO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

/**
 * Agent 监控服务
 * 
 * <p>
 * 负责异步记录 Agent 和 Skill 的执行日志、Token 消耗和健康状态
 * 
 * @author zzk
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AgentMonitorService {

    private final AgentExecutionLogMapper logMapper;

    /**
     * 记录 LLM 调用日志（包含 Token 消耗）
     */
    @Async
    public void logLlmCall(Long userId, String sessionId, String model,
            LocalDateTime startTime, String status,
            int promptTokens, int completionTokens, String error) {
        LocalDateTime endTime = LocalDateTime.now();
        long duration = ChronoUnit.MILLIS.between(startTime, endTime);

        AgentExecutionLogPO logPO = AgentExecutionLogPO.builder()
                .userId(userId)
                .sessionId(sessionId)
                .executorName("LLM_CORE")
                .actionType("LLM_CHAT")
                .model(model)
                .startTime(startTime)
                .endTime(endTime)
                .durationMs(duration)
                .status(status)
                .errorMessage(error)
                .promptTokens(promptTokens)
                .completionTokens(completionTokens)
                .totalTokens(promptTokens + completionTokens)
                .build();

        logMapper.insert(logPO);
        log.debug("[AgentMonitor] 已记录 LLM 调用: {} ms, {} tokens, status={}", duration, logPO.getTotalTokens(), status);
    }

    /**
     * 记录 Skill 执行日志
     */
    @Async
    public void logSkillExecution(Long userId, String sessionId, String skillName,
            LocalDateTime startTime, String status, String error,
            String inputSummary, String outputSummary) {
        LocalDateTime endTime = LocalDateTime.now();
        long duration = ChronoUnit.MILLIS.between(startTime, endTime);

        AgentExecutionLogPO logPO = AgentExecutionLogPO.builder()
                .userId(userId)
                .sessionId(sessionId)
                .executorName(skillName)
                .actionType("SKILL_EXECUTION")
                .startTime(startTime)
                .endTime(endTime)
                .durationMs(duration)
                .status(status)
                .errorMessage(error)
                .inputSummary(truncate(inputSummary))
                .outputSummary(truncate(outputSummary))
                .build();

        logMapper.insert(logPO);
        log.debug("[AgentMonitor] 已记录 Skill 执行: {}, {} ms, status={}", skillName, duration, status);
    }

    private String truncate(String s) {
        if (s == null)
            return null;
        return s.length() > 500 ? s.substring(0, 500) + "..." : s;
    }
}
