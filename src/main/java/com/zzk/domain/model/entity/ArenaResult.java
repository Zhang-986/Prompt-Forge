package com.zzk.domain.model.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 竞技场结果实体
 * 
 * <p>记录单个模型在一次竞技中的输出结果
 * 
 * @author zzk
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ArenaResult {

    /**
     * 结果 ID
     */
    private Long id;

    /**
     * 会话 ID
     */
    private Long sessionId;

    /**
     * 模型标识 (gpt-4, deepseek, claude)
     */
    private String modelId;

    /**
     * 生成的内容
     */
    private String content;

    /**
     * 消耗的 Token 数
     */
    private Integer tokensUsed;

    /**
     * 响应延迟 (毫秒)
     */
    private Integer latencyMs;

    /**
     * 状态: SUCCESS/FAILED/TIMEOUT
     */
    private String status;

    /**
     * 错误信息
     */
    private String errorMessage;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    // ==================== 领域行为 ====================

    /**
     * 标记为成功
     */
    public void markSuccess(String content, Integer tokensUsed, Integer latencyMs) {
        this.content = content;
        this.tokensUsed = tokensUsed;
        this.latencyMs = latencyMs;
        this.status = "SUCCESS";
    }

    /**
     * 标记为失败
     */
    public void markFailed(String errorMessage) {
        this.status = "FAILED";
        this.errorMessage = errorMessage;
    }

    /**
     * 标记为超时
     */
    public void markTimeout() {
        this.status = "TIMEOUT";
        this.errorMessage = "模型响应超时";
    }

    /**
     * 是否成功
     */
    public boolean isSuccess() {
        return "SUCCESS".equals(this.status);
    }
}
