package com.zzk.infrastructure.persistence.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 登录审计日志持久化对象
 * 
 * @author zzk
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("login_audit_log")
public class LoginAuditLogPO {

    /**
     * 日志ID
     */
    @TableId(type = IdType.AUTO)
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
     * 地理位置
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
    private String result;

    /**
     * 失败原因
     */
    private String failureReason;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;
}
