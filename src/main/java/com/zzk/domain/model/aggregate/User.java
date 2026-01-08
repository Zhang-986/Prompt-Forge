package com.zzk.domain.model.aggregate;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 用户聚合根
 * 
 * @author zzk
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User {

    /**
     * 用户 ID
     */
    private Long id;

    /**
     * 用户名
     */
    private String username;

    /**
     * 昵称
     */
    private String nickname;

    /**
     * 密码 (BCrypt 加密)
     */
    private String password;

    /**
     * 邮箱
     */
    private String email;

    /**
     * 头像 URL
     */
    private String avatar;

    /**
     * 角色: ADMIN/MEMBER/VIEWER
     */
    private String role;

    /**
     * 状态: 0-禁用, 1-启用
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
     * 是否是管理员
     */
    public boolean isAdmin() {
        return "ADMIN".equals(this.role);
    }

    /**
     * 是否已禁用
     */
    public boolean isDisabled() {
        return this.status != null && this.status == 0;
    }

    /**
     * 禁用用户
     */
    public void disable() {
        this.status = 0;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 启用用户
     */
    public void enable() {
        this.status = 1;
        this.updatedAt = LocalDateTime.now();
    }
}
