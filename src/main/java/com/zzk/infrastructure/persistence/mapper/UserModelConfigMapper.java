package com.zzk.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zzk.infrastructure.persistence.po.UserModelConfigPO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * 用户模型配置 Mapper
 * 
 * @author zzk
 * @since 1.0.0
 */
@Mapper
public interface UserModelConfigMapper extends BaseMapper<UserModelConfigPO> {

    /**
     * 根据用户ID查询所有配置
     */
    default List<UserModelConfigPO> findByUserId(Long userId) {
        return selectList(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<UserModelConfigPO>()
                .eq(UserModelConfigPO::getUserId, userId)
                .orderByDesc(UserModelConfigPO::getCreatedAt));
    }

    /**
     * 根据用户ID和提供商查询配置
     */
    default UserModelConfigPO findByUserIdAndProvider(Long userId, String provider) {
        return selectOne(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<UserModelConfigPO>()
                .eq(UserModelConfigPO::getUserId, userId)
                .eq(UserModelConfigPO::getProvider, provider));
    }

    /**
     * 查询用户启用的配置
     */
    default List<UserModelConfigPO> findEnabledByUserId(Long userId) {
        return selectList(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<UserModelConfigPO>()
                .eq(UserModelConfigPO::getUserId, userId)
                .eq(UserModelConfigPO::getEnabled, 1));
    }
}
