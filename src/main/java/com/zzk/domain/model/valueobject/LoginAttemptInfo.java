package com.zzk.domain.model.valueobject;

import java.time.LocalDateTime;

/**
 * 登录尝试信息值对象
 * 
 * <p>记录当前登录尝试的状态，包括失败次数、是否需要验证码、是否被封禁等。
 * 
 * @author zzk
 * @since 1.0.0
 */
public record LoginAttemptInfo(
    /**
     * 防护键（格式：IP:Username）
     */
    String guardKey,
    
    /**
     * 当前失败次数
     */
    int failureCount,
    
    /**
     * 是否需要验证码
     */
    boolean captchaRequired,
    
    /**
     * 是否被封禁
     */
    boolean banned,
    
    /**
     * 封禁截止时间（未封禁时为 null）
     */
    LocalDateTime bannedUntil
) {
    
    /**
     * 创建正常状态的登录尝试信息
     */
    public static LoginAttemptInfo normal(String guardKey, int failureCount, boolean captchaRequired) {
        return new LoginAttemptInfo(guardKey, failureCount, captchaRequired, false, null);
    }
    
    /**
     * 创建被封禁状态的登录尝试信息
     */
    public static LoginAttemptInfo banned(String guardKey, int failureCount, LocalDateTime bannedUntil) {
        return new LoginAttemptInfo(guardKey, failureCount, true, true, bannedUntil);
    }
}
