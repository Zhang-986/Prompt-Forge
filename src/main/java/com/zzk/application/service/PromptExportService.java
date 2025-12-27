package com.zzk.application.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.zzk.domain.model.aggregate.Prompt;
import com.zzk.domain.model.entity.PromptVersion;
import com.zzk.domain.repository.PromptRepository;
import com.zzk.domain.repository.PromptVersionRepository;
import com.zzk.interfaces.dto.request.ImportPromptRequest;
import com.zzk.interfaces.dto.response.ExportPromptDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Prompt 导入导出服务
 * 
 * @author zzk
 * @since 1.0.0
 */
@Service
@RequiredArgsConstructor
public class PromptExportService {
    
    private final PromptRepository promptRepository;
    private final PromptVersionRepository promptVersionRepository;
    
    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
            .enable(SerializationFeature.INDENT_OUTPUT);
    
    /**
     * 导出 Prompt 为 JSON 字符串
     */
    public String exportToJson(Long promptId) {
        ExportPromptDTO exportData = buildExportData(promptId);
        try {
            return objectMapper.writeValueAsString(exportData);
        } catch (Exception e) {
            throw new RuntimeException("导出失败: " + e.getMessage());
        }
    }
    
    /**
     * 从 JSON 导入 Prompt
     */
    @Transactional
    public Prompt importFromJson(String json, Long workspaceId, Long userId) {
        try {
            ImportPromptRequest importData = objectMapper.readValue(json, ImportPromptRequest.class);
            return createPromptFromImport(importData, workspaceId, userId);
        } catch (Exception e) {
            throw new RuntimeException("导入失败: " + e.getMessage());
        }
    }
    
    /**
     * 构建导出数据
     */
    private ExportPromptDTO buildExportData(Long promptId) {
        Prompt prompt = promptRepository.findById(promptId)
                .orElseThrow(() -> new IllegalArgumentException("Prompt 不存在"));
        
        // 使用正确的方法名
        List<PromptVersion> versions = promptVersionRepository.findByPromptIdOrderByVersionNumberDesc(promptId);
        
        return ExportPromptDTO.builder()
                .name(prompt.getName())
                .description(prompt.getDescription())
                .versions(versions.stream()
                        .map(v -> ExportPromptDTO.VersionDTO.builder()
                                .versionNumber(v.getVersionNumber())
                                .content(v.getContent())
                                .commitMessage(v.getCommitMessage())
                                .createdAt(v.getCreatedAt())
                                .build())
                        .toList())
                .exportedAt(LocalDateTime.now())
                .build();
    }
    
    /**
     * 从导入数据创建 Prompt
     */
    private Prompt createPromptFromImport(ImportPromptRequest importData, Long workspaceId, Long userId) {
        // 创建 Prompt
        Prompt prompt = Prompt.builder()
                .name(importData.getName())
                .description(importData.getDescription())
                .workspaceId(workspaceId)
                .creatorId(userId)
                .isPublic(false)
                .status(1)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        
        // save 返回 void，需要分开处理
        promptRepository.save(prompt);
        
        // 创建版本（如果有）
        if (importData.getVersions() != null && !importData.getVersions().isEmpty()) {
            Long lastVersionId = null;
            for (int i = 0; i < importData.getVersions().size(); i++) {
                ImportPromptRequest.VersionDTO versionDTO = importData.getVersions().get(i);
                
                PromptVersion version = PromptVersion.builder()
                        .promptId(prompt.getId())
                        .versionNumber(i + 1)
                        .content(versionDTO.getContent())
                        .commitMessage(versionDTO.getCommitMessage() != null 
                                ? versionDTO.getCommitMessage() 
                                : "导入版本 v" + (i + 1))
                        .parentId(lastVersionId)
                        .authorId(userId)
                        .createdAt(LocalDateTime.now())
                        .build();
                
                // save 返回 void
                promptVersionRepository.save(version);
                lastVersionId = version.getId();
            }
            
            // 更新 Prompt 的最新版本（使用 updateLatestVersion 方法）
            promptRepository.updateLatestVersion(prompt.getId(), lastVersionId);
            prompt.setLatestVersionId(lastVersionId);
            prompt.setLatestVersionNumber(importData.getVersions().size());
        }
        
        return prompt;
    }
}
