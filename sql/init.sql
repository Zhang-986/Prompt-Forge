-- =====================================================
-- Prompt-Forge 数据库初始化脚本
-- Database: MySQL 8.0+
-- =====================================================

-- 创建数据库
CREATE DATABASE IF NOT EXISTS prompt_forge DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE prompt_forge;

-- =====================================================
-- 1. 用户表
-- =====================================================
DROP TABLE IF EXISTS users;
CREATE TABLE users (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '用户ID',
    username VARCHAR(50) NOT NULL COMMENT '用户名',
    password VARCHAR(255) NOT NULL COMMENT '密码(BCrypt加密)',
    email VARCHAR(100) COMMENT '邮箱',
    avatar VARCHAR(500) COMMENT '头像URL',
    role VARCHAR(20) NOT NULL DEFAULT 'MEMBER' COMMENT '角色: ADMIN/MEMBER/VIEWER',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态: 0-禁用, 1-启用',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_username (username),
    UNIQUE KEY uk_email (email)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';

-- =====================================================
-- 2. 工作空间表
-- =====================================================
DROP TABLE IF EXISTS workspaces;
CREATE TABLE workspaces (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '工作空间ID',
    name VARCHAR(100) NOT NULL COMMENT '工作空间名称',
    description VARCHAR(500) COMMENT '描述',
    owner_id BIGINT NOT NULL COMMENT '所有者ID',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_owner_id (owner_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='工作空间表';

-- =====================================================
-- 3. 工作空间成员表
-- =====================================================
DROP TABLE IF EXISTS workspace_members;
CREATE TABLE workspace_members (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT 'ID',
    workspace_id BIGINT NOT NULL COMMENT '工作空间ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    role VARCHAR(20) NOT NULL DEFAULT 'MEMBER' COMMENT '角色: ADMIN/MEMBER/VIEWER',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '加入时间',
    UNIQUE KEY uk_workspace_user (workspace_id, user_id),
    INDEX idx_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='工作空间成员表';

-- =====================================================
-- 4. Prompt 主表（聚合根）
-- =====================================================
DROP TABLE IF EXISTS prompts;
CREATE TABLE prompts (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT 'Prompt ID',
    name VARCHAR(200) NOT NULL COMMENT 'Prompt 名称',
    description TEXT COMMENT '描述',
    workspace_id BIGINT NOT NULL COMMENT '所属工作空间ID',
    latest_version_id BIGINT COMMENT '最新版本ID (HEAD指针)',
    creator_id BIGINT NOT NULL COMMENT '创建者ID',
    is_public TINYINT NOT NULL DEFAULT 0 COMMENT '是否公开: 0-私有, 1-公开',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态: 0-已删除, 1-正常',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_workspace_id (workspace_id),
    INDEX idx_creator_id (creator_id),
    INDEX idx_latest_version_id (latest_version_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Prompt主表';

-- =====================================================
-- 5. Prompt 版本表（链式结构，不可变）
-- =====================================================
DROP TABLE IF EXISTS prompt_versions;
CREATE TABLE prompt_versions (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '版本ID',
    prompt_id BIGINT NOT NULL COMMENT '所属Prompt ID',
    version_number INT NOT NULL COMMENT '版本号',
    content TEXT NOT NULL COMMENT 'Prompt 内容',
    variables JSON COMMENT '变量定义 {"topic": "string", "style": "enum:formal,casual"}',
    parent_id BIGINT COMMENT '父版本ID (形成链式结构)',
    commit_message VARCHAR(500) COMMENT '提交说明',
    author_id BIGINT NOT NULL COMMENT '作者ID',
    content_hash VARCHAR(64) COMMENT '内容哈希 (用于Diff检查)',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_prompt_id (prompt_id),
    INDEX idx_parent_id (parent_id),
    INDEX idx_author_id (author_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Prompt版本表(不可变)';

-- =====================================================
-- 6. 竞技场会话表
-- =====================================================
DROP TABLE IF EXISTS arena_sessions;
CREATE TABLE arena_sessions (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '会话ID',
    prompt_version_id BIGINT NOT NULL COMMENT '使用的Prompt版本ID',
    final_prompt TEXT NOT NULL COMMENT '渲染后的最终Prompt',
    variables JSON COMMENT '本次使用的变量值',
    models JSON NOT NULL COMMENT '参与的模型列表 ["gpt-4", "deepseek"]',
    status VARCHAR(20) NOT NULL DEFAULT 'RUNNING' COMMENT '状态: RUNNING/COMPLETED/FAILED',
    creator_id BIGINT NOT NULL COMMENT '创建者ID',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    completed_at DATETIME COMMENT '完成时间',
    INDEX idx_prompt_version_id (prompt_version_id),
    INDEX idx_creator_id (creator_id),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='竞技场会话表';

-- =====================================================
-- 7. 竞技场结果表
-- =====================================================
DROP TABLE IF EXISTS arena_results;
CREATE TABLE arena_results (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '结果ID',
    session_id BIGINT NOT NULL COMMENT '会话ID',
    model_id VARCHAR(50) NOT NULL COMMENT '模型标识 (gpt-4, deepseek, claude)',
    content TEXT COMMENT '生成的内容',
    tokens_used INT COMMENT '消耗的Token数',
    latency_ms INT COMMENT '响应延迟(毫秒)',
    status VARCHAR(20) NOT NULL DEFAULT 'SUCCESS' COMMENT '状态: SUCCESS/FAILED/TIMEOUT',
    error_message TEXT COMMENT '错误信息',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_session_id (session_id),
    INDEX idx_model_id (model_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='竞技场结果表';

-- =====================================================
-- 8. 异步任务表
-- =====================================================
DROP TABLE IF EXISTS async_tasks;
CREATE TABLE async_tasks (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '任务ID',
    task_type VARCHAR(50) NOT NULL COMMENT '任务类型: BATCH_ARENA/EXPORT_REPORT',
    payload JSON COMMENT '任务参数',
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT '状态: PENDING/RUNNING/SUCCESS/FAILED/CANCELLED',
    progress INT DEFAULT 0 COMMENT '进度百分比 0-100',
    result JSON COMMENT '任务结果',
    error_message TEXT COMMENT '错误信息',
    retry_count INT DEFAULT 0 COMMENT '重试次数',
    creator_id BIGINT NOT NULL COMMENT '创建者ID',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    started_at DATETIME COMMENT '开始执行时间',
    finished_at DATETIME COMMENT '完成时间',
    INDEX idx_status (status),
    INDEX idx_task_type (task_type),
    INDEX idx_creator_id (creator_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='异步任务表';

-- =====================================================
-- 初始数据
-- =====================================================

-- 插入管理员用户 (密码: admin123, BCrypt加密)
INSERT INTO users (username, password, email, role, status) VALUES
('admin', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', 'admin@promptforge.com', 'ADMIN', 1);

-- 插入默认工作空间
INSERT INTO workspaces (name, description, owner_id) VALUES
('默认工作空间', '系统默认工作空间', 1);

-- 将管理员加入默认工作空间
INSERT INTO workspace_members (workspace_id, user_id, role) VALUES
(1, 1, 'ADMIN');

-- 插入示例 Prompt
INSERT INTO prompts (name, description, workspace_id, creator_id, is_public) VALUES
('通用问答模板', '一个通用的问答Prompt模板，支持主题和风格变量', 1, 1, 1);

-- 插入示例版本
INSERT INTO prompt_versions (prompt_id, version_number, content, variables, parent_id, commit_message, author_id, content_hash) VALUES
(1, 1, '请用{{style}}的风格，详细介绍一下{{topic}}这个主题。要求：\n1. 内容准确、有深度\n2. 语言{{style}}\n3. 适合{{audience}}阅读', 
'{"topic": {"type": "string", "description": "要介绍的主题"}, "style": {"type": "enum", "options": ["专业", "通俗", "幽默"], "description": "语言风格"}, "audience": {"type": "string", "description": "目标读者"}}',
NULL, '初始版本', 1, 'abc123');

-- 更新 Prompt 的 HEAD 指针
UPDATE prompts SET latest_version_id = 1 WHERE id = 1;
