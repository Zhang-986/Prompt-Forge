package com.zzk.infrastructure.persistence.po;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 标签持久化对象
 * 
 * @author zzk
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TagPO {
    
    private Long id;
    private String name;
    private String color;
    private Long creatorId;
    private Long workspaceId;
    private LocalDateTime createdAt;
}
