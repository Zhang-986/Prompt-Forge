package com.zzk.infrastructure.persistence.mapper;

import com.zzk.infrastructure.persistence.po.ArenaResultPO;
import org.apache.ibatis.annotations.*;

import java.util.List;

/**
 * 竞技场结果 Mapper
 * 
 * @author zzk
 * @since 1.0.0
 */
@Mapper
public interface ArenaResultMapper {
    
    @Insert("INSERT INTO arena_results (session_id, model_id, content, tokens_used, latency_ms, status, error_message) " +
            "VALUES (#{sessionId}, #{modelId}, #{content}, #{tokensUsed}, #{latencyMs}, #{status}, #{errorMessage})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(ArenaResultPO result);
    
    @Select("SELECT * FROM arena_results WHERE session_id = #{sessionId} ORDER BY created_at")
    List<ArenaResultPO> selectBySessionId(Long sessionId);
    
    @Update("UPDATE arena_results SET content = #{content}, tokens_used = #{tokensUsed}, " +
            "latency_ms = #{latencyMs}, status = #{status}, error_message = #{errorMessage} WHERE id = #{id}")
    int update(ArenaResultPO result);
}
