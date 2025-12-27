package com.zzk.infrastructure.persistence.po;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 竞技场会话持久化对象
 * 
 * @author zzk
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ArenaSessionPO {
    
    private Long id;
    private Long promptVersionId;
    private String finalPrompt;
    private String variables;  // JSON string
    private String models;     // JSON string
    private String status;
    private Long creatorId;
    private LocalDateTime createdAt;
    private LocalDateTime completedAt;
}
