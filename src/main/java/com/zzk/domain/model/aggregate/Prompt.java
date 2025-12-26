package com.zzk.domain.model.aggregate;

import com.zzk.domain.model.entity.PromptVersion;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Prompt 聚合根
 * 
 * <p>作为 DDD 聚合根，维护 Prompt 的完整性边界。
 * 包含 HEAD 指针指向最新版本，类似 Git 的设计。
 * 
 * @author zzk
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Prompt {

    /**
     * Prompt ID
     */
    private Long id;

    /**
     * Prompt 名称
     */
    private String name;

    /**
     * 描述
     */
    private String description;

    /**
     * 所属工作空间 ID
     */
    private Long workspaceId;

    /**
     * 最新版本 ID (HEAD 指针)
     */
    private Long latestVersionId;

    /**
     * 最新版本号 (用于前端显示)
     */
    private Integer latestVersionNumber;

    /**
     * 创建者 ID
     */
    private Long creatorId;

    /**
     * 是否公开
     */
    private Boolean isPublic;

    /**
     * 状态: 0-已删除, 1-正常
     */
    private Integer status;

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
     * 更新 HEAD 指针到新版本
     * 
     * @param newVersion 新版本
     */
    public void updateHead(PromptVersion newVersion) {
        if (newVersion == null || newVersion.getId() == null) {
            throw new IllegalArgumentException("新版本不能为空");
        }
        this.latestVersionId = newVersion.getId();
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 检查用户是否有权限编辑
     * 
     * @param userId 用户 ID
     * @return 是否有权限
     */
    public boolean canEdit(Long userId) {
        return this.creatorId.equals(userId) || this.isPublic;
    }

    /**
     * 软删除
     */
    public void softDelete() {
        this.status = 0;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 是否已删除
     */
    public boolean isDeleted() {
        return this.status != null && this.status == 0;
    }
}
