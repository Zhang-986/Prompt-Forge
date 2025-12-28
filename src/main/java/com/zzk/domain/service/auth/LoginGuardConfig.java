package com.zzk.domain.service.auth;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * 登录防护配置
 * 
 * <p>支持从 application.yml 读取阈值配置：
 * <pre>
 * login:
 *   guard:
 *     captcha-trigger-count: 3
 *     short-ban-trigger-count: 5
 *     long-ban-trigger-count: 10
 *     short-ban-duration: 15m
 *     long-ban-duration: 24h
 * </pre>
 * 
 * @author zzk
 * @since 1.0.0
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "login.guard")
public class LoginGuardConfig {

    /**
     * 触发验证码的失败次数阈值
     */
    private int captchaTriggerCount = 3;

    /**
     * 触发短期封禁的失败次数阈值
     */
    private int shortBanTriggerCount = 5;

    /**
     * 触发长期封禁的失败次数阈值
     */
    private int longBanTriggerCount = 10;

    /**
     * 短期封禁时长（默认15分钟）
     */
    private Duration shortBanDuration = Duration.ofMinutes(15);

    /**
     * 长期封禁时长（默认24小时）
     */
    private Duration longBanDuration = Duration.ofHours(24);

    /**
     * 失败计数器过期时间（默认24小时）
     */
    private Duration counterExpiration = Duration.ofHours(24);

    /**
     * 验证码有效期（默认5分钟）
     */
    private Duration captchaExpiration = Duration.ofMinutes(5);
}
