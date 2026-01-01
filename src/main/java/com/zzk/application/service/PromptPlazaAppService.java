package com.zzk.application.service;

import com.zzk.domain.model.aggregate.Prompt;
import com.zzk.domain.model.entity.PromptTemplate;
import com.zzk.domain.model.entity.PromptVersion;
import com.zzk.domain.repository.PromptRepository;
import com.zzk.domain.repository.PromptTemplateRepository;
import com.zzk.domain.repository.PromptVersionRepository;
import com.zzk.infrastructure.annotation.SensitiveCheck;
import com.zzk.infrastructure.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Prompt 广场应用服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PromptPlazaAppService {
    
    private final PromptTemplateRepository templateRepository;
    private final PromptRepository promptRepository;
    private final PromptVersionRepository versionRepository;
    
    /**
     * 获取模板列表
     */
    public List<PromptTemplate> getTemplates(String category) {
        if (category == null || category.isEmpty() || "ALL".equalsIgnoreCase(category)) {
            return templateRepository.findAllActive();
        }
        return templateRepository.findByCategory(category);
    }
    
    /**
     * 获取模板详情
     */
    public PromptTemplate getTemplate(Long id) {
        return templateRepository.findById(id)
                .orElseThrow(() -> new BusinessException("模板不存在: " + id));
    }
    
    /**
     * 克隆模板到工作空间
     */
    @Transactional(rollbackFor = Exception.class)
    public Prompt cloneTemplate(Long templateId, Long workspaceId, Long userId) {
        log.info("克隆模板: templateId={}, workspaceId={}, userId={}", templateId, workspaceId, userId);
        
        PromptTemplate template = templateRepository.findById(templateId)
                .orElseThrow(() -> new BusinessException("模板不存在: " + templateId));
        
        // 创建 Prompt
        Prompt prompt = Prompt.builder()
                .name(template.getName() + " (克隆)")
                .description(template.getDescription())
                .workspaceId(workspaceId)
                .creatorId(userId)
                .latestVersionNumber(1)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        
        promptRepository.save(prompt);
        
        // 创建初始版本
        PromptVersion version = PromptVersion.builder()
                .promptId(prompt.getId())
                .versionNumber(1)
                .content(template.getContent())
                .commitMessage("从广场克隆: " + template.getName())
                .authorId(userId)
                .createdAt(LocalDateTime.now())
                .build();
        
        versionRepository.save(version);
        
        // 更新 Prompt 的最新版本 ID
        prompt.setLatestVersionId(version.getId());
        promptRepository.save(prompt);
        
        // 增加克隆计数
        templateRepository.incrementCloneCount(templateId);
        
        log.info("模板克隆成功: promptId={}", prompt.getId());
        return prompt;
    }
    
    /**
     * 发布 Prompt 到广场
     */
    @SensitiveCheck
    @Transactional(rollbackFor = Exception.class)
    public PromptTemplate publishToPlaza(Long promptId, String category, Long userId, String authorName) {
        log.info("发布到广场: promptId={}, category={}, userId={}", promptId, category, userId);
        
        Prompt prompt = promptRepository.findById(promptId)
                .orElseThrow(() -> new BusinessException("Prompt 不存在: " + promptId));
        
        // 获取最新版本内容 - 使用版本列表获取第一个（最新的）
        List<PromptVersion> versions = versionRepository.findByPromptIdOrderByVersionNumberDesc(promptId);
        if (versions.isEmpty()) {
            throw new BusinessException("该 Prompt 还没有任何版本，请先提交内容");
        }
        PromptVersion latestVersion = versions.get(0);
        
        // 创建模板
        PromptTemplate template = PromptTemplate.builder()
                .name(prompt.getName())
                .description(prompt.getDescription())
                .content(latestVersion.getContent())
                .category(category)
                .authorId(userId)
                .authorName(authorName)
                .isOfficial(false)
                .isActive(true)
                .build();
        
        templateRepository.save(template);
        
        log.info("发布成功: templateId={}", template.getId());
        return template;
    }
}
