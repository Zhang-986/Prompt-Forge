-- 登录审计日志表
-- 用于记录所有登录尝试，包括成功和失败

CREATE TABLE IF NOT EXISTS login_audit_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '日志ID',
    username VARCHAR(100) NOT NULL COMMENT '用户名',
    ip_address VARCHAR(45) NOT NULL COMMENT '客户端IP地址',
    geo_location VARCHAR(200) DEFAULT NULL COMMENT '地理位置（由IP解析）',
    device_fingerprint VARCHAR(200) DEFAULT NULL COMMENT '设备指纹',
    user_agent VARCHAR(500) DEFAULT NULL COMMENT 'User-Agent',
    result VARCHAR(50) NOT NULL COMMENT '登录结果: SUCCESS/FAILED_PASSWORD/BANNED/CAPTCHA_FAILED',
    failure_reason VARCHAR(200) DEFAULT NULL COMMENT '失败原因',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    
    INDEX idx_username (username),
    INDEX idx_ip_address (ip_address),
    INDEX idx_created_at (created_at),
    INDEX idx_result (result)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='登录审计日志表';
