package com.zzk.infrastructure.persistence.repository;

import com.alibaba.fastjson2.JSON;
import com.zzk.domain.model.entity.PromptCoachSession;
import com.zzk.domain.repository.PromptCoachSessionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.util.Optional;

/**
 * Prompt Coach 会话 Redis 仓储实现
 * 
 * <p>会话存储在 Redis 中，TTL 2 小时。
 * 
 * @author zzk
 * @since 1.0.0
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class RedisPromptCoachSessionRepository implements PromptCoachSessionRepository {

    private final RedissonClient redissonClient;

    private static final String KEY_PREFIX = "prompt:coach:";
    private static final Duration DEFAULT_TTL = Duration.ofHours(2);

    @Override
    public void save(PromptCoachSession session) {
        String key = KEY_PREFIX + session.getSessionId();
        RBucket<String> bucket = redissonClient.getBucket(key);
        String json = JSON.toJSONString(session);
        bucket.set(json, DEFAULT_TTL);
        log.debug("保存 Coach 会话: sessionId={}", session.getSessionId());
    }

    @Override
    public Optional<PromptCoachSession> findById(String sessionId) {
        String key = KEY_PREFIX + sessionId;
        RBucket<String> bucket = redissonClient.getBucket(key);
        String json = bucket.get();
        
        if (json == null) {
            log.debug("Coach 会话不存在或已过期: sessionId={}", sessionId);
            return Optional.empty();
        }
        
        try {
            PromptCoachSession session = JSON.parseObject(json, PromptCoachSession.class);
            return Optional.ofNullable(session);
        } catch (Exception e) {
            log.error("解析 Coach 会话失败: sessionId={}", sessionId, e);
            return Optional.empty();
        }
    }

    @Override
    public void delete(String sessionId) {
        String key = KEY_PREFIX + sessionId;
        redissonClient.getBucket(key).delete();
        log.debug("删除 Coach 会话: sessionId={}", sessionId);
    }

    @Override
    public void refreshTtl(String sessionId) {
        String key = KEY_PREFIX + sessionId;
        RBucket<String> bucket = redissonClient.getBucket(key);
        if (bucket.isExists()) {
            bucket.expire(DEFAULT_TTL);
            log.debug("刷新 Coach 会话 TTL: sessionId={}", sessionId);
        }
    }
}
