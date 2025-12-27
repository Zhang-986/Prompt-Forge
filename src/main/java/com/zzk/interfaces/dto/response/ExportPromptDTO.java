package com.zzk.interfaces.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 导出 Prompt DTO
 * 
 * @author zzk
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExportPromptDTO {
    
    private String name;
    private String description;
    private List<VersionDTO> versions;
    private LocalDateTime exportedAt;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class VersionDTO {
        private Integer versionNumber;
        private String content;
        private String commitMessage;
        private LocalDateTime createdAt;
    }
}
