package com.zzk.domain.model.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 广场分类实体
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlazaCategory {
    
    private Long id;
    private String value;
    private String label;
    private String icon;
    private Integer sortOrder;
    private Boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
