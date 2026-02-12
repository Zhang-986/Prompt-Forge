package config

import (
	"fmt"
	"os"

	"gopkg.in/yaml.v3"
)

// Config 应用配置
type Config struct {
	Server   ServerConfig   `yaml:"server"`
	Database DatabaseConfig `yaml:"database"`
	JWT      JWTConfig      `yaml:"jwt"`
}

// ServerConfig 服务器配置
type ServerConfig struct {
	Port     int `yaml:"port"`
	GRPCPort int `yaml:"grpc_port"`
}

// DatabaseConfig 数据库配置
type DatabaseConfig struct {
	Host     string `yaml:"host"`
	Port     int    `yaml:"port"`
	User     string `yaml:"user"`
	Password string `yaml:"password"`
	DBName   string `yaml:"dbname"`
}

// JWTConfig JWT 配置
type JWTConfig struct {
	Secret string `yaml:"secret"`
}

// DSN 生成 MySQL 连接字符串
func (d *DatabaseConfig) DSN() string {
	return fmt.Sprintf("%s:%s@tcp(%s:%d)/%s?charset=utf8mb4&parseTime=True&loc=Asia%%2FShanghai",
		d.User, d.Password, d.Host, d.Port, d.DBName)
}

// Load 加载配置文件
func Load(path string) (*Config, error) {
	data, err := os.ReadFile(path)
	if err != nil {
		return nil, fmt.Errorf("读取配置文件失败: %w", err)
	}

	cfg := &Config{
		// 默认值
		Server: ServerConfig{Port: 8081, GRPCPort: 9090},
		Database: DatabaseConfig{
			Host:     "10.147.17.199",
			Port:     3306,
			User:     "root",
			Password: "1234",
			DBName:   "prompt_forge",
		},
		JWT: JWTConfig{
			Secret: "PromptForge2024SecretKeyForJWTTokenGenerationMustBeLongEnough",
		},
	}

	if err := yaml.Unmarshal(data, cfg); err != nil {
		return nil, fmt.Errorf("解析配置文件失败: %w", err)
	}

	return cfg, nil
}

// LoadDefault 使用默认配置（当配置文件不存在时）
func LoadDefault() *Config {
	return &Config{
		Server: ServerConfig{Port: 8081, GRPCPort: 9090},
		Database: DatabaseConfig{
			Host:     "10.147.17.199",
			Port:     3306,
			User:     "root",
			Password: "1234",
			DBName:   "prompt_forge",
		},
		JWT: JWTConfig{
			Secret: "PromptForge2024SecretKeyForJWTTokenGenerationMustBeLongEnough",
		},
	}
}
