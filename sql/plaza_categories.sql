-- 广场分类表
-- 用于动态管理广场的分类，替代前端硬编码的分类列表

CREATE TABLE IF NOT EXISTS plaza_categories (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    value VARCHAR(50) NOT NULL UNIQUE COMMENT '分类值，如 WRITING, CODING',
    label VARCHAR(50) NOT NULL COMMENT '显示名称，如 文案写作',
    icon VARCHAR(20) DEFAULT '📦' COMMENT 'Emoji 图标',
    sort_order INT DEFAULT 0 COMMENT '排序顺序，越小越靠前',
    is_active TINYINT(1) DEFAULT 1 COMMENT '是否启用',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) COMMENT '广场分类表' CHARSET=utf8mb4;

-- 初始化默认分类数据
INSERT INTO plaza_categories (value, label, icon, sort_order) VALUES
('WRITING', '文案写作', '✍️', 1),
('CODING', '代码助手', '💻', 2),
('ANALYSIS', '数据分析', '📊', 3),
('ROLEPLAY', '角色扮演', '🎭', 4),
('EDUCATION', '教育辅导', '📚', 5),
('TRANSLATION', '翻译润色', '🌍', 6),
('OTHER', '其他', '📦', 99);
