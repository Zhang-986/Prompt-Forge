-- ============================================
-- 标签表 (prompt_tag)
-- ============================================
CREATE TABLE IF NOT EXISTS prompt_tag (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '标签ID',
    name VARCHAR(50) NOT NULL COMMENT '标签名称',
    color VARCHAR(20) DEFAULT '#5e6ad2' COMMENT '标签颜色',
    creator_id BIGINT COMMENT '创建者ID',
    workspace_id BIGINT COMMENT '工作空间ID',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    UNIQUE KEY uk_name_workspace (name, workspace_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Prompt标签表';

-- ============================================
-- 标签关联表 (prompt_tag_relation)
-- ============================================
CREATE TABLE IF NOT EXISTS prompt_tag_relation (
    prompt_id BIGINT NOT NULL COMMENT 'Prompt ID',
    tag_id BIGINT NOT NULL COMMENT '标签ID',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '关联时间',
    PRIMARY KEY (prompt_id, tag_id),
    KEY idx_tag_id (tag_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Prompt与标签关联表';
