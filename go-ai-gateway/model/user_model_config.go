package model

import "database/sql"

// UserModelConfig 用户模型配置实体
// 对应 MySQL 表 user_model_configs
type UserModelConfig struct {
	ID              int64          `json:"id"`
	UserID          int64          `json:"user_id"`
	Provider        string         `json:"provider"`
	APIKey          string         `json:"api_key"`
	BaseURL         sql.NullString `json:"-"`
	ModelName       sql.NullString `json:"-"`
	AvailableModels sql.NullString `json:"-"`
	Enabled         int            `json:"enabled"`
}

// GetEffectiveBaseURL 获取生效的 Base URL
// 如果用户配置了自定义 URL 则使用自定义的，否则返回厂商默认值
func (c *UserModelConfig) GetEffectiveBaseURL() string {
	if c.BaseURL.Valid && c.BaseURL.String != "" {
		return c.BaseURL.String
	}
	switch c.Provider {
	case "google":
		return "https://generativelanguage.googleapis.com"
	case "zhipu":
		return "https://open.bigmodel.cn/api/paas/v4"
	case "deepseek":
		return "https://api.deepseek.com"
	case "openai":
		return "https://api.openai.com"
	case "claude", "anthropic":
		return "https://api.anthropic.com"
	default:
		return ""
	}
}

// GetEffectiveModelName 获取生效的模型名称
func (c *UserModelConfig) GetEffectiveModelName() string {
	if c.ModelName.Valid && c.ModelName.String != "" {
		return c.ModelName.String
	}
	switch c.Provider {
	case "google":
		return "gemini-2.0-flash"
	case "zhipu":
		return "glm-4-flash"
	case "deepseek":
		return "deepseek-chat"
	case "openai":
		return "gpt-4"
	case "claude", "anthropic":
		return "claude-3-opus-20240229"
	default:
		return ""
	}
}

// GetBaseURLString 获取 BaseURL 字符串
func (c *UserModelConfig) GetBaseURLString() string {
	if c.BaseURL.Valid {
		return c.BaseURL.String
	}
	return ""
}

// GetModelNameString 获取 ModelName 字符串
func (c *UserModelConfig) GetModelNameString() string {
	if c.ModelName.Valid {
		return c.ModelName.String
	}
	return ""
}
