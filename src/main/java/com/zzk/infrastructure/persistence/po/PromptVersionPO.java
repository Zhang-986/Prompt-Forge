package com.zzk.infrastructure.persistence.po;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Prompt 版本持久化对象
 * 
 * @author zzk
 * @since 1.0.0
 */
@Data
@TableName("prompt_versions")
public class PromptVersionPO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long promptId;

    private Integer versionNumber;

    private String content;

    private String variables;

    private Long parentId;

    private String commitMessage;

    private Long authorId;

    private String contentHash;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
