package com.zzk.infrastructure.config;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zzk.infrastructure.persistence.mapper.UserMapper;
import com.zzk.infrastructure.persistence.po.UserPO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 数据初始化
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UserMapper userMapper;
    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(String... args) throws Exception {
        initAdminUser();
    }

    /**
     * 初始化管理员用户
     * 防止数据库重置后，新注册用户获取 ID 1，从而意外拥有原 ID 1 用户的孤儿数据（如工作空间）
     */
    private void initAdminUser() {
        try {
            UserPO admin = userMapper.selectById(1L);
            if (admin == null) {
                log.info("初始化默认管理员账号 (ID=1)...");
                // 使用 JDBC 强制插入指定 ID，绕过自增策略
                String sql = "INSERT INTO users (id, username, password, email, role, status, created_at, updated_at) " +
                           "VALUES (1, 'admin', 'admin123', 'admin@promptforge.com', 'ADMIN', 1, NOW(), NOW())";
                
                jdbcTemplate.update(sql);
                log.info("管理员账号初始化成功");
            }
        } catch (Exception e) {
            log.error("初始化管理员账号失败", e);
        }
    }
}
