-- 禁用不需要 Function Calling 的 Skills
UPDATE agent_skills SET enabled = 0 WHERE name IN (
    'evaluation',
    'optimization',
    'structurization',
    'prompt-library',
    'version-analyzer'
);

-- 确保核心 Skills 启用
UPDATE agent_skills SET enabled = 1 WHERE name IN (
    'url-fetch',
    'web-search',
    'code-analyzer',
    'code-generator'
);
