package com.zzk.infrastructure.persistence.repository;

import com.zzk.domain.repository.LoginGuardRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RAtomicLong;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * 登录防护仓储 Redisson 实现
 * 
 * <p>使用 Redisson 的原子计数器和分布式缓存实现高并发下的原子性保证。
 * 
 * @author zzk
 * @since 1.0.0
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class RedissonLoginGuardRepository implements LoginGuardRepository {

    private final RedissonClient redissonClient;

    /**
     * 失败计数器 Key 前缀
     */
    private static final String FAILURE_COUNT_PREFIX = "login:failure:";

    /**
     * 封禁标记 Key 前缀
     */
    private static final String BAN_PREFIX = "login:ban:";

    @Override
    public int incrementFailureCount(String guardKey, Duration expiration) {
        String key = FAILURE_COUNT_PREFIX + guardKey;
        RAtomicLong counter = redissonClient.getAtomicLong(key);
        
        // 原子递增
        long newValue = counter.incrementAndGet();
        
        // 首次设置过期时间
        if (newValue == 1) {
            counter.expire(expiration);
            log.debug("设置计数器过期时间: key={}, expiration={}", key, expiration);
        }
        
        log.debug("失败计数递增: key={}, newValue={}", key, newValue);
        return (int) newValue;
    }

    @Override
    public int getFailureCount(String guardKey) {
        String key = FAILURE_COUNT_PREFIX + guardKey;
        RAtomicLong counter = redissonClient.getAtomicLong(key);
        return (int) counter.get();
    }

    @Override
    public void clearFailureCount(String guardKey) {
        String key = FAILURE_COUNT_PREFIX + guardKey;
        redissonClient.getAtomicLong(key).delete();
        log.debug("清除失败计数: key={}", key);
    }

    @Override
    public void setBan(String guardKey, Duration duration) {
        String key = BAN_PREFIX + guardKey;
        LocalDateTime bannedUntil = LocalDateTime.now().plus(duration);
        
        RBucket<String> bucket = redissonClient.getBucket(key);
        bucket.set(bannedUntil.toString(), duration.toMillis(), TimeUnit.MILLISECONDS);
        
        log.info("设置封禁: key={}, bannedUntil={}", key, bannedUntil);
    }

    @Override
    public Optional<LocalDateTime> getBanExpiration(String guardKey) {
        String key = BAN_PREFIX + guardKey;
        RBucket<String> bucket = redissonClient.getBucket(key);
        String value = bucket.get();
        
        if (value == null) {
            return Optional.empty();
        }
        
        try {
            LocalDateTime bannedUntil = LocalDateTime.parse(value);
            return Optional.of(bannedUntil);
        } catch (Exception e) {
            log.warn("解析封禁时间失败: key={}, value={}", key, value, e);
            return Optional.empty();
        }
    }
}
