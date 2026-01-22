package com.zzk.infrastructure.persistence.mapper;

import com.zzk.infrastructure.persistence.po.TagPO;
import org.apache.ibatis.annotations.*;

import java.util.List;

/**
 * 标签 Mapper
 * 
 * @author zzk
 * @since 1.0.0
 */
@Mapper
public interface TagMapper {

        @Insert("INSERT INTO prompt_tag (name, color, creator_id, workspace_id) " +
                        "VALUES (#{name}, #{color}, #{creatorId}, #{workspaceId})")
        @Options(useGeneratedKeys = true, keyProperty = "id")
        int insert(TagPO tag);

        @Select("SELECT * FROM prompt_tag WHERE id = #{id}")
        TagPO selectById(Long id);

        @Select("SELECT * FROM prompt_tag WHERE workspace_id = #{workspaceId} ORDER BY created_at DESC")
        List<TagPO> selectByWorkspaceId(Long workspaceId);

        @Select("SELECT * FROM prompt_tag WHERE name = #{name} AND workspace_id = #{workspaceId}")
        TagPO selectByNameAndWorkspaceId(@Param("name") String name, @Param("workspaceId") Long workspaceId);

        @Update("UPDATE prompt_tag SET name = #{name}, color = #{color} WHERE id = #{id}")
        int update(TagPO tag);

        @Delete("DELETE FROM prompt_tag WHERE id = #{id}")
        int deleteById(Long id);

        // ==================== 标签关联操作 ====================

        @Insert("INSERT INTO prompt_tag_relation (prompt_id, tag_id) VALUES (#{promptId}, #{tagId})")
        int addTagToPrompt(@Param("promptId") Long promptId, @Param("tagId") Long tagId);

        @Delete("DELETE FROM prompt_tag_relation WHERE prompt_id = #{promptId} AND tag_id = #{tagId}")
        int removeTagFromPrompt(@Param("promptId") Long promptId, @Param("tagId") Long tagId);

        @Select("SELECT t.* FROM prompt_tag t " +
                        "INNER JOIN prompt_tag_relation r ON t.id = r.tag_id " +
                        "WHERE r.prompt_id = #{promptId}")
        List<TagPO> selectTagsByPromptId(Long promptId);

        @Select("SELECT prompt_id FROM prompt_tag_relation WHERE tag_id = #{tagId}")
        List<Long> selectPromptIdsByTagId(Long tagId);

        @Delete("DELETE FROM prompt_tag_relation WHERE tag_id = #{tagId}")
        int removeAllTagRelations(Long tagId);

        @Delete("DELETE FROM prompt_tag_relation WHERE prompt_id = #{promptId}")
        int removeAllPromptTagRelations(Long promptId);

        /**
         * 批量获取工作空间内所有 Prompt 的标签关联
         * 返回 promptId 和 tagId 的映射列表
         */
        @Select("SELECT r.prompt_id as promptId, r.tag_id as tagId " +
                        "FROM prompt_tag_relation r " +
                        "INNER JOIN prompts p ON r.prompt_id = p.id " +
                        "WHERE p.workspace_id = #{workspaceId}")
        List<java.util.Map<String, Long>> selectAllPromptTagRelationsByWorkspace(Long workspaceId);
}
