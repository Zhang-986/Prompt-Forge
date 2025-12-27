package com.zzk.infrastructure.persistence.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Prompt 模板 PO
 */
@Data
@TableName("prompt_templates")
public class PromptTemplatePO {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private String name;
    private String description;
    private String content;
    private String category;
    private Long authorId;
    private String authorName;
    private Integer cloneCount;
    private Boolean isOfficial;
    private Boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
