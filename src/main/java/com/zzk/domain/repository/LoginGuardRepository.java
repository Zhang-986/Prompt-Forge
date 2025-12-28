package com.zzk.domain.repository;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;

/**
 * 登录防护仓储接口
 * 
 * <p>定义登录限流器的存储抽象，由基础设施层使用 Redisson 实现。
 * 
 * @author zzk
 * @since 1.0.0
 */
public interface LoginGuardRepository {

    /**
     * 原子递增失败计数
     * 
     * @param guardKey   防护键（格式：IP:Username）
     * @param expiration 计数器过期时间
     * @return 递增后的新值
     */
    int incrementFailureCount(String guardKey, Duration expiration);

    /**
     * 获取当前失败计数
     * 
     * @param guardKey 防护键
     * @return 当前失败次数
     */
    int getFailureCount(String guardKey);

    /**
     * 清除失败计数
     * 
     * @param guardKey 防护键
     */
    void clearFailureCount(String guardKey);

    /**
     * 设置封禁
     * 
     * @param guardKey 防护键
     * @param duration 封禁时长
     */
    void setBan(String guardKey, Duration duration);

    /**
     * 获取封禁截止时间
     * 
     * @param guardKey 防护键
     * @return 封禁截止时间，未封禁时返回 empty
     */
    Optional<LocalDateTime> getBanExpiration(String guardKey);

    /**
     * 检查是否被封禁
     * 
     * @param guardKey 防护键
     * @return true 表示被封禁
     */
    default boolean isBanned(String guardKey) {
        return getBanExpiration(guardKey).isPresent();
    }
}
