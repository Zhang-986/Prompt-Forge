package com.zzk.infrastructure.persistence.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zzk.domain.model.entity.Workspace;
import com.zzk.domain.model.entity.WorkspaceMember;
import com.zzk.domain.repository.WorkspaceRepository;
import com.zzk.infrastructure.persistence.mapper.UserMapper;
import com.zzk.infrastructure.persistence.mapper.WorkspaceMapper;
import com.zzk.infrastructure.persistence.mapper.WorkspaceMemberMapper;
import com.zzk.infrastructure.persistence.po.UserPO;
import com.zzk.infrastructure.persistence.po.WorkspaceMemberPO;
import com.zzk.infrastructure.persistence.po.WorkspacePO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 工作空间仓储实现
 * 
 * @author zzk
 * @since 1.0.0
 */
@Repository
@RequiredArgsConstructor
public class WorkspaceRepositoryImpl implements WorkspaceRepository {

    private final WorkspaceMapper workspaceMapper;
    private final WorkspaceMemberMapper memberMapper;
    private final UserMapper userMapper;

    @Override
    public Optional<Workspace> findById(Long id) {
        WorkspacePO po = workspaceMapper.selectById(id);
        return Optional.ofNullable(po).map(this::toDomain);
    }

    @Override
    public List<Workspace> findByUserId(Long userId) {
        return workspaceMapper.findByMemberUserId(userId).stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public Workspace save(Workspace workspace) {
        WorkspacePO po = toPO(workspace);
        workspaceMapper.insert(po);
        workspace.setId(po.getId());
        return workspace;
    }

    @Override
    public void update(Workspace workspace) {
        WorkspacePO po = toPO(workspace);
        workspaceMapper.updateById(po);
    }

    @Override
    public void deleteById(Long id) {
        // 先删除成员关系
        LambdaQueryWrapper<WorkspaceMemberPO> memberWrapper = new LambdaQueryWrapper<>();
        memberWrapper.eq(WorkspaceMemberPO::getWorkspaceId, id);
        memberMapper.delete(memberWrapper);
        // 再删除工作空间
        workspaceMapper.deleteById(id);
    }

    @Override
    public void addMember(Long workspaceId, Long userId, String role) {
        WorkspaceMemberPO po = new WorkspaceMemberPO();
        po.setWorkspaceId(workspaceId);
        po.setUserId(userId);
        po.setRole(role);
        po.setCreatedAt(java.time.LocalDateTime.now());
        memberMapper.insert(po);
    }

    @Override
    public void removeMember(Long workspaceId, Long userId) {
        LambdaQueryWrapper<WorkspaceMemberPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(WorkspaceMemberPO::getWorkspaceId, workspaceId)
               .eq(WorkspaceMemberPO::getUserId, userId);
        memberMapper.delete(wrapper);
    }

    @Override
    public List<WorkspaceMember> getMembers(Long workspaceId) {
        LambdaQueryWrapper<WorkspaceMemberPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(WorkspaceMemberPO::getWorkspaceId, workspaceId);
        return memberMapper.selectList(wrapper).stream()
                .map(this::toMemberDomain)
                .collect(Collectors.toList());
    }

    @Override
    public boolean isMember(Long workspaceId, Long userId) {
        LambdaQueryWrapper<WorkspaceMemberPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(WorkspaceMemberPO::getWorkspaceId, workspaceId)
               .eq(WorkspaceMemberPO::getUserId, userId);
        return memberMapper.selectCount(wrapper) > 0;
    }

    @Override
    public Optional<String> getMemberRole(Long workspaceId, Long userId) {
        LambdaQueryWrapper<WorkspaceMemberPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(WorkspaceMemberPO::getWorkspaceId, workspaceId)
               .eq(WorkspaceMemberPO::getUserId, userId);
        WorkspaceMemberPO po = memberMapper.selectOne(wrapper);
        return Optional.ofNullable(po).map(WorkspaceMemberPO::getRole);
    }

    // ==================== 转换方法 ====================

    private Workspace toDomain(WorkspacePO po) {
        if (po == null) return null;
        return Workspace.builder()
                .id(po.getId())
                .name(po.getName())
                .description(po.getDescription())
                .ownerId(po.getOwnerId())
                .createdAt(po.getCreatedAt())
                .updatedAt(po.getUpdatedAt())
                .build();
    }

    private WorkspacePO toPO(Workspace domain) {
        if (domain == null) return null;
        WorkspacePO po = new WorkspacePO();
        po.setId(domain.getId());
        po.setName(domain.getName());
        po.setDescription(domain.getDescription());
        po.setOwnerId(domain.getOwnerId());
        po.setCreatedAt(domain.getCreatedAt());
        po.setUpdatedAt(domain.getUpdatedAt());
        return po;
    }

    private WorkspaceMember toMemberDomain(WorkspaceMemberPO po) {
        if (po == null) return null;
        // 查询用户名
        String username = null;
        UserPO user = userMapper.selectById(po.getUserId());
        if (user != null) {
            username = user.getUsername();
        }
        return WorkspaceMember.builder()
                .id(po.getId())
                .workspaceId(po.getWorkspaceId())
                .userId(po.getUserId())
                .username(username)
                .role(po.getRole())
                .createdAt(po.getCreatedAt())
                .build();
    }

    @Override
    public void updateMemberRole(Long workspaceId, Long userId, String newRole) {
        com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<WorkspaceMemberPO> wrapper = 
                new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<>();
        wrapper.eq(WorkspaceMemberPO::getWorkspaceId, workspaceId)
               .eq(WorkspaceMemberPO::getUserId, userId)
               .set(WorkspaceMemberPO::getRole, newRole);
        memberMapper.update(null, wrapper);
    }
}
