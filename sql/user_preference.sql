-- 用户偏好画像表
-- 用于存储从多轮对话中收集的用户偏好信息

CREATE TABLE IF NOT EXISTS user_preference (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    preferred_tech_stack VARCHAR(200) COMMENT '偏好的技术栈',
    preferred_output_format VARCHAR(100) COMMENT '偏好的输出格式',
    preferred_provider VARCHAR(50) COMMENT '偏好的AI模型',
    common_domains TEXT COMMENT '常用领域（JSON数组）',
    topic_frequency TEXT COMMENT '话题频率统计（JSON对象）',
    total_sessions INT DEFAULT 0 COMMENT '总对话次数',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    
    UNIQUE KEY uk_user_id (user_id),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户偏好画像表';
