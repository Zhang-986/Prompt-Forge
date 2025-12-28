package com.zzk.infrastructure.persistence.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zzk.domain.model.entity.LoginAuditLog;
import com.zzk.domain.model.valueobject.LoginResult;
import com.zzk.domain.repository.LoginAuditLogRepository;
import com.zzk.infrastructure.persistence.mapper.LoginAuditLogMapper;
import com.zzk.infrastructure.persistence.po.LoginAuditLogPO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 登录审计日志仓储实现
 * 
 * @author zzk
 * @since 1.0.0
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class LoginAuditLogRepositoryImpl implements LoginAuditLogRepository {

    private final LoginAuditLogMapper mapper;

    @Override
    public void save(LoginAuditLog auditLog) {
        LoginAuditLogPO po = toPO(auditLog);
        mapper.insert(po);
        auditLog.setId(po.getId());
        log.debug("审计日志已保存: id={}", po.getId());
    }

    @Override
    public List<LoginAuditLog> findByUsername(String username, int limit) {
        LambdaQueryWrapper<LoginAuditLogPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(LoginAuditLogPO::getUsername, username)
               .orderByDesc(LoginAuditLogPO::getCreatedAt)
               .last("LIMIT " + limit);
        
        return mapper.selectList(wrapper).stream()
                .map(this::toEntity)
                .collect(Collectors.toList());
    }

    @Override
    public List<LoginAuditLog> findByIpAddress(String ipAddress, int limit) {
        LambdaQueryWrapper<LoginAuditLogPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(LoginAuditLogPO::getIpAddress, ipAddress)
               .orderByDesc(LoginAuditLogPO::getCreatedAt)
               .last("LIMIT " + limit);
        
        return mapper.selectList(wrapper).stream()
                .map(this::toEntity)
                .collect(Collectors.toList());
    }

    // ==================== 转换方法 ====================

    private LoginAuditLogPO toPO(LoginAuditLog entity) {
        return LoginAuditLogPO.builder()
                .id(entity.getId())
                .username(entity.getUsername())
                .ipAddress(entity.getIpAddress())
                .geoLocation(entity.getGeoLocation())
                .deviceFingerprint(entity.getDeviceFingerprint())
                .userAgent(entity.getUserAgent())
                .result(entity.getResult() != null ? entity.getResult().name() : null)
                .failureReason(entity.getFailureReason())
                .createdAt(entity.getCreatedAt())
                .build();
    }

    private LoginAuditLog toEntity(LoginAuditLogPO po) {
        return LoginAuditLog.builder()
                .id(po.getId())
                .username(po.getUsername())
                .ipAddress(po.getIpAddress())
                .geoLocation(po.getGeoLocation())
                .deviceFingerprint(po.getDeviceFingerprint())
                .userAgent(po.getUserAgent())
                .result(po.getResult() != null ? LoginResult.valueOf(po.getResult()) : null)
                .failureReason(po.getFailureReason())
                .createdAt(po.getCreatedAt())
                .build();
    }
}
