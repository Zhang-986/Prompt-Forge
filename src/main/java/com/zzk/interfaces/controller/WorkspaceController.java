package com.zzk.interfaces.controller;

import com.zzk.application.service.WorkspaceAppService;
import com.zzk.domain.model.entity.Workspace;
import com.zzk.domain.model.entity.WorkspaceMember;
import com.zzk.interfaces.dto.response.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 工作空间控制器
 * 
 * <p>提供工作空间的 CRUD 和成员管理接口
 * 
 * @author zzk
 * @since 1.0.0
 */
@Slf4j
@RestController
@RequestMapping("/api/workspaces")
@RequiredArgsConstructor
@Tag(name = "工作空间管理", description = "工作空间 CRUD 和成员管理接口")
public class WorkspaceController {

    private final WorkspaceAppService workspaceAppService;

    /**
     * 创建工作空间
     */
    @PostMapping
    @Operation(summary = "创建工作空间", description = "创建一个新的工作空间")
    public Result<Workspace> createWorkspace(
            @RequestAttribute("userId") Long userId,
            @Valid @RequestBody CreateWorkspaceRequest request) {
        log.info("创建工作空间: name={}, userId={}", request.getName(), userId);

        Workspace workspace = workspaceAppService.createWorkspace(
                request.getName(),
                request.getDescription(),
                userId
        );

        return Result.success("创建成功", workspace);
    }

    /**
     * 获取用户的所有工作空间
     */
    @GetMapping
    @Operation(summary = "获取工作空间列表", description = "获取当前用户可访问的所有工作空间")
    public Result<List<Workspace>> getWorkspaces(@RequestAttribute("userId") Long userId) {
        return Result.success(workspaceAppService.getWorkspacesForUser(userId));
    }

    /**
     * 获取工作空间详情
     */
    @GetMapping("/{id}")
    @Operation(summary = "获取工作空间详情")
    public Result<Workspace> getWorkspace(
            @RequestAttribute("userId") Long userId,
            @Parameter(description = "工作空间 ID") @PathVariable Long id) {
        return Result.success(workspaceAppService.getWorkspaceById(id, userId));
    }

    /**
     * 更新工作空间
     */
    @PutMapping("/{id}")
    @Operation(summary = "更新工作空间", description = "更新工作空间名称和描述")
    public Result<Workspace> updateWorkspace(
            @RequestAttribute("userId") Long userId,
            @Parameter(description = "工作空间 ID") @PathVariable Long id,
            @Valid @RequestBody UpdateWorkspaceRequest request) {
        log.info("更新工作空间: id={}, userId={}", id, userId);

        Workspace workspace = workspaceAppService.updateWorkspace(
                id,
                request.getName(),
                request.getDescription(),
                userId
        );

        return Result.success("更新成功", workspace);
    }

    /**
     * 删除工作空间
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "删除工作空间", description = "删除工作空间及其所有成员关系")
    public Result<Void> deleteWorkspace(
            @RequestAttribute("userId") Long userId,
            @Parameter(description = "工作空间 ID") @PathVariable Long id) {
        log.info("删除工作空间: id={}, userId={}", id, userId);

        workspaceAppService.deleteWorkspace(id, userId);
        return Result.success("删除成功", null);
    }

    /**
     * 获取工作空间成员列表
     */
    @GetMapping("/{id}/members")
    @Operation(summary = "获取成员列表")
    public Result<List<WorkspaceMember>> getMembers(
            @RequestAttribute("userId") Long userId,
            @Parameter(description = "工作空间 ID") @PathVariable Long id) {
        return Result.success(workspaceAppService.getMembers(id, userId));
    }

    /**
     * 添加成员
     */
    @PostMapping("/{id}/members")
    @Operation(summary = "添加成员", description = "将用户添加到工作空间")
    public Result<Void> addMember(
            @RequestAttribute("userId") Long operatorId,
            @Parameter(description = "工作空间 ID") @PathVariable Long id,
            @Valid @RequestBody AddMemberRequest request) {
        log.info("添加成员: workspaceId={}, targetUserId={}, operatorId={}", id, request.getUserId(), operatorId);

        workspaceAppService.addMember(id, request.getUserId(), request.getRole(), operatorId);
        return Result.success("添加成功", null);
    }

    /**
     * 移除成员
     */
    @DeleteMapping("/{id}/members/{targetUserId}")
    @Operation(summary = "移除成员", description = "将用户从工作空间移除")
    public Result<Void> removeMember(
            @RequestAttribute("userId") Long operatorId,
            @Parameter(description = "工作空间 ID") @PathVariable Long id,
            @Parameter(description = "目标用户 ID") @PathVariable Long targetUserId) {
        log.info("移除成员: workspaceId={}, targetUserId={}, operatorId={}", id, targetUserId, operatorId);

        workspaceAppService.removeMember(id, targetUserId, operatorId);
        return Result.success("移除成功", null);
    }

    /**
     * 更新成员角色
     */
    @PutMapping("/{id}/members/{targetUserId}")
    @Operation(summary = "更新成员角色", description = "修改工作空间成员的角色")
    public Result<Void> updateMemberRole(
            @RequestAttribute("userId") Long operatorId,
            @Parameter(description = "工作空间 ID") @PathVariable Long id,
            @Parameter(description = "目标用户 ID") @PathVariable Long targetUserId,
            @Valid @RequestBody UpdateRoleRequest request) {
        log.info("更新成员角色: workspaceId={}, targetUserId={}, newRole={}, operatorId={}", 
                id, targetUserId, request.getRole(), operatorId);

        workspaceAppService.updateMemberRole(id, targetUserId, request.getRole(), operatorId);
        return Result.success("角色更新成功", null);
    }

    // ==================== DTO ====================

    @Data
    public static class CreateWorkspaceRequest {
        @NotBlank(message = "工作空间名称不能为空")
        private String name;
        private String description;
    }

    @Data
    public static class UpdateWorkspaceRequest {
        @NotBlank(message = "工作空间名称不能为空")
        private String name;
        private String description;
    }

    @Data
    public static class AddMemberRequest {
        private Long userId;
        private String role = "MEMBER";
    }

    @Data
    public static class UpdateRoleRequest {
        @NotBlank(message = "角色不能为空")
        private String role;
    }
}
