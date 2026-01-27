package com.zzk.domain.model.entity;

import com.zzk.domain.model.valueobject.LoginResult;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 登录审计日志实体
 * 
 * <p>
 * 记录每次登录尝试的详细信息，用于安全审计和异常行为分析。
 * 
 * @author zzk
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginAuditLog {

    /**
     * 日志ID
     */
    private Long id;

    /**
     * 用户名
     */
    private String username;

    /**
     * 客户端IP地址
     */
    private String ipAddress;

    /**
     * 地理位置（由IP解析）
     */
    private String geoLocation;

    /**
     * 设备指纹
     */
    private String deviceFingerprint;

    /**
     * User-Agent
     */
    private String userAgent;

    /**
     * 登录结果
     */
    private LoginResult result;

    /**
     * 失败原因（仅失败时填写）
     */
    private String failureReason;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 创建成功日志的工厂方法
     */
    public static LoginAuditLog success(String username, String ipAddress, String userAgent) {
        return LoginAuditLog.builder()
                .username(username)
                .ipAddress(ipAddress)
                .userAgent(userAgent)
                .result(LoginResult.SUCCESS)
                .createdAt(LocalDateTime.now())
                .build();
    }

    /**
     * 创建失败日志的工厂方法
     */
    public static LoginAuditLog failure(String username, String ipAddress, String userAgent,
            LoginResult result, String reason) {
        return LoginAuditLog.builder()
                .username(username)
                .ipAddress(ipAddress)
                .userAgent(userAgent)
                .result(result)
                .failureReason(reason)
                .createdAt(LocalDateTime.now())
                .build();
    }

    /**
     * 创建注册后自动登录日志的工厂方法
     */
    public static LoginAuditLog registerSuccess(String username, String ipAddress, String userAgent) {
        return LoginAuditLog.builder()
                .username(username)
                .ipAddress(ipAddress)
                .userAgent(userAgent)
                .result(LoginResult.REGISTER_SUCCESS)
                .createdAt(LocalDateTime.now())
                .build();
    }
}
