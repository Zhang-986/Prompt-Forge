package com.zzk.infrastructure.persistence.po;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Prompt 持久化对象
 * 
 * @author zzk
 * @since 1.0.0
 */
@Data
@TableName("prompts")
public class PromptPO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String name;

    private String description;

    private Long workspaceId;

    private Long latestVersionId;

    private Long creatorId;

    private Boolean isPublic;

    private Integer status;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
