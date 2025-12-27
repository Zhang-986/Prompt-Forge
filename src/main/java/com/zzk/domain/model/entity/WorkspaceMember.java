package com.zzk.domain.model.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 工作空间成员实体
 * 
 * @author zzk
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkspaceMember {

    /**
     * ID
     */
    private Long id;

    /**
     * 工作空间 ID
     */
    private Long workspaceId;

    /**
     * 用户 ID
     */
    private Long userId;

    /**
     * 用户名（用于展示）
     */
    private String username;

    /**
     * 角色: ADMIN/MEMBER/VIEWER
     */
    private String role;

    /**
     * 加入时间
     */
    private LocalDateTime createdAt;

    // ==================== 领域行为 ====================

    /**
     * 检查是否是管理员
     */
    public boolean isAdmin() {
        return "ADMIN".equals(this.role);
    }

    /**
     * 检查是否有写权限
     */
    public boolean canWrite() {
        return "ADMIN".equals(this.role) || "MEMBER".equals(this.role);
    }
}
