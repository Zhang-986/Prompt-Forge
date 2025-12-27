package com.zzk.application.service;

import com.zzk.domain.model.aggregate.Prompt;
import com.zzk.domain.model.entity.PromptVersion;
import com.zzk.domain.model.entity.Workspace;
import com.zzk.domain.repository.PromptRepository;
import com.zzk.domain.repository.PromptVersionRepository;
import com.zzk.domain.service.PromptDomainService;
import com.zzk.infrastructure.exception.BusinessException;
import com.zzk.interfaces.dto.response.DiffResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Prompt 应用服务
 * 
 * <p>负责 Prompt 的 CRUD 和版本管理的业务编排
 * 
 * @author zzk
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PromptAppService {

    private final PromptRepository promptRepository;
    private final PromptVersionRepository versionRepository;
    private final PromptDomainService promptDomainService;
    private final com.zzk.domain.repository.WorkspaceRepository workspaceRepository;

    /**
     * 创建 Prompt
     * 
     * @param name 名称
     * @param description 描述
     * @param content 初始内容
     * @param workspaceId 工作空间 ID
     * @param userId 创建者 ID
     * @return 创建的 Prompt
     */
    @Transactional(rollbackFor = Exception.class)
    public Prompt createPrompt(String name, String description, String content, 
                                Long workspaceId, Long userId) {
        log.info("创建 Prompt: name={}, workspaceId={}", name, workspaceId);

        // 1. 创建 Prompt 聚合根
        Prompt prompt = Prompt.builder()
                .name(name)
                .description(description)
                .workspaceId(workspaceId)
                .creatorId(userId)
                .isPublic(false)
                .status(1)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        promptRepository.save(prompt);
        log.info("Prompt 创建成功: id={}", prompt.getId());

        // 2. 创建初始版本
        PromptVersion version = promptDomainService.commit(
                prompt.getId(), content, null, "初始版本", userId);
        
        log.info("初始版本创建成功: versionId={}", version.getId());

        return prompt;
    }

    /**
     * 获取 Prompt 详情
     */
    @Cacheable(value = "prompt", key = "#promptId", unless = "#result == null")
    public Prompt getPromptById(Long promptId) {
        return promptRepository.findById(promptId)
                .orElseThrow(() -> new BusinessException("Prompt 不存在: " + promptId));
    }

    /**
     * 获取 Prompt 列表（按工作空间）
     */
    public List<Prompt> getPromptsByWorkspace(Long workspaceId, Long userId) {
        // 检查权限
        Workspace workspace = workspaceRepository.findById(workspaceId)
                .orElseThrow(() -> new BusinessException("工作空间不存在"));
                
        if (!workspace.isOwner(userId) && !workspaceRepository.isMember(workspaceId, userId)) {
            throw new BusinessException("无权访问该工作空间");
        }

        List<Prompt> prompts = promptRepository.findByWorkspaceId(workspaceId);
        
        // 填充版本号
        for (Prompt prompt : prompts) {
            if (prompt.getLatestVersionId() != null) {
                versionRepository.findById(prompt.getLatestVersionId())
                    .ifPresent(v -> prompt.setLatestVersionNumber(v.getVersionNumber()));
            } else {
                prompt.setLatestVersionNumber(1);
            }
        }
        
        return prompts;
    }

    /**
     * 提交新版本
     */
    @CacheEvict(value = "prompt", key = "#promptId")
    @Transactional(rollbackFor = Exception.class)
    public PromptVersion commitVersion(Long promptId, String content, 
                                        Long parentVersionId, String commitMessage, Long userId) {
        log.info("提交新版本: promptId={}, parentVersionId={}", promptId, parentVersionId);

        // 检查权限
        Prompt prompt = getPromptById(promptId);
        if (!prompt.canEdit(userId)) {
            throw new BusinessException("没有编辑权限");
        }

        // 调用领域服务提交版本
        return promptDomainService.commit(promptId, content, parentVersionId, commitMessage, userId);
    }

    /**
     * 获取版本历史
     */
    public List<PromptVersion> getVersionHistory(Long promptId) {
        return promptDomainService.getVersionHistory(promptId);
    }

    /**
     * 获取最新版本
     */
    @Cacheable(value = "promptVersion", key = "'latest:' + #promptId", unless = "#result == null")
    public PromptVersion getLatestVersion(Long promptId) {
        Prompt prompt = getPromptById(promptId);
        if (prompt.getLatestVersionId() == null) {
            // 尝试查找最新版本（容错处理，用于修复旧数据或克隆失败的数据）
            List<PromptVersion> versions = versionRepository.findByPromptIdOrderByVersionNumberDesc(promptId);
            if (!versions.isEmpty()) {
                PromptVersion latest = versions.get(0);
                // 自动修复
                prompt.setLatestVersionId(latest.getId());
                promptRepository.save(prompt);
                return latest;
            }
            throw new BusinessException("Prompt 没有版本: " + promptId);
        }
        return versionRepository.findById(prompt.getLatestVersionId())
                .orElseThrow(() -> new BusinessException("版本不存在"));
    }

    /**
     * 回滚到指定版本
     */
    @CacheEvict(value = {"prompt", "promptVersion"}, allEntries = true)
    @Transactional(rollbackFor = Exception.class)
    public PromptVersion rollbackToVersion(Long promptId, Long targetVersionId, Long userId) {
        log.info("回滚版本: promptId={}, targetVersionId={}", promptId, targetVersionId);

        // 检查权限
        Prompt prompt = getPromptById(promptId);
        if (!prompt.canEdit(userId)) {
            throw new BusinessException("没有编辑权限");
        }

        return promptDomainService.rollback(promptId, targetVersionId, userId);
    }

    /**
     * 删除 Prompt（软删除）
     */
    @CacheEvict(value = "prompt", key = "#promptId")
    @Transactional(rollbackFor = Exception.class)
    public void deletePrompt(Long promptId, Long userId) {
        log.info("删除 Prompt: promptId={}", promptId);

        Prompt prompt = getPromptById(promptId);
        if (!prompt.canEdit(userId)) {
            throw new BusinessException("没有删除权限");
        }

        prompt.softDelete();
        promptRepository.deleteById(promptId);
    }

    /**
     * 更新 Prompt 信息（名称、描述）
     */
    @CacheEvict(value = "prompt", key = "#promptId")
    @Transactional(rollbackFor = Exception.class)
    public Prompt updatePromptInfo(Long promptId, String name, String description, Long userId) {
        log.info("更新 Prompt 信息: promptId={}", promptId);

        Prompt prompt = getPromptById(promptId);
        if (!prompt.canEdit(userId)) {
            throw new BusinessException("没有编辑权限");
        }

        prompt.setName(name);
        prompt.setDescription(description);
        prompt.setUpdatedAt(LocalDateTime.now());
        promptRepository.save(prompt);

        return prompt;
    }

    /**
     * 获取两个版本之间的 Diff
     * 
     * @param versionId1 版本 1 ID（源版本）
     * @param versionId2 版本 2 ID（目标版本）
     * @return Diff 结果
     */
    public DiffResult getVersionDiff(Long versionId1, Long versionId2) {
        return promptDomainService.getVersionDiff(versionId1, versionId2);
    }
}
