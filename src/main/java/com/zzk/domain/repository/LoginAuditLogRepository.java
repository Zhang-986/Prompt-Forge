package com.zzk.domain.repository;

import com.zzk.domain.model.entity.LoginAuditLog;

import java.util.List;

/**
 * 登录审计日志仓储接口
 * 
 * @author zzk
 * @since 1.0.0
 */
public interface LoginAuditLogRepository {

    /**
     * 保存审计日志
     * 
     * @param log 审计日志
     */
    void save(LoginAuditLog log);

    /**
     * 根据用户名查询审计日志
     * 
     * @param username 用户名
     * @param limit    限制条数
     * @return 审计日志列表
     */
    List<LoginAuditLog> findByUsername(String username, int limit);

    /**
     * 根据IP地址查询审计日志
     * 
     * @param ipAddress IP地址
     * @param limit     限制条数
     * @return 审计日志列表
     */
    List<LoginAuditLog> findByIpAddress(String ipAddress, int limit);
}
