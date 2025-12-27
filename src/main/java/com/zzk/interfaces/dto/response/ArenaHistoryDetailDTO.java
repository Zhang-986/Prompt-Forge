package com.zzk.interfaces.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 竞技历史详情 DTO
 * 
 * @author zzk
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ArenaHistoryDetailDTO {
    
    private Long id;
    private Long promptVersionId;
    private String finalPrompt;
    private String variables;
    private String models;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime completedAt;
    private List<ResultDTO> results;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ResultDTO {
        private String modelId;
        private String content;
        private Integer tokensUsed;
        private Integer latencyMs;
        private String status;
        private String errorMessage;
    }
}
