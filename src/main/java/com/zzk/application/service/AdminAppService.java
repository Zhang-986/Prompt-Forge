package com.zzk.application.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zzk.domain.model.aggregate.User;
import com.zzk.domain.model.entity.PromptTemplate;
import com.zzk.domain.model.entity.Workspace;
import com.zzk.domain.repository.PromptTemplateRepository;
import com.zzk.domain.repository.UserRepository;
import com.zzk.domain.repository.WorkspaceRepository;
import com.zzk.infrastructure.exception.BusinessException;
import com.zzk.infrastructure.persistence.converter.PromptTemplateConverter;
import com.zzk.infrastructure.persistence.converter.WorkspaceConverter;
import com.zzk.infrastructure.persistence.mapper.*;
import com.zzk.infrastructure.persistence.po.*;
import com.zzk.interfaces.dto.response.AdminPromptDTO;
import com.zzk.interfaces.dto.response.AdminUserDTO;
import com.zzk.interfaces.dto.response.DashboardStatsDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 管理员应用服务
 * 
 * <p>提供超级管理员的业务功能：仪表盘统计、用户管理、工作空间管理、广场模板管理
 * 
 * @author zzk
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminAppService {

    private final UserRepository userRepository;
    private final WorkspaceRepository workspaceRepository;
    private final PromptTemplateRepository promptTemplateRepository;
    
    private final UserMapper userMapper;
    private final WorkspaceMapper workspaceMapper;
    private final PromptMapper promptMapper;
    private final PromptTemplateMapper promptTemplateMapper;
    private final ArenaSessionMapper arenaSessionMapper;
    private final WorkspaceMemberMapper workspaceMemberMapper;

    // ==================== 仪表盘统计 ====================

    /**
     * 获取仪表盘统计数据
     */
    public DashboardStatsDTO getDashboardStats() {
        log.info("获取仪表盘统计数据");
        
        long totalUsers = userMapper.selectCount(null);
        long totalWorkspaces = workspaceMapper.selectCount(null);
        
        long totalPrompts = promptMapper.selectCount(
                new LambdaQueryWrapper<PromptPO>().eq(PromptPO::getStatus, 1));
        
        long publicPrompts = promptMapper.selectCount(
                new LambdaQueryWrapper<PromptPO>()
                        .eq(PromptPO::getStatus, 1)
                        .eq(PromptPO::getIsPublic, true));
        
        long totalArenaSessions = arenaSessionMapper.selectCount(null);
        
        // 最近7天有活动的用户数（简化统计，暂用会话数代替）
        LocalDateTime sevenDaysAgo = LocalDateTime.now().minusDays(7);
        long activeUsersLast7Days = arenaSessionMapper.selectCount(
                new LambdaQueryWrapper<ArenaSessionPO>()
                        .ge(ArenaSessionPO::getCreatedAt, sevenDaysAgo));
        
        long totalTemplates = promptTemplateMapper.selectCount(
                new LambdaQueryWrapper<PromptTemplatePO>().eq(PromptTemplatePO::getIsActive, true));

        return DashboardStatsDTO.builder()
                .totalUsers(totalUsers)
                .totalWorkspaces(totalWorkspaces)
                .totalPrompts(totalPrompts)
                .publicPrompts(publicPrompts)
                .totalArenaSessions(totalArenaSessions)
                .activeUsersLast7Days(activeUsersLast7Days)
                .totalTemplates(totalTemplates)
                .build();
    }

    // ==================== 用户管理 ====================

    /**
     * 获取所有用户列表（分页）- 包含工作空间信息
     */
    public List<AdminUserDTO> getAllUsers(int page, int size) {
        log.info("获取所有用户列表: page={}, size={}", page, size);
        
        Page<UserPO> pageResult = userMapper.selectPage(
                new Page<>(page + 1, size),
                new LambdaQueryWrapper<UserPO>().orderByDesc(UserPO::getId));
        
        return pageResult.getRecords().stream()
                .map(this::buildAdminUserDTO)
                .collect(Collectors.toList());
    }
    
    /**
     * 构建管理员用户 DTO
     */
    private AdminUserDTO buildAdminUserDTO(UserPO userPO) {
        Long userId = userPO.getId();
        
        // 查询用户所属的工作空间
        List<WorkspaceMemberPO> memberList = workspaceMemberMapper.selectList(
                new LambdaQueryWrapper<WorkspaceMemberPO>().eq(WorkspaceMemberPO::getUserId, userId));
        
        List<AdminUserDTO.WorkspaceInfo> workspaceInfoList = memberList.stream()
                .map(member -> {
                    WorkspacePO ws = workspaceMapper.selectById(member.getWorkspaceId());
                    if (ws == null) return null;
                    return AdminUserDTO.WorkspaceInfo.builder()
                            .id(ws.getId())
                            .name(ws.getName())
                            .role(member.getRole())
                            .isOwner(ws.getOwnerId().equals(userId))
                            .build();
                })
                .filter(info -> info != null)
                .collect(Collectors.toList());
        
        // 统计用户创建的 Prompt 数量
        long promptCount = promptMapper.selectCount(
                new LambdaQueryWrapper<PromptPO>()
                        .eq(PromptPO::getCreatorId, userId)
                        .eq(PromptPO::getStatus, 1));
        
        // 统计用户竞技场会话数
        long arenaCount = arenaSessionMapper.selectCount(
                new LambdaQueryWrapper<ArenaSessionPO>().eq(ArenaSessionPO::getCreatorId, userId));
        
        return AdminUserDTO.builder()
                .id(userPO.getId())
                .username(userPO.getUsername())
                .email(userPO.getEmail())
                .avatar(userPO.getAvatar())
                .role(userPO.getRole())
                .status(userPO.getStatus())
                .createdAt(userPO.getCreatedAt())
                .updatedAt(userPO.getUpdatedAt())
                .workspaces(workspaceInfoList)
                .promptCount((int) promptCount)
                .arenaSessionCount((int) arenaCount)
                .build();
    }

    /**
     * 获取用户总数
     */
    public long getUserCount() {
        return userMapper.selectCount(null);
    }

    /**
     * 更新用户状态（启用/禁用）
     */
    @Transactional(rollbackFor = Exception.class)
    public void updateUserStatus(Long userId, boolean enabled) {
        log.info("更新用户状态: userId={}, enabled={}", userId, enabled);
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException("用户不存在"));
        
        // 不能禁用管理员
        if (user.isAdmin()) {
            throw new BusinessException("不能禁用管理员账户");
        }
        
        int status = enabled ? 1 : 0;
        userMapper.update(null, 
                new LambdaUpdateWrapper<UserPO>()
                        .eq(UserPO::getId, userId)
                        .set(UserPO::getStatus, status)
                        .set(UserPO::getUpdatedAt, LocalDateTime.now()));
    }

    /**
     * 更新用户角色
     */
    @Transactional(rollbackFor = Exception.class)
    public void updateUserRole(Long userId, String role) {
        log.info("更新用户角色: userId={}, role={}", userId, role);
        
        if (!isValidRole(role)) {
            throw new BusinessException("无效的角色: " + role);
        }
        
        userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException("用户不存在"));
        
        userMapper.update(null,
                new LambdaUpdateWrapper<UserPO>()
                        .eq(UserPO::getId, userId)
                        .set(UserPO::getRole, role)
                        .set(UserPO::getUpdatedAt, LocalDateTime.now()));
    }

    private boolean isValidRole(String role) {
        return "ADMIN".equals(role) || "MEMBER".equals(role) || "VIEWER".equals(role);
    }

    // ==================== 工作空间管理 ====================

    /**
     * 获取所有工作空间列表（分页）
     */
    public List<Workspace> getAllWorkspaces(int page, int size) {
        log.info("获取所有工作空间列表: page={}, size={}", page, size);
        
        Page<WorkspacePO> pageResult = workspaceMapper.selectPage(
                new Page<>(page + 1, size),
                new LambdaQueryWrapper<WorkspacePO>().orderByDesc(WorkspacePO::getId));
        
        return pageResult.getRecords().stream()
                .map(WorkspaceConverter::toDomain)
                .collect(Collectors.toList());
    }

    /**
     * 获取工作空间总数
     */
    public long getWorkspaceCount() {
        return workspaceMapper.selectCount(null);
    }

    /**
     * 删除工作空间（管理员强制删除）
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteWorkspace(Long workspaceId) {
        log.info("管理员删除工作空间: workspaceId={}", workspaceId);
        
        workspaceRepository.findById(workspaceId)
                .orElseThrow(() -> new BusinessException("工作空间不存在"));
        
        // 删除工作空间成员
        workspaceMemberMapper.delete(
                new LambdaQueryWrapper<WorkspaceMemberPO>()
                        .eq(WorkspaceMemberPO::getWorkspaceId, workspaceId));
        
        // 软删除工作空间下的 Prompts
        promptMapper.update(null,
                new LambdaUpdateWrapper<PromptPO>()
                        .eq(PromptPO::getWorkspaceId, workspaceId)
                        .set(PromptPO::getStatus, 0));
        
        // 删除工作空间
        workspaceMapper.deleteById(workspaceId);
    }

    // ==================== 广场模板管理 ====================

    /**
     * 获取所有广场模板列表（分页）
     */
    public List<PromptTemplate> getAllTemplates(int page, int size) {
        log.info("获取所有广场模板列表: page={}, size={}", page, size);
        
        Page<PromptTemplatePO> pageResult = promptTemplateMapper.selectPage(
                new Page<>(page + 1, size),
                new LambdaQueryWrapper<PromptTemplatePO>().orderByDesc(PromptTemplatePO::getId));
        
        return pageResult.getRecords().stream()
                .map(PromptTemplateConverter::toDomain)
                .collect(Collectors.toList());
    }

    /**
     * 获取模板总数
     */
    public long getTemplateCount() {
        return promptTemplateMapper.selectCount(null);
    }

    /**
     * 删除广场模板
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteTemplate(Long templateId) {
        log.info("管理员删除广场模板: templateId={}", templateId);
        
        promptTemplateRepository.findById(templateId)
                .orElseThrow(() -> new BusinessException("模板不存在"));
        
        // 软删除：设置 is_active = FALSE
        promptTemplateMapper.update(null,
                new LambdaUpdateWrapper<PromptTemplatePO>()
                        .eq(PromptTemplatePO::getId, templateId)
                        .set(PromptTemplatePO::getIsActive, false));
    }

    /**
     * 设置模板官方推荐状态
     */
    @Transactional(rollbackFor = Exception.class)
    public void setTemplateOfficial(Long templateId, boolean isOfficial) {
        log.info("设置模板官方推荐: templateId={}, isOfficial={}", templateId, isOfficial);
        
        promptTemplateRepository.findById(templateId)
                .orElseThrow(() -> new BusinessException("模板不存在"));
        
        promptTemplateMapper.update(null,
                new LambdaUpdateWrapper<PromptTemplatePO>()
                        .eq(PromptTemplatePO::getId, templateId)
                        .set(PromptTemplatePO::getIsOfficial, isOfficial));
    }
    
    // ==================== Prompt 管理 ====================
    
    /**
     * 获取所有 Prompt 列表（分页）
     */
    public List<AdminPromptDTO> getAllPrompts(int page, int size) {
        log.info("获取所有Prompt列表: page={}, size={}", page, size);
        
        Page<PromptPO> pageResult = promptMapper.selectPage(
                new Page<>(page + 1, size),
                new LambdaQueryWrapper<PromptPO>().orderByDesc(PromptPO::getId));
        
        return pageResult.getRecords().stream()
                .map(prompt -> {
                    String workspaceName = "未知工作空间";
                    WorkspacePO ws = workspaceMapper.selectById(prompt.getWorkspaceId());
                    if (ws != null) {
                        workspaceName = ws.getName();
                    }
                    
                    String creatorName = "未知用户";
                    UserPO user = userMapper.selectById(prompt.getCreatorId());
                    if (user != null) {
                        creatorName = user.getUsername();
                    }
                    
                    return AdminPromptDTO.builder()
                            .id(prompt.getId())
                            .name(prompt.getName())
                            .description(prompt.getDescription())
                            .workspaceId(prompt.getWorkspaceId())
                            .workspaceName(workspaceName)
                            .creatorId(prompt.getCreatorId())
                            .creatorName(creatorName)
                            .latestVersionId(prompt.getLatestVersionId())
                            .isPublic(prompt.getIsPublic())
                            .status(prompt.getStatus())
                            .createdAt(prompt.getCreatedAt())
                            .updatedAt(prompt.getUpdatedAt())
                            .build();
                })
                .collect(Collectors.toList());
    }
    
    /**
     * 获取 Prompt 总数
     */
    public long getPromptCount() {
        return promptMapper.selectCount(null);
    }
    
    /**
     * 删除 Prompt（软删除）
     */
    @Transactional(rollbackFor = Exception.class)
    public void deletePrompt(Long promptId) {
        log.info("管理员删除Prompt: promptId={}", promptId);
        
        promptMapper.update(null,
                new LambdaUpdateWrapper<PromptPO>()
                        .eq(PromptPO::getId, promptId)
                        .set(PromptPO::getStatus, 0));
    }
    
    // ==================== 竞技场会话管理 ====================
    
    /**
     * 获取所有竞技场会话列表（分页）
     */
    public List<ArenaSessionPO> getAllArenaSessions(int page, int size) {
        log.info("获取所有竞技场会话列表: page={}, size={}", page, size);
        
        Page<ArenaSessionPO> pageResult = arenaSessionMapper.selectPage(
                new Page<>(page + 1, size),
                new LambdaQueryWrapper<ArenaSessionPO>().orderByDesc(ArenaSessionPO::getCreatedAt));
        
        return pageResult.getRecords();
    }
    
    /**
     * 获取竞技场会话总数
     */
    public long getArenaSessionCount() {
        return arenaSessionMapper.selectCount(null);
    }
    
    // ==================== 登录日志管理 ====================
    
    private final LoginAuditLogMapper loginAuditLogMapper;
    
    /**
     * 获取所有登录日志列表（分页）
     */
    public List<LoginAuditLogPO> getAllLoginLogs(int page, int size) {
        log.info("获取所有登录日志列表: page={}, size={}", page, size);
        
        Page<LoginAuditLogPO> pageResult = loginAuditLogMapper.selectPage(
                new Page<>(page + 1, size),
                new LambdaQueryWrapper<LoginAuditLogPO>().orderByDesc(LoginAuditLogPO::getCreatedAt));
        
        return pageResult.getRecords();
    }
    
    /**
     * 获取登录日志总数
     */
    public long getLoginLogCount() {
        return loginAuditLogMapper.selectCount(null);
    }
}

