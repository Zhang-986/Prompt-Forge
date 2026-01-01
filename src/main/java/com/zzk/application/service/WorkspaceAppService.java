package com.zzk.application.service;

import com.zzk.domain.model.entity.Workspace;
import com.zzk.domain.model.entity.WorkspaceMember;
import com.zzk.domain.repository.WorkspaceRepository;
import com.zzk.infrastructure.annotation.SensitiveCheck;
import com.zzk.infrastructure.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 工作空间应用服务
 * 
 * <p>负责工作空间的 CRUD 和成员管理
 * 
 * @author zzk
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WorkspaceAppService {

    private final WorkspaceRepository workspaceRepository;

    /**
     * 创建工作空间
     */
    @SensitiveCheck
    @Transactional(rollbackFor = Exception.class)
    public Workspace createWorkspace(String name, String description, Long ownerId) {
        log.info("创建工作空间: name={}, ownerId={}", name, ownerId);

        Workspace workspace = Workspace.builder()
                .name(name)
                .description(description)
                .ownerId(ownerId)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        workspace = workspaceRepository.save(workspace);
        
        // 将创建者添加为管理员
        workspaceRepository.addMember(workspace.getId(), ownerId, "ADMIN");
        
        log.info("工作空间创建成功: id={}", workspace.getId());
        return workspace;
    }

    /**
     * 获取用户可访问的所有工作空间
     */
    public List<Workspace> getWorkspacesForUser(Long userId) {
        return workspaceRepository.findByUserId(userId);
    }

    /**
     * 获取工作空间详情
     */
    public Workspace getWorkspaceById(Long id, Long userId) {
        Workspace workspace = workspaceRepository.findById(id)
                .orElseThrow(() -> new BusinessException("工作空间不存在: " + id));
        
        // 检查权限
        if (!workspace.isOwner(userId) && !workspaceRepository.isMember(id, userId)) {
            throw new BusinessException("没有访问权限");
        }
        
        return workspace;
    }

    /**
     * 更新工作空间
     */
    @Transactional(rollbackFor = Exception.class)
    public Workspace updateWorkspace(Long id, String name, String description, Long userId) {
        log.info("更新工作空间: id={}", id);

        Workspace workspace = workspaceRepository.findById(id)
                .orElseThrow(() -> new BusinessException("工作空间不存在: " + id));

        // 只有所有者可以更新
        if (!workspace.isOwner(userId)) {
            throw new BusinessException("只有所有者可以更新工作空间");
        }

        workspace.setName(name);
        workspace.setDescription(description);
        workspace.setUpdatedAt(LocalDateTime.now());
        workspaceRepository.update(workspace);

        return workspace;
    }

    /**
     * 删除工作空间
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteWorkspace(Long id, Long userId) {
        log.info("删除工作空间: id={}", id);

        Workspace workspace = workspaceRepository.findById(id)
                .orElseThrow(() -> new BusinessException("工作空间不存在: " + id));

        // 只有所有者可以删除
        if (!workspace.isOwner(userId)) {
            throw new BusinessException("只有所有者可以删除工作空间");
        }

        workspaceRepository.deleteById(id);
    }

    /**
     * 添加成员
     */
    @Transactional(rollbackFor = Exception.class)
    public void addMember(Long workspaceId, Long targetUserId, String role, Long operatorId) {
        log.info("添加成员: workspaceId={}, targetUserId={}, role={}", workspaceId, targetUserId, role);

        Workspace workspace = workspaceRepository.findById(workspaceId)
                .orElseThrow(() -> new BusinessException("工作空间不存在: " + workspaceId));

        // 检查操作者权限（所有者或管理员可以添加成员）
        if (!workspace.isOwner(operatorId)) {
            String operatorRole = workspaceRepository.getMemberRole(workspaceId, operatorId)
                    .orElse(null);
            if (!"ADMIN".equals(operatorRole)) {
                throw new BusinessException("没有添加成员的权限");
            }
        }

        // 检查是否已经是成员
        if (workspaceRepository.isMember(workspaceId, targetUserId)) {
            throw new BusinessException("用户已经是工作空间成员");
        }

        workspaceRepository.addMember(workspaceId, targetUserId, role);
    }

    /**
     * 移除成员
     */
    @Transactional(rollbackFor = Exception.class)
    public void removeMember(Long workspaceId, Long targetUserId, Long operatorId) {
        log.info("移除成员: workspaceId={}, targetUserId={}", workspaceId, targetUserId);

        Workspace workspace = workspaceRepository.findById(workspaceId)
                .orElseThrow(() -> new BusinessException("工作空间不存在: " + workspaceId));

        // 不能移除所有者
        if (workspace.isOwner(targetUserId)) {
            throw new BusinessException("不能移除工作空间所有者");
        }

        // 检查操作者权限
        if (!workspace.isOwner(operatorId)) {
            String operatorRole = workspaceRepository.getMemberRole(workspaceId, operatorId)
                    .orElse(null);
            if (!"ADMIN".equals(operatorRole)) {
                throw new BusinessException("没有移除成员的权限");
            }
        }

        workspaceRepository.removeMember(workspaceId, targetUserId);
    }

    /**
     * 获取工作空间成员列表
     */
    public List<WorkspaceMember> getMembers(Long workspaceId, Long userId) {
        // 检查权限
        if (!workspaceRepository.isMember(workspaceId, userId)) {
            Workspace workspace = workspaceRepository.findById(workspaceId)
                    .orElseThrow(() -> new BusinessException("工作空间不存在"));
            if (!workspace.isOwner(userId)) {
                throw new BusinessException("没有访问权限");
            }
        }

        return workspaceRepository.getMembers(workspaceId);
    }

    /**
     * 更新成员角色
     */
    @Transactional(rollbackFor = Exception.class)
    public void updateMemberRole(Long workspaceId, Long targetUserId, String newRole, Long operatorId) {
        log.info("更新成员角色: workspaceId={}, targetUserId={}, newRole={}", workspaceId, targetUserId, newRole);

        Workspace workspace = workspaceRepository.findById(workspaceId)
                .orElseThrow(() -> new BusinessException("工作空间不存在: " + workspaceId));

        // 不能修改所有者角色
        if (workspace.isOwner(targetUserId)) {
            throw new BusinessException("不能修改工作空间所有者的角色");
        }

        // 检查目标用户是否是成员
        if (!workspaceRepository.isMember(workspaceId, targetUserId)) {
            throw new BusinessException("用户不是工作空间成员");
        }

        // 检查操作者权限
        if (!workspace.isOwner(operatorId)) {
            String operatorRole = workspaceRepository.getMemberRole(workspaceId, operatorId)
                    .orElse(null);
            if (!"ADMIN".equals(operatorRole)) {
                throw new BusinessException("没有修改成员角色的权限");
            }
        }

        // 验证角色值
        if (!List.of("ADMIN", "MEMBER", "VIEWER").contains(newRole)) {
            throw new BusinessException("无效的角色: " + newRole);
        }

        workspaceRepository.updateMemberRole(workspaceId, targetUserId, newRole);
    }
}
