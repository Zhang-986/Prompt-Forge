package com.zzk.infrastructure.persistence.po;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 工作空间成员持久化对象
 * 
 * @author zzk
 * @since 1.0.0
 */
@Data
@TableName("workspace_members")
public class WorkspaceMemberPO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long workspaceId;

    private Long userId;

    private String role;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
