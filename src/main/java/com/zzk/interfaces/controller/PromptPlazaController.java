package com.zzk.interfaces.controller;

import com.zzk.application.service.PromptPlazaAppService;
import com.zzk.domain.model.aggregate.Prompt;
import com.zzk.domain.model.entity.PromptTemplate;
import com.zzk.interfaces.dto.response.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Prompt 广场控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/plaza")
@RequiredArgsConstructor
@Tag(name = "Prompt 广场", description = "公共模板浏览、克隆、发布")
public class PromptPlazaController {
    
    private final PromptPlazaAppService plazaAppService;
    
    /**
     * 获取模板列表
     */
    @GetMapping
    @Operation(summary = "获取模板列表", description = "获取广场中的所有公共模板，可按分类筛选")
    public Result<List<PromptTemplate>> getTemplates(
            @Parameter(description = "分类筛选") @RequestParam(required = false) String category) {
        return Result.success(plazaAppService.getTemplates(category));
    }
    
    /**
     * 获取模板详情
     */
    @GetMapping("/{id}")
    @Operation(summary = "获取模板详情")
    public Result<PromptTemplate> getTemplate(
            @Parameter(description = "模板 ID") @PathVariable Long id) {
        return Result.success(plazaAppService.getTemplate(id));
    }
    
    /**
     * 克隆模板到工作空间
     */
    @PostMapping("/{id}/clone")
    @Operation(summary = "克隆模板", description = "将模板克隆到指定工作空间")
    public Result<Prompt> cloneTemplate(
            @RequestAttribute("userId") Long userId,
            @Parameter(description = "模板 ID") @PathVariable Long id,
            @Valid @RequestBody CloneRequest request) {
        log.info("克隆模板: templateId={}, workspaceId={}", id, request.getWorkspaceId());
        Prompt prompt = plazaAppService.cloneTemplate(id, request.getWorkspaceId(), userId);
        return Result.success("克隆成功", prompt);
    }
    
    /**
     * 发布 Prompt 到广场
     */
    @PostMapping("/publish")
    @Operation(summary = "发布到广场", description = "将自己的 Prompt 发布到公共广场")
    public Result<PromptTemplate> publish(
            @RequestAttribute("userId") Long userId,
            @Valid @RequestBody PublishRequest request) {
        log.info("发布到广场: promptId={}", request.getPromptId());
        PromptTemplate template = plazaAppService.publishToPlaza(
                request.getPromptId(), 
                request.getCategory(), 
                userId, 
                request.getAuthorName()
        );
        return Result.success("发布成功", template);
    }
    
    // ==================== DTO ====================
    
    @Data
    public static class CloneRequest {
        @NotNull(message = "工作空间ID不能为空")
        private Long workspaceId;
    }
    
    @Data
    public static class PublishRequest {
        @NotNull(message = "Prompt ID不能为空")
        private Long promptId;
        
        @NotBlank(message = "分类不能为空")
        private String category;
        
        private String authorName;
    }
}
