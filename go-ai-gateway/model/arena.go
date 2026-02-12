package model

import (
	"database/sql"
	"time"
)

// ArenaSession 竞技场会话
type ArenaSession struct {
	ID              int64          `json:"id"`
	PromptVersionID int64          `json:"prompt_version_id"`
	FinalPrompt     string         `json:"final_prompt"`
	Variables       sql.NullString `json:"variables"` // JSON string
	Models          string         `json:"models"`    // JSON string array
	Status          string         `json:"status"`    // RUNNING, COMPLETED, FAILED
	CreatorID       int64          `json:"creator_id"`
	CreatedAt       time.Time      `json:"created_at"`
	CompletedAt     sql.NullTime   `json:"completed_at"`
}

// ArenaResult 竞技场结果
type ArenaResult struct {
	ID           int64          `json:"id"`
	SessionID    int64          `json:"session_id"`
	ModelID      string         `json:"model_id"`
	Content      sql.NullString `json:"content"`
	TokensUsed   int            `json:"tokens_used"`
	LatencyMs    int            `json:"latency_ms"`
	Status       string         `json:"status"` // SUCCESS, FAILED, TIMEOUT
	ErrorMessage sql.NullString `json:"error_message"`
	CreatedAt    time.Time      `json:"created_at"`
}

// PromptVersion Prompt 版本
type PromptVersion struct {
	ID            int64          `json:"id"`
	PromptID      int64          `json:"prompt_id"`
	VersionNumber int            `json:"version_number"`
	Content       string         `json:"content"`
	Variables     sql.NullString `json:"variables"` // JSON schema
	ParentID      sql.NullInt64  `json:"parent_id"`
	CreatedAt     time.Time      `json:"created_at"`
}
