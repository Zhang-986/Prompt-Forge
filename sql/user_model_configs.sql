-- =====================================================
-- 用户模型配置表
-- =====================================================
CREATE TABLE IF NOT EXISTS user_model_configs (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '配置ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    provider VARCHAR(50) NOT NULL COMMENT '提供商: google/zhipu/deepseek/openai/claude',
    api_key VARCHAR(500) NOT NULL COMMENT 'API Key',
    base_url VARCHAR(255) COMMENT '自定义 Base URL (可选)',
    model_name VARCHAR(100) COMMENT '模型名称 (如 gpt-4/glm-4-flash)',
    enabled TINYINT NOT NULL DEFAULT 1 COMMENT '是否启用: 0-禁用, 1-启用',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_user_provider (user_id, provider),
    INDEX idx_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户模型配置表';
