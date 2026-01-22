package com.zzk.domain.repository;

import com.zzk.domain.model.entity.Tag;
import java.util.List;
import java.util.Optional;

/**
 * 标签仓储接口
 * 
 * @author zzk
 * @since 1.0.0
 */
public interface TagRepository {

    /**
     * 保存标签
     */
    Tag save(Tag tag);

    /**
     * 根据 ID 查询
     */
    Optional<Tag> findById(Long id);

    /**
     * 根据工作空间查询所有标签
     */
    List<Tag> findByWorkspaceId(Long workspaceId);

    /**
     * 根据名称和工作空间查询
     */
    Optional<Tag> findByNameAndWorkspaceId(String name, Long workspaceId);

    /**
     * 删除标签
     */
    void deleteById(Long id);

    /**
     * 为 Prompt 添加标签
     */
    void addTagToPrompt(Long promptId, Long tagId);

    /**
     * 移除 Prompt 的标签
     */
    void removeTagFromPrompt(Long promptId, Long tagId);

    /**
     * 获取 Prompt 的所有标签
     */
    List<Tag> findTagsByPromptId(Long promptId);

    /**
     * 根据标签查询 Prompt ID 列表
     */
    List<Long> findPromptIdsByTagId(Long tagId);

    /**
     * 移除标签的所有关联
     */
    void removeAllTagRelations(Long tagId);

    /**
     * 批量获取工作空间内所有 Prompt 的标签 ID 映射
     * 
     * @return Map of promptId -> List of tagIds
     */
    java.util.Map<Long, java.util.List<Long>> findAllPromptTagMappingsByWorkspace(Long workspaceId);
}
