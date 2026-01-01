package com.zzk.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
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
public interface ArenaSessionMapper extends BaseMapper<ArenaSessionPO> {
    
    @Select("SELECT * FROM arena_sessions WHERE creator_id = #{creatorId} ORDER BY created_at DESC LIMIT #{limit}")
    List<ArenaSessionPO> selectByCreatorId(@Param("creatorId") Long creatorId, @Param("limit") int limit);
    
    @Update("UPDATE arena_sessions SET status = #{status}, completed_at = NOW() WHERE id = #{id}")
    int updateStatus(@Param("id") Long id, @Param("status") String status);
}

