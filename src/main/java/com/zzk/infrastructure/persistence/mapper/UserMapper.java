package com.zzk.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zzk.infrastructure.persistence.po.UserPO;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户 Mapper
 * 
 * @author zzk
 * @since 1.0.0
 */
@Mapper
public interface UserMapper extends BaseMapper<UserPO> {
}
