package com.zzk.infrastructure.persistence.po;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 竞技场结果持久化对象
 * 
 * @author zzk
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ArenaResultPO {
    
    private Long id;
    private Long sessionId;
    private String modelId;
    private String content;
    private Integer tokensUsed;
    private Integer latencyMs;
    private String status;
    private String errorMessage;
    private LocalDateTime createdAt;
}
