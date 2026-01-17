-- 插入 url-fetch Skill
INSERT INTO agent_skills (name, display_name, description, trigger_keywords, category, instructions, executor_bean, parameter_schema, enabled, sort_order)
VALUES (
    'url-fetch',
    '读取网页',
    '读取指定 URL 的网页内容并提取正文。当用户提供具体的 URL 链接并希望获取其内容时使用。',
    '["读取", "网页", "链接", "url", "网站", "打开"]',
    'DATA',
    '# 网页读取技能\n\n## 使用场景\n- 用户提供了具体的 URL，需要读取内容\n- 需要获取网页的文本信息进行分析\n\n## 参数\n- url: 要读取的网页 URL\n\n## 注意事项\n- 仅支持公开可访问的网页\n- 返回内容会自动提取正文，去除脚本和样式',
    'urlFetchExecutor',
    '{"type": "object", "required": ["url"], "properties": {"url": {"type": "string", "description": "要读取的网页 URL"}}}',
    1,
    2
);

-- 可选：暂时禁用不需要 Function Calling 的 Skills
-- UPDATE agent_skills SET enabled = 0 WHERE name IN ('evaluation', 'structurization', 'optimization');
