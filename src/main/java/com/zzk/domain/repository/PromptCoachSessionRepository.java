package com.zzk.domain.repository;

import com.zzk.domain.model.entity.PromptCoachSession;

import java.util.Optional;

/**
 * Prompt Coach 会话仓储接口
 * 
 * <p>会话临时存储，TTL 2 小时。
 * 
 * @author zzk
 * @since 1.0.0
 */
public interface PromptCoachSessionRepository {

    /**
     * 保存会话
     * 
     * @param session 会话实体
     */
    void save(PromptCoachSession session);

    /**
     * 根据 sessionId 查找会话
     * 
     * @param sessionId 会话ID
     * @return 会话（可能不存在或已过期）
     */
    Optional<PromptCoachSession> findById(String sessionId);

    /**
     * 删除会话
     * 
     * @param sessionId 会话ID
     */
    void delete(String sessionId);

    /**
     * 刷新会话过期时间
     * 
     * @param sessionId 会话ID
     */
    void refreshTtl(String sessionId);
}
