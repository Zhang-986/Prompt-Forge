package com.zzk.infrastructure.persistence.repository;

import com.zzk.domain.model.entity.Tag;
import com.zzk.domain.repository.TagRepository;
import com.zzk.infrastructure.persistence.mapper.TagMapper;
import com.zzk.infrastructure.persistence.po.TagPO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 标签仓储实现
 * 
 * @author zzk
 * @since 1.0.0
 */
@Repository
@RequiredArgsConstructor
public class TagRepositoryImpl implements TagRepository {
    
    private final TagMapper tagMapper;
    
    @Override
    public Tag save(Tag tag) {
        TagPO po = toTagPO(tag);
        if (tag.getId() == null) {
            tagMapper.insert(po);
            tag.setId(po.getId());
        } else {
            tagMapper.update(po);
        }
        return tag;
    }
    
    @Override
    public Optional<Tag> findById(Long id) {
        TagPO po = tagMapper.selectById(id);
        return Optional.ofNullable(po).map(this::toTag);
    }
    
    @Override
    public List<Tag> findByWorkspaceId(Long workspaceId) {
        return tagMapper.selectByWorkspaceId(workspaceId).stream()
                .map(this::toTag)
                .collect(Collectors.toList());
    }
    
    @Override
    public Optional<Tag> findByNameAndWorkspaceId(String name, Long workspaceId) {
        TagPO po = tagMapper.selectByNameAndWorkspaceId(name, workspaceId);
        return Optional.ofNullable(po).map(this::toTag);
    }
    
    @Override
    public void deleteById(Long id) {
        tagMapper.deleteById(id);
    }
    
    @Override
    public void addTagToPrompt(Long promptId, Long tagId) {
        tagMapper.addTagToPrompt(promptId, tagId);
    }
    
    @Override
    public void removeTagFromPrompt(Long promptId, Long tagId) {
        tagMapper.removeTagFromPrompt(promptId, tagId);
    }
    
    @Override
    public List<Tag> findTagsByPromptId(Long promptId) {
        return tagMapper.selectTagsByPromptId(promptId).stream()
                .map(this::toTag)
                .collect(Collectors.toList());
    }
    
    @Override
    public List<Long> findPromptIdsByTagId(Long tagId) {
        return tagMapper.selectPromptIdsByTagId(tagId);
    }
    
    @Override
    public void removeAllTagRelations(Long tagId) {
        tagMapper.removeAllTagRelations(tagId);
    }
    
    // ==================== 转换方法 ====================
    
    private Tag toTag(TagPO po) {
        return Tag.builder()
                .id(po.getId())
                .name(po.getName())
                .color(po.getColor())
                .creatorId(po.getCreatorId())
                .workspaceId(po.getWorkspaceId())
                .createdAt(po.getCreatedAt())
                .build();
    }
    
    private TagPO toTagPO(Tag tag) {
        return TagPO.builder()
                .id(tag.getId())
                .name(tag.getName())
                .color(tag.getColor())
                .creatorId(tag.getCreatorId())
                .workspaceId(tag.getWorkspaceId())
                .createdAt(tag.getCreatedAt())
                .build();
    }
}
