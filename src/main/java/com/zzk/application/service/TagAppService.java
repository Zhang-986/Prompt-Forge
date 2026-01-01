package com.zzk.application.service;

import com.zzk.domain.model.entity.Tag;
import com.zzk.domain.repository.TagRepository;
import com.zzk.infrastructure.annotation.SensitiveCheck;
import com.zzk.interfaces.dto.request.CreateTagRequest;
import com.zzk.interfaces.dto.response.TagDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 标签应用服务
 * 
 * @author zzk
 * @since 1.0.0
 */
@Service
@RequiredArgsConstructor
public class TagAppService {
    
    private final TagRepository tagRepository;
    
    /**
     * 创建标签
     */
    @SensitiveCheck
    @Transactional
    public TagDTO createTag(CreateTagRequest request, Long userId, Long workspaceId) {
        // 检查标签名是否已存在
        if (tagRepository.findByNameAndWorkspaceId(request.getName(), workspaceId).isPresent()) {
            throw new IllegalArgumentException("标签名称已存在");
        }
        
        Tag tag = Tag.builder()
                .name(request.getName())
                .color(request.getColor() != null ? request.getColor() : Tag.COLOR_PURPLE)
                .creatorId(userId)
                .workspaceId(workspaceId)
                .createdAt(LocalDateTime.now())
                .build();
        
        tag = tagRepository.save(tag);
        return toTagDTO(tag);
    }
    
    /**
     * 获取工作空间所有标签
     */
    public List<TagDTO> getTagsByWorkspace(Long workspaceId) {
        return tagRepository.findByWorkspaceId(workspaceId).stream()
                .map(this::toTagDTO)
                .collect(Collectors.toList());
    }
    
    /**
     * 获取 Prompt 的标签
     */
    public List<TagDTO> getTagsByPromptId(Long promptId) {
        return tagRepository.findTagsByPromptId(promptId).stream()
                .map(this::toTagDTO)
                .collect(Collectors.toList());
    }
    
    /**
     * 删除标签
     */
    @Transactional
    public void deleteTag(Long tagId) {
        // 先移除所有关联
        tagRepository.removeAllTagRelations(tagId);
        // 再删除标签
        tagRepository.deleteById(tagId);
    }
    
    /**
     * 为 Prompt 添加标签
     */
    @Transactional
    public void addTagToPrompt(Long promptId, Long tagId) {
        // 检查标签是否存在
        tagRepository.findById(tagId)
                .orElseThrow(() -> new IllegalArgumentException("标签不存在"));
        
        tagRepository.addTagToPrompt(promptId, tagId);
    }
    
    /**
     * 移除 Prompt 的标签
     */
    @Transactional
    public void removeTagFromPrompt(Long promptId, Long tagId) {
        tagRepository.removeTagFromPrompt(promptId, tagId);
    }
    
    /**
     * 批量设置 Prompt 的标签
     */
    @Transactional
    public void setPromptTags(Long promptId, List<Long> tagIds) {
        // 获取当前标签
        List<Long> currentTagIds = tagRepository.findTagsByPromptId(promptId).stream()
                .map(Tag::getId)
                .collect(Collectors.toList());
        
        // 需要移除的标签
        for (Long currentTagId : currentTagIds) {
            if (!tagIds.contains(currentTagId)) {
                tagRepository.removeTagFromPrompt(promptId, currentTagId);
            }
        }
        
        // 需要添加的标签
        for (Long tagId : tagIds) {
            if (!currentTagIds.contains(tagId)) {
                tagRepository.addTagToPrompt(promptId, tagId);
            }
        }
    }
    
    // ==================== DTO 转换 ====================
    
    private TagDTO toTagDTO(Tag tag) {
        return TagDTO.builder()
                .id(tag.getId())
                .name(tag.getName())
                .color(tag.getColor())
                .createdAt(tag.getCreatedAt())
                .build();
    }
}
