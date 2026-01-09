package com.zzk.interfaces.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 竞技场会话详情 DTO
 * 
 * @author zzk
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ArenaSessionDetailDTO {

    private Long id;
    private Long promptId; // Added for frontend navigation
    private Long promptVersionId;
    private String finalPrompt;
    private Map<String, Object> variables; // JSON -> Map
    private List<String> models; // JSON -> List
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
        private String error;
        private Integer latencyMs;
        private Integer tokensUsed;
    }
}
