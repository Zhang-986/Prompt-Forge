package com.zzk.interfaces.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 管理员用户详情 DTO
 * 
 * <p>包含用户基本信息及其所属工作空间列表
 * 
 * @author zzk
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminUserDTO {
    
    private Long id;
    private String username;
    private String email;
    private String avatar;
    private String role;
    private Integer status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    /**
     * 用户所属工作空间列表
     */
    private List<WorkspaceInfo> workspaces;
    
    /**
     * 用户创建的 Prompt 数量
     */
    private Integer promptCount;
    
    /**
     * 用户竞技场会话数
     */
    private Integer arenaSessionCount;
    
    /**
     * 工作空间简要信息
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class WorkspaceInfo {
        private Long id;
        private String name;
        private String role; // 用户在该工作空间的角色
        private Boolean isOwner; // 是否为该工作空间的所有者
    }
}
