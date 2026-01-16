-- Agent Skills 表
-- 基于 Claude Agent Skills 架构设计，支持三层渐进式加载

CREATE TABLE IF NOT EXISTS agent_skills (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    
    -- Level 1: Metadata (启动时加载到内存)
    name VARCHAR(64) NOT NULL UNIQUE COMMENT '技能唯一标识，如 prompt-library',
    display_name VARCHAR(128) COMMENT '显示名称，如 Prompt 库搜索',
    description VARCHAR(1024) NOT NULL COMMENT '技能描述（给 LLM 理解用）',
    trigger_keywords JSON COMMENT '触发关键词 ["参考", "模板", "搜索"]',
    category ENUM('CORE', 'DATA', 'CODE', 'CONTENT') DEFAULT 'DATA' COMMENT '技能分类',
    
    -- Level 2: Instructions (匹配时按需加载)
    instructions TEXT COMMENT '详细使用指令（Markdown 格式）',
    
    -- Level 3: Executor (执行时调用)
    executor_bean VARCHAR(128) NOT NULL COMMENT 'Spring Bean 名称',
    parameter_schema JSON COMMENT '参数 JSON Schema（Function Calling 用）',
    
    -- 管理字段
    enabled TINYINT(1) DEFAULT 1,
    sort_order INT DEFAULT 0,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    INDEX idx_category (category),
    INDEX idx_enabled (enabled)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Agent Skills 配置表';

-- 初始化 6 个核心 Skill
INSERT INTO agent_skills (name, display_name, description, trigger_keywords, category, executor_bean, parameter_schema, instructions, sort_order) VALUES

-- CORE 技能：始终加载
('evaluation', 'Prompt 质量评估', 
 '评估 Prompt 的质量，检查清晰度、完整性、可执行性。当需要检验 Prompt 质量时使用。', 
 '["评估", "检查", "质量", "打分"]', 
 'CORE', 'evaluationExecutor',
 '{"type":"object","properties":{"prompt":{"type":"string","description":"要评估的 Prompt 内容"}},"required":["prompt"]}',
 '# Prompt 评估指南\n\n## 评估维度\n1. 清晰度：指令是否明确\n2. 完整性：上下文是否充分\n3. 可执行性：AI 能否理解并执行\n\n## 输出格式\n返回评分（1-10）和改进建议。',
 1),

-- DATA 技能
('prompt-library', 'Prompt 库搜索', 
 '搜索用户的 Prompt 库，找到相似或相关的 Prompt 模板。当用户提到"参考"、"模板"、"类似的"、"之前写过"时使用。', 
 '["参考", "模板", "类似", "之前", "搜索prompt", "找一下"]', 
 'DATA', 'promptLibraryExecutor',
 '{"type":"object","properties":{"query":{"type":"string","description":"搜索关键词"}},"required":["query"]}',
 '# Prompt 库搜索技能\n\n## 搜索策略\n1. 优先匹配标题\n2. 其次匹配描述\n3. 最后匹配内容\n\n## 返回格式\n列出前5个匹配结果，包含标题、描述、最新版本预览。',
 2),

('web-search', '网络搜索', 
 '在互联网上搜索最新信息。当用户需要查找实时信息、新闻、技术文档时使用。', 
 '["搜一下", "搜索", "查一下", "最新", "新闻", "文档", "资料"]', 
 'DATA', 'webSearchExecutor',
 '{"type":"object","properties":{"query":{"type":"string","description":"搜索关键词"}},"required":["query"]}',
 '# 网络搜索技能\n\n## 使用场景\n- 查找最新技术动态\n- 获取实时信息\n- 搜索文档和教程\n\n## 注意事项\n返回结果需要标注来源 URL。',
 3),

-- CODE 技能
('code-analyzer', '代码仓库分析', 
 '分析 GitHub 代码仓库，提取项目结构、技术栈、核心逻辑。当用户提供 GitHub 链接或讨论代码项目时使用。', 
 '["github", "代码", "仓库", "项目", "分析代码", "技术栈"]', 
 'CODE', 'codeAnalyzerExecutor',
 '{"type":"object","properties":{"repoUrl":{"type":"string","description":"GitHub 仓库 URL"}},"required":["repoUrl"]}',
 '# 代码仓库分析技能\n\n## 分析内容\n1. 项目结构\n2. 主要技术栈\n3. 核心业务逻辑\n4. 代码风格\n\n## 输出\n结构化的项目分析报告。',
 4),

('code-generator', 'Python 代码生成', 
 '生成 Python 代码片段。当用户需要编写脚本、数据处理、自动化任务时使用。', 
 '["写代码", "python", "脚本", "生成代码", "编程"]', 
 'CODE', 'codeGeneratorExecutor',
 '{"type":"object","properties":{"requirement":{"type":"string","description":"代码需求描述"}},"required":["requirement"]}',
 '# 代码生成技能\n\n## 代码规范\n1. 添加必要注释\n2. 处理异常情况\n3. 遵循 PEP8\n\n## 输出格式\n返回可直接运行的代码块。',
 5),

-- CONTENT 技能
('version-analyzer', '版本分析', 
 '分析 Prompt 的版本变更历史，对比不同版本的差异。当用户提到"上一版"、"改了什么"、"回滚"时使用。', 
 '["版本", "上一版", "改动", "diff", "变更", "回滚", "历史"]', 
 'CONTENT', 'versionAnalyzerExecutor',
 '{"type":"object","properties":{"promptId":{"type":"integer","description":"Prompt ID"},"versionId":{"type":"integer","description":"版本 ID（可选）"}}}',
 '# 版本分析技能\n\n## 分析内容\n1. 版本间的文本差异\n2. 变更的意图猜测\n3. 是否推荐回滚\n\n## 输出\n使用 diff 格式展示变更。',
 6),

-- CORE 技能：结构化和优化
('structurization', 'Prompt 结构化', 
 '将原始 Prompt 内容格式化为标准结构（角色、约束、工作流、输出格式）。当用户的 Prompt 缺乏结构或需要整理时使用。', 
 '["结构化", "整理", "格式化", "标准化", "模板化"]', 
 'CORE', 'structurizationExecutor',
 '{"type":"object","properties":{"rawContent":{"type":"string","description":"原始 Prompt 内容"}},"required":["rawContent"]}',
 '# 结构化技能\n\n## 标准结构\n1. 角色定义 (Role)\n2. 任务目标 (Goal)\n3. 约束条件 (Constraints)\n4. 工作流程 (Workflow)\n5. 输出格式 (Output Format)\n\n## 适用场景\n用户的原始想法比较零散时使用。',
 7),

('optimization', 'Prompt 优化', 
 '应用高级技巧优化 Prompt 质量。支持 Chain-of-Thought (cot)、Few-Shot 示例 (few-shot)、清晰度优化 (clarity)。', 
 '["优化", "改进", "提升", "cot", "思维链", "few-shot", "示例"]', 
 'CORE', 'optimizationExecutor',
 '{"type":"object","properties":{"originalPrompt":{"type":"string","description":"原始 Prompt"},"technique":{"type":"string","description":"优化技巧 (cot/few-shot/clarity)"}},"required":["originalPrompt"]}',
 '# 优化技能\n\n## 支持的技巧\n1. **cot** - Chain-of-Thought 思维链\n2. **few-shot** - Few-Shot 示例引导\n3. **clarity** - 清晰度提升\n\n## 使用方式\n根据 Prompt 类型选择合适的优化技巧。',
 8);

