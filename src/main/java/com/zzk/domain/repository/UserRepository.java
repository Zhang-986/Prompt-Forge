package com.zzk.domain.repository;

import com.zzk.domain.model.aggregate.User;

import java.util.Optional;

/**
 * 用户仓储接口
 * 
 * @author zzk
 * @since 1.0.0
 */
public interface UserRepository {

    /**
     * 根据 ID 查询
     */
    Optional<User> findById(Long id);

    /**
     * 根据用户名查询
     */
    Optional<User> findByUsername(String username);

    /**
     * 根据邮箱查询
     */
    Optional<User> findByEmail(String email);

    /**
     * 保存
     */
    void save(User user);

    /**
     * 检查用户名是否存在
     */
    boolean existsByUsername(String username);

    /**
     * 检查邮箱是否存在
     */
    boolean existsByEmail(String email);

    /**
     * 删除用户
     */
    void deleteById(Long id);
}
