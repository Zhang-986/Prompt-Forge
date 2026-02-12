package repository

import (
	"database/sql"
	"fmt"

	_ "github.com/go-sql-driver/mysql"
	"go-ai-gateway/config"
	"go-ai-gateway/model"
)

// ConfigRepo 用户模型配置数据仓库
type ConfigRepo struct {
	db *sql.DB
}

// NewConfigRepo 创建数据仓库并初始化数据库连接
func NewConfigRepo(cfg *config.Config) (*ConfigRepo, error) {
	db, err := sql.Open("mysql", cfg.Database.DSN())
	if err != nil {
		return nil, fmt.Errorf("连接数据库失败: %w", err)
	}

	// 连接池配置
	db.SetMaxOpenConns(20)
	db.SetMaxIdleConns(5)

	if err := db.Ping(); err != nil {
		return nil, fmt.Errorf("数据库 Ping 失败: %w", err)
	}

	return &ConfigRepo{db: db}, nil
}

// GetEnabledConfigsByUserID 获取用户已启用的模型配置列表
func (r *ConfigRepo) GetEnabledConfigsByUserID(userID int64) ([]model.UserModelConfig, error) {
	query := `SELECT id, user_id, provider, api_key, base_url, model_name, available_models, enabled 
			  FROM user_model_configs 
			  WHERE user_id = ? AND enabled = 1`

	rows, err := r.db.Query(query, userID)
	if err != nil {
		return nil, fmt.Errorf("查询用户配置失败: %w", err)
	}
	defer rows.Close()

	var configs []model.UserModelConfig
	for rows.Next() {
		var c model.UserModelConfig
		err := rows.Scan(&c.ID, &c.UserID, &c.Provider, &c.APIKey,
			&c.BaseURL, &c.ModelName, &c.AvailableModels, &c.Enabled)
		if err != nil {
			return nil, fmt.Errorf("扫描行数据失败: %w", err)
		}
		configs = append(configs, c)
	}

	return configs, rows.Err()
}

// GetConfigByID 根据配置 ID 获取单条配置
func (r *ConfigRepo) GetConfigByID(configID int64) (*model.UserModelConfig, error) {
	query := `SELECT id, user_id, provider, api_key, base_url, model_name, available_models, enabled 
			  FROM user_model_configs 
			  WHERE id = ?`

	var c model.UserModelConfig
	err := r.db.QueryRow(query, configID).Scan(
		&c.ID, &c.UserID, &c.Provider, &c.APIKey,
		&c.BaseURL, &c.ModelName, &c.AvailableModels, &c.Enabled)
	if err != nil {
		if err == sql.ErrNoRows {
			return nil, fmt.Errorf("配置不存在: id=%d", configID)
		}
		return nil, fmt.Errorf("查询配置失败: %w", err)
	}

	return &c, nil
}

// GetConfigByUserAndProvider 根据用户 ID 和提供商获取配置
func (r *ConfigRepo) GetConfigByUserAndProvider(userID int64, provider string) (*model.UserModelConfig, error) {
	query := `SELECT id, user_id, provider, api_key, base_url, model_name, available_models, enabled 
			  FROM user_model_configs 
			  WHERE user_id = ? AND provider = ? AND enabled = 1`

	var c model.UserModelConfig
	err := r.db.QueryRow(query, userID, provider).Scan(
		&c.ID, &c.UserID, &c.Provider, &c.APIKey,
		&c.BaseURL, &c.ModelName, &c.AvailableModels, &c.Enabled)
	if err != nil {
		if err == sql.ErrNoRows {
			return nil, fmt.Errorf("用户 %d 未配置提供商 %s", userID, provider)
		}
		return nil, fmt.Errorf("查询配置失败: %w", err)
	}

	return &c, nil
}

// Close 关闭数据库连接
func (r *ConfigRepo) Close() error {
	return r.db.Close()
}
