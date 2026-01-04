package com.zzk.interfaces.controller;

import com.zzk.application.service.AdminAppService;
import com.zzk.domain.model.entity.PlazaCategory;
import com.zzk.domain.model.entity.PromptTemplate;
import com.zzk.domain.model.entity.Workspace;
import com.zzk.interfaces.dto.response.AdminUserDTO;
import com.zzk.interfaces.dto.response.DashboardStatsDTO;
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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 管理员控制器
 * 
 * <p>提供超级管理员专属的管理接口，包括仪表盘统计、用户管理、工作空间管理、广场模板管理
 * 
 * @author zzk
 * @since 1.0.0
 */
@Slf4j
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@Tag(name = "管理员后台", description = "超级管理员专属接口")
public class AdminController {

    private final AdminAppService adminAppService;

    // ==================== 仪表盘 ====================

    /**
     * 获取仪表盘统计数据
     */
    @GetMapping("/dashboard")
    @Operation(summary = "获取仪表盘统计", description = "获取系统整体统计数据")
    public Result<DashboardStatsDTO> getDashboardStats() {
        log.info("获取仪表盘统计数据");
        DashboardStatsDTO stats = adminAppService.getDashboardStats();
        return Result.success(stats);
    }

    // ==================== 用户管理 ====================

    /**
     * 获取所有用户列表
     */
    @GetMapping("/users")
    @Operation(summary = "获取用户列表", description = "分页获取所有用户，包含工作空间信息")
    public Result<Map<String, Object>> getAllUsers(
            @Parameter(description = "页码，从0开始") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "每页数量") @RequestParam(defaultValue = "10") int size) {
        log.info("获取用户列表: page={}, size={}", page, size);
        
        List<AdminUserDTO> users = adminAppService.getAllUsers(page, size);
        long total = adminAppService.getUserCount();
        
        Map<String, Object> result = new HashMap<>();
        result.put("list", users);
        result.put("total", total);
        result.put("page", page);
        result.put("size", size);
        
        return Result.success(result);
    }

    /**
     * 更新用户状态（禁用/启用）
     */
    @PutMapping("/users/{id}/status")
    @Operation(summary = "更新用户状态", description = "禁用或启用用户")
    public Result<Void> updateUserStatus(
            @Parameter(description = "用户ID") @PathVariable Long id,
            @Valid @RequestBody UpdateStatusRequest request) {
        log.info("更新用户状态: userId={}, enabled={}", id, request.getEnabled());
        adminAppService.updateUserStatus(id, request.getEnabled());
        return Result.success("状态更新成功", null);
    }

    /**
     * 更新用户角色
     */
    @PutMapping("/users/{id}/role")
    @Operation(summary = "更新用户角色", description = "修改用户的角色")
    public Result<Void> updateUserRole(
            @Parameter(description = "用户ID") @PathVariable Long id,
            @Valid @RequestBody UpdateRoleRequest request) {
        log.info("更新用户角色: userId={}, role={}", id, request.getRole());
        adminAppService.updateUserRole(id, request.getRole());
        return Result.success("角色更新成功", null);
    }

    // ==================== 工作空间管理 ====================

    /**
     * 获取所有工作空间列表
     */
    @GetMapping("/workspaces")
    @Operation(summary = "获取工作空间列表", description = "分页获取所有工作空间")
    public Result<Map<String, Object>> getAllWorkspaces(
            @Parameter(description = "页码，从0开始") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "每页数量") @RequestParam(defaultValue = "10") int size) {
        log.info("获取工作空间列表: page={}, size={}", page, size);
        
        List<Workspace> workspaces = adminAppService.getAllWorkspaces(page, size);
        long total = adminAppService.getWorkspaceCount();
        
        Map<String, Object> result = new HashMap<>();
        result.put("list", workspaces);
        result.put("total", total);
        result.put("page", page);
        result.put("size", size);
        
        return Result.success(result);
    }

    /**
     * 删除工作空间
     */
    @DeleteMapping("/workspaces/{id}")
    @Operation(summary = "删除工作空间", description = "强制删除工作空间及其所有内容")
    public Result<Void> deleteWorkspace(
            @Parameter(description = "工作空间ID") @PathVariable Long id) {
        log.info("删除工作空间: workspaceId={}", id);
        adminAppService.deleteWorkspace(id);
        return Result.success("删除成功", null);
    }

    // ==================== 广场模板管理 ====================

    /**
     * 获取所有广场模板列表
     */
    @GetMapping("/templates")
    @Operation(summary = "获取广场模板列表", description = "分页获取所有广场模板")
    public Result<Map<String, Object>> getAllTemplates(
            @Parameter(description = "页码，从0开始") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "每页数量") @RequestParam(defaultValue = "10") int size) {
        log.info("获取广场模板列表: page={}, size={}", page, size);
        
        List<PromptTemplate> templates = adminAppService.getAllTemplates(page, size);
        long total = adminAppService.getTemplateCount();
        
        Map<String, Object> result = new HashMap<>();
        result.put("list", templates);
        result.put("total", total);
        result.put("page", page);
        result.put("size", size);
        
        return Result.success(result);
    }

    /**
     * 删除广场模板
     */
    @DeleteMapping("/templates/{id}")
    @Operation(summary = "删除广场模板", description = "从广场移除模板")
    public Result<Void> deleteTemplate(
            @Parameter(description = "模板ID") @PathVariable Long id) {
        log.info("删除广场模板: templateId={}", id);
        adminAppService.deleteTemplate(id);
        return Result.success("删除成功", null);
    }

    /**
     * 设置模板官方推荐
     */
    @PutMapping("/templates/{id}/official")
    @Operation(summary = "设置官方推荐", description = "设置或取消模板的官方推荐状态")
    public Result<Void> setTemplateOfficial(
            @Parameter(description = "模板ID") @PathVariable Long id,
            @Valid @RequestBody SetOfficialRequest request) {
        log.info("设置模板官方推荐: templateId={}, isOfficial={}", id, request.getIsOfficial());
        adminAppService.setTemplateOfficial(id, request.getIsOfficial());
        return Result.success("设置成功", null);
    }

    /**
     * 更新广场模板
     */
    @PutMapping("/templates/{id}")
    @Operation(summary = "更新广场模板", description = "编辑模板的名称、描述、内容和分类")
    public Result<PromptTemplate> updateTemplate(
            @Parameter(description = "模板ID") @PathVariable Long id,
            @Valid @RequestBody UpdateTemplateRequest request) {
        log.info("更新广场模板: templateId={}", id);
        PromptTemplate template = adminAppService.updateTemplate(
                id,
                request.getName(),
                request.getDescription(),
                request.getContent(),
                request.getCategory()
        );
        return Result.success("更新成功", template);
    }

    // ==================== Prompt 管理 ====================

    /**
     * 获取所有 Prompt 列表
     */
    @GetMapping("/prompts")
    @Operation(summary = "获取Prompt列表", description = "分页获取所有Prompt")
    public Result<Map<String, Object>> getAllPrompts(
            @Parameter(description = "页码，从0开始") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "每页数量") @RequestParam(defaultValue = "10") int size) {
        log.info("获取Prompt列表: page={}, size={}", page, size);
        
        var prompts = adminAppService.getAllPrompts(page, size);
        long total = adminAppService.getPromptCount();
        
        Map<String, Object> result = new HashMap<>();
        result.put("list", prompts);
        result.put("total", total);
        result.put("page", page);
        result.put("size", size);
        
        return Result.success(result);
    }

    /**
     * 删除 Prompt
     */
    @DeleteMapping("/prompts/{id}")
    @Operation(summary = "删除Prompt", description = "软删除Prompt")
    public Result<Void> deletePrompt(
            @Parameter(description = "Prompt ID") @PathVariable Long id) {
        log.info("删除Prompt: promptId={}", id);
        adminAppService.deletePrompt(id);
        return Result.success("删除成功", null);
    }

    // ==================== 竞技场会话管理 ====================

    /**
     * 获取所有竞技场会话列表
     */
    @GetMapping("/arena-sessions")
    @Operation(summary = "获取竞技场会话列表", description = "分页获取所有竞技场会话")
    public Result<Map<String, Object>> getAllArenaSessions(
            @Parameter(description = "页码，从0开始") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "每页数量") @RequestParam(defaultValue = "10") int size) {
        log.info("获取竞技场会话列表: page={}, size={}", page, size);
        
        var sessions = adminAppService.getAllArenaSessions(page, size);
        long total = adminAppService.getArenaSessionCount();
        
        Map<String, Object> result = new HashMap<>();
        result.put("list", sessions);
        result.put("total", total);
        result.put("page", page);
        result.put("size", size);
        
        return Result.success(result);
    }

    // ==================== 登录日志管理 ====================

    /**
     * 获取所有登录日志列表
     */
    @GetMapping("/login-logs")
    @Operation(summary = "获取登录日志列表", description = "分页获取所有登录日志")
    public Result<Map<String, Object>> getAllLoginLogs(
            @Parameter(description = "页码，从0开始") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "每页数量") @RequestParam(defaultValue = "10") int size) {
        log.info("获取登录日志列表: page={}, size={}", page, size);
        
        var logs = adminAppService.getAllLoginLogs(page, size);
        long total = adminAppService.getLoginLogCount();
        
        Map<String, Object> result = new HashMap<>();
        result.put("list", logs);
        result.put("total", total);
        result.put("page", page);
        result.put("size", size);
        
        return Result.success(result);
    }

    // ==================== 广场分类管理 ====================

    /**
     * 获取所有分类
     */
    @GetMapping("/categories")
    @Operation(summary = "获取分类列表", description = "获取所有广场分类")
    public Result<List<PlazaCategory>> getAllCategories() {
        log.info("获取所有分类");
        return Result.success(adminAppService.getAllCategories());
    }

    /**
     * 创建分类
     */
    @PostMapping("/categories")
    @Operation(summary = "创建分类", description = "创建新的广场分类")
    public Result<PlazaCategory> createCategory(@Valid @RequestBody CategoryRequest request) {
        log.info("创建分类: value={}", request.getValue());
        PlazaCategory category = adminAppService.createCategory(
                request.getValue(),
                request.getLabel(),
                request.getIcon(),
                request.getSortOrder()
        );
        return Result.success("创建成功", category);
    }

    /**
     * 更新分类
     */
    @PutMapping("/categories/{id}")
    @Operation(summary = "更新分类", description = "更新分类信息")
    public Result<PlazaCategory> updateCategory(
            @Parameter(description = "分类ID") @PathVariable Long id,
            @Valid @RequestBody CategoryRequest request) {
        log.info("更新分类: id={}", id);
        PlazaCategory category = adminAppService.updateCategory(
                id,
                request.getValue(),
                request.getLabel(),
                request.getIcon(),
                request.getSortOrder()
        );
        return Result.success("更新成功", category);
    }

    /**
     * 删除分类
     */
    @DeleteMapping("/categories/{id}")
    @Operation(summary = "删除分类", description = "删除广场分类")
    public Result<Void> deleteCategory(@Parameter(description = "分类ID") @PathVariable Long id) {
        log.info("删除分类: id={}", id);
        adminAppService.deleteCategory(id);
        return Result.success("删除成功", null);
    }

    // ==================== DTO ====================

    @Data
    public static class UpdateStatusRequest {
        @NotNull(message = "状态不能为空")
        private Boolean enabled;
    }

    @Data
    public static class UpdateRoleRequest {
        @NotBlank(message = "角色不能为空")
        private String role;
    }

    @Data
    public static class SetOfficialRequest {
        @NotNull(message = "官方状态不能为空")
        private Boolean isOfficial;
    }

    @Data
    public static class UpdateTemplateRequest {
        @NotBlank(message = "名称不能为空")
        private String name;
        
        private String description;
        
        @NotBlank(message = "内容不能为空")
        private String content;
        
        @NotBlank(message = "分类不能为空")
        private String category;
    }

    @Data
    public static class CategoryRequest {
        @NotBlank(message = "分类值不能为空")
        private String value;
        
        @NotBlank(message = "分类名称不能为空")
        private String label;
        
        private String icon;
        
        private Integer sortOrder;
    }
}

