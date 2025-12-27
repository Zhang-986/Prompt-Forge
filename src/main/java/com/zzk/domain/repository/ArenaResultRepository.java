package com.zzk.domain.repository;

import com.zzk.domain.model.entity.ArenaResult;
import java.util.List;

/**
 * 竞技场结果仓储接口
 * 
 * @author zzk
 * @since 1.0.0
 */
public interface ArenaResultRepository {
    
    /**
     * 保存结果
     */
    ArenaResult save(ArenaResult result);
    
    /**
     * 根据会话 ID 查询所有结果
     */
    List<ArenaResult> findBySessionId(Long sessionId);
    
    /**
     * 批量保存结果
     */
    void saveAll(List<ArenaResult> results);
}
