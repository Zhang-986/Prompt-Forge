package com.zzk.domain.repository;

import com.zzk.domain.model.entity.UserModelConfig;

import java.util.List;
import java.util.Optional;

/**
 * 用户模型配置仓储接口
 * 
 * @author zzk
 * @since 1.0.0
 */
public interface UserModelConfigRepository {

    /**
     * 根据ID查询配置
     */
    Optional<UserModelConfig> findById(Long id);

    /**
     * 根据用户ID查询所有配置
     */
    List<UserModelConfig> findByUserId(Long userId);

    /**
     * 根据用户ID和提供商查询配置
     */
    Optional<UserModelConfig> findByUserIdAndProvider(Long userId, String provider);

    /**
     * 查询用户启用的配置
     */
    List<UserModelConfig> findEnabledByUserId(Long userId);

    /**
     * 保存配置
     */
    UserModelConfig save(UserModelConfig config);

    /**
     * 更新配置
     */
    UserModelConfig update(UserModelConfig config);

    /**
     * 删除配置
     */
    void deleteById(Long id);
}
