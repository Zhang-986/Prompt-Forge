package com.zzk.interfaces.controller;

import com.zzk.application.service.PromptAppService;
import com.zzk.domain.model.aggregate.Prompt;
import com.zzk.domain.model.entity.PromptVersion;
import com.zzk.interfaces.dto.request.CommitVersionRequest;
import com.zzk.interfaces.dto.request.CreatePromptRequest;
import com.zzk.interfaces.dto.response.DiffResult;
import com.zzk.interfaces.dto.response.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Prompt 控制器
 * 
 * <p>提供 Prompt 的 CRUD 和版本管理接口
 * 
 * @author zzk
 * @since 1.0.0
 */
@Slf4j
@RestController
@RequestMapping("/api/prompts")
@RequiredArgsConstructor
@Tag(name = "Prompt 管理", description = "Prompt CRUD 和版本管理接口")
public class PromptController {

    private final PromptAppService promptAppService;

    /**
     * 创建 Prompt
     */
    @PostMapping
    @Operation(summary = "创建 Prompt", description = "创建一个新的 Prompt 并初始化第一个版本")
    public Result<Prompt> createPrompt(@Valid @RequestBody CreatePromptRequest request) {
        log.info("创建 Prompt: name={}", request.getName());

        // TODO: 从 JWT 中获取用户 ID
        Long userId = 1L;

        Prompt prompt = promptAppService.createPrompt(
                request.getName(),
                request.getDescription(),
                request.getContent(),
                request.getWorkspaceId(),
                userId
        );

        return Result.success("创建成功", prompt);
    }

    /**
     * 获取 Prompt 详情
     */
    @GetMapping("/{promptId}")
    @Operation(summary = "获取 Prompt 详情")
    public Result<Prompt> getPrompt(
            @Parameter(description = "Prompt ID") @PathVariable Long promptId) {
        return Result.success(promptAppService.getPromptById(promptId));
    }

    /**
     * 获取工作空间下的 Prompt 列表
     */
    @GetMapping
    @Operation(summary = "获取 Prompt 列表", description = "按工作空间获取 Prompt 列表")
    public Result<List<Prompt>> getPromptsByWorkspace(
            @Parameter(description = "工作空间 ID") @RequestParam Long workspaceId) {
        return Result.success(promptAppService.getPromptsByWorkspace(workspaceId));
    }

    /**
     * 提交新版本
     */
    @PostMapping("/{promptId}/commit")
    @Operation(summary = "提交新版本", description = "提交 Prompt 的新版本，类似 Git commit")
    public Result<PromptVersion> commitVersion(
            @Parameter(description = "Prompt ID") @PathVariable Long promptId,
            @Valid @RequestBody CommitVersionRequest request) {
        log.info("提交新版本: promptId={}, parentVersionId={}", promptId, request.getParentVersionId());

        // TODO: 从 JWT 中获取用户 ID
        Long userId = 1L;

        PromptVersion version = promptAppService.commitVersion(
                promptId,
                request.getContent(),
                request.getParentVersionId(),
                request.getCommitMessage(),
                userId
        );

        return Result.success("提交成功", version);
    }

    /**
     * 获取版本历史
     */
    @GetMapping("/{promptId}/versions")
    @Operation(summary = "获取版本历史", description = "获取 Prompt 的所有版本，按版本号倒序")
    public Result<List<PromptVersion>> getVersionHistory(
            @Parameter(description = "Prompt ID") @PathVariable Long promptId) {
        return Result.success(promptAppService.getVersionHistory(promptId));
    }

    /**
     * 获取最新版本
     */
    @GetMapping("/{promptId}/latest")
    @Operation(summary = "获取最新版本", description = "获取 Prompt 的最新版本（HEAD）")
    public Result<PromptVersion> getLatestVersion(
            @Parameter(description = "Prompt ID") @PathVariable Long promptId) {
        return Result.success(promptAppService.getLatestVersion(promptId));
    }

    /**
     * 回滚到指定版本
     */
    @PostMapping("/{promptId}/rollback/{targetVersionId}")
    @Operation(summary = "回滚版本", description = "回滚到指定版本（会创建新版本）")
    public Result<PromptVersion> rollbackToVersion(
            @Parameter(description = "Prompt ID") @PathVariable Long promptId,
            @Parameter(description = "目标版本 ID") @PathVariable Long targetVersionId) {
        log.info("回滚版本: promptId={}, targetVersionId={}", promptId, targetVersionId);

        // TODO: 从 JWT 中获取用户 ID
        Long userId = 1L;

        PromptVersion version = promptAppService.rollbackToVersion(promptId, targetVersionId, userId);
        return Result.success("回滚成功", version);
    }

    /**
     * 删除 Prompt
     */
    @DeleteMapping("/{promptId}")
    @Operation(summary = "删除 Prompt", description = "软删除 Prompt")
    public Result<Void> deletePrompt(
            @Parameter(description = "Prompt ID") @PathVariable Long promptId) {
        log.info("删除 Prompt: promptId={}", promptId);

        // TODO: 从 JWT 中获取用户 ID
        Long userId = 1L;

        promptAppService.deletePrompt(promptId, userId);
        return Result.success("删除成功", null);
    }

    /**
     * 获取版本 Diff
     */
    @GetMapping("/diff")
    @Operation(summary = "获取版本 Diff", description = "对比两个版本之间的差异")
    public Result<DiffResult> getVersionDiff(
            @Parameter(description = "版本 1 ID（源版本）") @RequestParam Long versionId1,
            @Parameter(description = "版本 2 ID（目标版本）") @RequestParam Long versionId2) {
        log.info("获取版本 Diff: versionId1={}, versionId2={}", versionId1, versionId2);
        return Result.success(promptAppService.getVersionDiff(versionId1, versionId2));
    }
}
