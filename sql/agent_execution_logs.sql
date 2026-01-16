-- Agent 执行日志表
-- 用于监控 Skill 调用情况、耗时、状态以及 Token 消耗

CREATE TABLE IF NOT EXISTS agent_execution_logs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    
    -- 关联信息
    user_id BIGINT NOT NULL COMMENT '用户ID',
    session_id VARCHAR(64) NOT NULL COMMENT '会话ID',
    trace_id VARCHAR(64) COMMENT '链路追踪ID',
    
    -- 执行内容
    executor_name VARCHAR(64) NOT NULL COMMENT '执行器名称 (LLM 或 Skill Name)',
    action_type ENUM('LLM_CHAT', 'SKILL_EXECUTION') NOT NULL COMMENT '操作类型',
    input_summary TEXT COMMENT '输入摘要 (截取)',
    output_summary TEXT COMMENT '输出摘要 (截取)',
    
    -- 性能指标
    start_time DATETIME NOT NULL COMMENT '开始时间',
    end_time DATETIME NOT NULL COMMENT '结束时间',
    duration_ms BIGINT NOT NULL COMMENT '耗时 (毫秒)',
    
    -- 状态
    status ENUM('SUCCESS', 'FAILURE', 'TIMEOUT') NOT NULL COMMENT '执行状态',
    error_message TEXT COMMENT '错误信息',
    
    -- Token 统计 (仅 LLM_CHAT 类型有效)
    model VARCHAR(64) COMMENT '使用的模型',
    prompt_tokens INT DEFAULT 0 COMMENT '输入 Token',
    completion_tokens INT DEFAULT 0 COMMENT '输出 Token',
    total_tokens INT DEFAULT 0 COMMENT '总 Token',
    
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    
    INDEX idx_user_session (user_id, session_id),
    INDEX idx_executor (executor_name),
    INDEX idx_start_time (start_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Agent 执行监控日志';
