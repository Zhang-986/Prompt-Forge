package com.zzk.domain.model.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Prompt 模板实体
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PromptTemplate {
    
    private Long id;
    private Long promptId;
    private String name;
    private String description;
    private String content;
    private String category;
    private Long authorId;
    private String authorName;
    private Integer cloneCount;
    private Boolean isOfficial;
    private Boolean isActive;
    private Integer status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

