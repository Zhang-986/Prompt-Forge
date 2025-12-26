package com.zzk.domain.model.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 工作空间实体
 * 
 * <p>用于多租户隔离，每个 Prompt 归属于一个工作空间
 * 
 * @author zzk
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Workspace {

    /**
     * 工作空间 ID
     */
    private Long id;

    /**
     * 工作空间名称
     */
    private String name;

    /**
     * 描述
     */
    private String description;

    /**
     * 所有者 ID
     */
    private Long ownerId;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;

    // ==================== 领域行为 ====================

    /**
     * 检查用户是否是所有者
     */
    public boolean isOwner(Long userId) {
        return this.ownerId != null && this.ownerId.equals(userId);
    }
}
