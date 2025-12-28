package com.zzk.domain.service.auth;

import com.zzk.domain.model.entity.LoginAuditLog;
import com.zzk.domain.model.valueobject.CaptchaResult;
import com.zzk.domain.model.valueobject.LoginAttemptInfo;
import com.zzk.domain.repository.LoginAuditLogRepository;
import com.zzk.domain.repository.LoginGuardRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * 登录防护领域服务
 * 
 * <p>实现阶梯式登录防御机制：
 * <ul>
 *   <li>失败 N1 次：触发验证码</li>
 *   <li>失败 N2 次：短期封禁</li>
 *   <li>失败 N3 次：长期封禁</li>
 * </ul>
 * 
 * <p>阈值从 {@link LoginGuardConfig} 读取，支持动态配置。
 * 
 * @author zzk
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LoginGuardService {

    private final LoginGuardRepository guardRepository;
    private final LoginAuditLogRepository auditLogRepository;
    private final CaptchaService captchaService;
    private final LoginGuardConfig config;
    private final IpGeoService ipGeoService;

    /**
     * 构建防护键
     * 
     * @param ip       客户端IP
     * @param username 用户名
     * @return 防护键（格式：IP:Username）
     */
    public String buildGuardKey(String ip, String username) {
        return ip + ":" + username;
    }

    /**
     * 登录前检查
     * 
     * <p>检查当前 IP+账号 是否被封禁、是否需要验证码。
     * 
     * @param ip       客户端IP
     * @param username 用户名
     * @return 登录尝试信息
     */
    public LoginAttemptInfo preLoginCheck(String ip, String username) {
        String guardKey = buildGuardKey(ip, username);
        
        // 1. 检查是否被封禁
        Optional<LocalDateTime> banExpiration = guardRepository.getBanExpiration(guardKey);
        if (banExpiration.isPresent()) {
            int failureCount = guardRepository.getFailureCount(guardKey);
            log.warn("登录被封禁: guardKey={}, 解封时间={}", guardKey, banExpiration.get());
            return LoginAttemptInfo.banned(guardKey, failureCount, banExpiration.get());
        }
        
        // 2. 检查失败次数，判断是否需要验证码
        int failureCount = guardRepository.getFailureCount(guardKey);
        boolean captchaRequired = failureCount >= config.getCaptchaTriggerCount();
        
        log.debug("登录前检查: guardKey={}, failureCount={}, captchaRequired={}", 
                guardKey, failureCount, captchaRequired);
        
        return LoginAttemptInfo.normal(guardKey, failureCount, captchaRequired);
    }

    /**
     * 记录登录失败
     * 
     * <p>原子递增失败计数，并根据阈值判断是否需要封禁。
     * 
     * @param ip       客户端IP
     * @param username 用户名
     * @param reason   失败原因
     * @return 更新后的登录尝试信息
     */
    public LoginAttemptInfo recordFailure(String ip, String username, String reason) {
        String guardKey = buildGuardKey(ip, username);
        
        // 原子递增失败计数
        int newCount = guardRepository.incrementFailureCount(guardKey, config.getCounterExpiration());
        log.info("登录失败记录: guardKey={}, newCount={}, reason={}", guardKey, newCount, reason);
        
        // 判断是否需要封禁
        if (newCount >= config.getLongBanTriggerCount()) {
            // 长期封禁
            guardRepository.setBan(guardKey, config.getLongBanDuration());
            LocalDateTime bannedUntil = LocalDateTime.now().plus(config.getLongBanDuration());
            log.warn("触发长期封禁: guardKey={}, duration={}", guardKey, config.getLongBanDuration());
            return LoginAttemptInfo.banned(guardKey, newCount, bannedUntil);
            
        } else if (newCount >= config.getShortBanTriggerCount()) {
            // 短期封禁
            guardRepository.setBan(guardKey, config.getShortBanDuration());
            LocalDateTime bannedUntil = LocalDateTime.now().plus(config.getShortBanDuration());
            log.warn("触发短期封禁: guardKey={}, duration={}", guardKey, config.getShortBanDuration());
            return LoginAttemptInfo.banned(guardKey, newCount, bannedUntil);
        }
        
        // 未封禁，但可能需要验证码
        boolean captchaRequired = newCount >= config.getCaptchaTriggerCount();
        return LoginAttemptInfo.normal(guardKey, newCount, captchaRequired);
    }

    /**
     * 记录登录成功
     * 
     * <p>清除失败计数器。
     * 
     * @param ip       客户端IP
     * @param username 用户名
     */
    public void recordSuccess(String ip, String username) {
        String guardKey = buildGuardKey(ip, username);
        guardRepository.clearFailureCount(guardKey);
        log.info("登录成功，清除失败计数: guardKey={}", guardKey);
    }

    /**
     * 生成验证码
     * 
     * @return 验证码结果
     */
    public CaptchaResult generateCaptcha() {
        return captchaService.generateCaptcha();
    }

    /**
     * 校验验证码
     * 
     * @param captchaKey  验证码Key
     * @param captchaCode 用户输入的验证码
     * @return true 表示校验通过
     */
    public boolean verifyCaptcha(String captchaKey, String captchaCode) {
        if (captchaKey == null || captchaCode == null) {
            return false;
        }
        return captchaService.verifyCaptcha(captchaKey, captchaCode);
    }

    /**
     * 记录审计日志
     * 
     * <p>自动根据 IP 地址填充地理位置信息。
     * 
     * @param auditLog 审计日志
     */
    public void logAudit(LoginAuditLog auditLog) {
        try {
            // 自动填充地理位置
            if (auditLog.getGeoLocation() == null && auditLog.getIpAddress() != null) {
                String geoLocation = ipGeoService.getFormattedLocation(auditLog.getIpAddress());
                auditLog.setGeoLocation(geoLocation);
            }
            
            auditLogRepository.save(auditLog);
            log.debug("审计日志已记录: username={}, result={}, geoLocation={}", 
                    auditLog.getUsername(), auditLog.getResult(), auditLog.getGeoLocation());
        } catch (Exception e) {
            // 审计日志记录失败不应影响主流程
            log.error("审计日志记录失败: {}", e.getMessage(), e);
        }
    }
}
