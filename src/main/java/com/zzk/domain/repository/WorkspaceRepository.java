package com.zzk.domain.repository;

import com.zzk.domain.model.entity.Workspace;
import com.zzk.domain.model.entity.WorkspaceMember;

import java.util.List;
import java.util.Optional;

/**
 * 工作空间仓储接口
 * 
 * @author zzk
 * @since 1.0.0
 */
public interface WorkspaceRepository {

    /**
     * 根据 ID 查询
     */
    Optional<Workspace> findById(Long id);

    /**
     * 查询用户可访问的所有工作空间（作为所有者或成员）
     */
    List<Workspace> findByUserId(Long userId);

    /**
     * 保存工作空间
     */
    Workspace save(Workspace workspace);

    /**
     * 更新工作空间
     */
    void update(Workspace workspace);

    /**
     * 删除工作空间
     */
    void deleteById(Long id);

    /**
     * 添加成员
     */
    void addMember(Long workspaceId, Long userId, String role);

    /**
     * 移除成员
     */
    void removeMember(Long workspaceId, Long userId);

    /**
     * 获取工作空间成员列表
     */
    List<WorkspaceMember> getMembers(Long workspaceId);

    /**
     * 检查用户是否是工作空间成员
     */
    boolean isMember(Long workspaceId, Long userId);

    /**
     * 获取用户在工作空间中的角色
     */
    Optional<String> getMemberRole(Long workspaceId, Long userId);
}
