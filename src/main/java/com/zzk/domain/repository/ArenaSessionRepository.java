package com.zzk.domain.repository;

import com.zzk.domain.model.entity.ArenaSession;
import java.util.List;
import java.util.Optional;

/**
 * 竞技场会话仓储接口
 * 
 * @author zzk
 * @since 1.0.0
 */
public interface ArenaSessionRepository {
    
    /**
     * 保存会话
     */
    ArenaSession save(ArenaSession session);
    
    /**
     * 根据 ID 查询
     */
    Optional<ArenaSession> findById(Long id);
    
    /**
     * 根据创建者查询最近会话
     */
    List<ArenaSession> findByCreatorIdOrderByCreatedAtDesc(Long creatorId, int limit);
    
    /**
     * 更新会话状态
     */
    void updateStatus(Long sessionId, String status);
}
