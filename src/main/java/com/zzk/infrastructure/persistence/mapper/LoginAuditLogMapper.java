package com.zzk.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zzk.infrastructure.persistence.po.LoginAuditLogPO;
import org.apache.ibatis.annotations.Mapper;

/**
 * 登录审计日志 Mapper
 * 
 * @author zzk
 * @since 1.0.0
 */
@Mapper
public interface LoginAuditLogMapper extends BaseMapper<LoginAuditLogPO> {
}
