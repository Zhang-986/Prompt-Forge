package com.zzk.interfaces.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 管理员端 Prompt 列表 DTO
 * 
 * @author zzk
 * @since 1.0.0
 */
@Data
@Builder
public class AdminPromptDTO {
    private Long id;
    private String name;
    private String description;
    
    // 工作空间信息
    private Long workspaceId;
    private String workspaceName;
    
    // 创建者信息
    private Long creatorId;
    private String creatorName; // 用户名
    
    private Long latestVersionId;
    private Boolean isPublic;
    private Integer status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
