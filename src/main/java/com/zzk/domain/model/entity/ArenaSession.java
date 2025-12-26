package com.zzk.domain.model.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 竞技场会话实体
 * 
 * <p>记录一次多模型竞技的完整信息
 * 
 * @author zzk
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ArenaSession {

    /**
     * 会话 ID
     */
    private Long id;

    /**
     * 使用的 Prompt 版本 ID
     */
    private Long promptVersionId;

    /**
     * 渲染后的最终 Prompt
     */
    private String finalPrompt;

    /**
     * 本次使用的变量值 (JSON)
     */
    private String variables;

    /**
     * 参与的模型列表 (JSON)
     */
    private String models;

    /**
     * 状态: RUNNING/COMPLETED/FAILED
     */
    private String status;

    /**
     * 创建者 ID
     */
    private Long creatorId;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 完成时间
     */
    private LocalDateTime completedAt;

    // ==================== 领域行为 ====================

    /**
     * 标记为完成
     */
    public void markCompleted() {
        this.status = "COMPLETED";
        this.completedAt = LocalDateTime.now();
    }

    /**
     * 标记为失败
     */
    public void markFailed() {
        this.status = "FAILED";
        this.completedAt = LocalDateTime.now();
    }

    /**
     * 是否正在运行
     */
    public boolean isRunning() {
        return "RUNNING".equals(this.status);
    }

    /**
     * 是否已完成
     */
    public boolean isCompleted() {
        return "COMPLETED".equals(this.status);
    }
}
