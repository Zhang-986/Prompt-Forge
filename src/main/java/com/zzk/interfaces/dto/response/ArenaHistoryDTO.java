package com.zzk.interfaces.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 竞技历史列表 DTO
 * 
 * @author zzk
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ArenaHistoryDTO {
    
    private Long id;
    private Long promptVersionId;
    private String status;
    private String models;  // JSON string of model list
    private LocalDateTime createdAt;
    private LocalDateTime completedAt;
}
