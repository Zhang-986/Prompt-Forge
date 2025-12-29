package com.zzk.domain.repository;

import com.zzk.domain.model.entity.UserPreference;

import java.util.Optional;

/**
 * 用户偏好画像仓储接口
 * 
 * <p>持久化存储用户画像，用于个性化推荐。
 * 
 * @author zzk
 * @since 1.0.0
 */
public interface UserPreferenceRepository {

    /**
     * 保存用户偏好
     * 
     * @param preference 用户偏好
     */
    void save(UserPreference preference);

    /**
     * 根据用户ID查找偏好
     * 
     * @param userId 用户ID
     * @return 用户偏好
     */
    Optional<UserPreference> findByUserId(Long userId);

    /**
     * 获取或创建用户偏好
     * 
     * @param userId 用户ID
     * @return 用户偏好（不存在则创建新的）
     */
    default UserPreference getOrCreate(Long userId) {
        return findByUserId(userId).orElseGet(() -> {
            UserPreference newPref = UserPreference.createNew(userId);
            save(newPref);
            return newPref;
        });
    }
}
