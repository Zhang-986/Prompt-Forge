package com.zzk.interfaces.controller;

import com.zzk.application.service.PromptExportService;
import com.zzk.domain.model.aggregate.Prompt;
import com.zzk.interfaces.dto.response.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * Prompt 导入导出控制器
 * 
 * @author zzk
 * @since 1.0.0
 */
@RestController
@RequestMapping("/api/prompts")
@RequiredArgsConstructor
public class PromptExportController {
    
    private final PromptExportService promptExportService;
    
    /**
     * 导出 Prompt 为 JSON
     */
    @GetMapping("/{promptId}/export")
    public ResponseEntity<byte[]> exportPrompt(@PathVariable Long promptId) {
        String json = promptExportService.exportToJson(promptId);
        
        byte[] content = json.getBytes(StandardCharsets.UTF_8);
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setContentDispositionFormData("attachment", "prompt_" + promptId + ".json");
        headers.setContentLength(content.length);
        
        return ResponseEntity.ok()
                .headers(headers)
                .body(content);
    }
    
    /**
     * 导入 Prompt (JSON 文件)
     */
    @PostMapping("/import")
    public Result<Map<String, Object>> importPrompt(
            @RequestAttribute("userId") Long userId,
            @RequestParam(defaultValue = "1") Long workspaceId,
            @RequestParam("file") MultipartFile file) {
        try {
            if (file.isEmpty()) {
                return Result.error(400, "请选择文件");
            }
            
            String json = new String(file.getBytes(), StandardCharsets.UTF_8);
            Prompt prompt = promptExportService.importFromJson(json, workspaceId, userId);
            
            return Result.success(Map.of(
                    "id", prompt.getId(),
                    "name", prompt.getName(),
                    "message", "导入成功"
            ));
        } catch (Exception e) {
            return Result.error(500, "导入失败: " + e.getMessage());
        }
    }
    
    /**
     * 导入 Prompt (JSON 字符串)
     */
    @PostMapping("/import/json")
    public Result<Map<String, Object>> importPromptJson(
            @RequestAttribute("userId") Long userId,
            @RequestParam(defaultValue = "1") Long workspaceId,
            @RequestBody String json) {
        try {
            Prompt prompt = promptExportService.importFromJson(json, workspaceId, userId);
            
            return Result.success(Map.of(
                    "id", prompt.getId(),
                    "name", prompt.getName(),
                    "message", "导入成功"
            ));
        } catch (Exception e) {
            return Result.error(500, "导入失败: " + e.getMessage());
        }
    }
}
