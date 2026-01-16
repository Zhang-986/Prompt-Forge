package com.zzk.infrastructure.persistence.po;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Agent 执行日志 PO
 * 
 * @author zzk
 * @since 1.0.0
 */
@Data
@Builder
@TableName("agent_execution_logs")
public class AgentExecutionLogPO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private String sessionId;

    private String traceId;

    private String executorName;

    private String actionType; // LLM_CHAT, SKILL_EXECUTION

    private String inputSummary;

    private String outputSummary;

    private LocalDateTime startTime;

    private LocalDateTime endTime;

    private Long durationMs;

    private String status; // SUCCESS, FAILURE

    private String errorMessage;

    private String model;

    private Integer promptTokens;

    private Integer completionTokens;

    private Integer totalTokens;

    @com.fasterxml.jackson.annotation.JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
