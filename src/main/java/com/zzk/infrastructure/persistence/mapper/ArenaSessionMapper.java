package com.zzk.infrastructure.persistence.mapper;

import com.zzk.infrastructure.persistence.po.ArenaSessionPO;
import org.apache.ibatis.annotations.*;

import java.util.List;

/**
 * 竞技场会话 Mapper
 * 
 * @author zzk
 * @since 1.0.0
 */
@Mapper
public interface ArenaSessionMapper {
    
    @Insert("INSERT INTO arena_sessions (prompt_version_id, final_prompt, variables, models, status, creator_id) " +
            "VALUES (#{promptVersionId}, #{finalPrompt}, #{variables}, #{models}, #{status}, #{creatorId})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(ArenaSessionPO session);
    
    @Select("SELECT * FROM arena_sessions WHERE id = #{id}")
    ArenaSessionPO selectById(Long id);
    
    @Select("SELECT * FROM arena_sessions WHERE creator_id = #{creatorId} ORDER BY created_at DESC LIMIT #{limit}")
    List<ArenaSessionPO> selectByCreatorId(@Param("creatorId") Long creatorId, @Param("limit") int limit);
    
    @Update("UPDATE arena_sessions SET status = #{status}, completed_at = NOW() WHERE id = #{id}")
    int updateStatus(@Param("id") Long id, @Param("status") String status);
}
