package repository

import (
	"fmt"
	"go-ai-gateway/model"
	"time"
)

// GetPromptVersionByID 获取 Prompt 版本
func (r *ConfigRepo) GetPromptVersionByID(id int64) (*model.PromptVersion, error) {
	query := `SELECT id, prompt_id, version_number, content, variables, parent_id, created_at 
	          FROM prompt_versions WHERE id = ?`

	var pv model.PromptVersion
	err := r.db.QueryRow(query, id).Scan(
		&pv.ID, &pv.PromptID, &pv.VersionNumber, &pv.Content,
		&pv.Variables, &pv.ParentID, &pv.CreatedAt,
	)
	if err != nil {
		return nil, err
	}
	return &pv, nil
}

// CreateArenaSession 创建竞技场会话
func (r *ConfigRepo) CreateArenaSession(session *model.ArenaSession) error {
	query := `INSERT INTO arena_sessions 
	          (prompt_version_id, final_prompt, variables, models, status, creator_id, created_at) 
	          VALUES (?, ?, ?, ?, ?, ?, ?)`

	res, err := r.db.Exec(query,
		session.PromptVersionID,
		session.FinalPrompt,
		session.Variables,
		session.Models,
		session.Status,
		session.CreatorID,
		session.CreatedAt,
	)
	if err != nil {
		return fmt.Errorf("create session failed: %v", err)
	}

	id, err := res.LastInsertId()
	if err != nil {
		return err
	}
	session.ID = id
	return nil
}

// UpdateArenaSessionStatus 更新会话状态
func (r *ConfigRepo) UpdateArenaSessionStatus(id int64, status string) error {
	query := `UPDATE arena_sessions SET status = ?, completed_at = ? WHERE id = ?`

	var completedAt interface{}
	if status == "COMPLETED" || status == "FAILED" {
		completedAt = time.Now()
	} else {
		completedAt = nil
	}

	_, err := r.db.Exec(query, status, completedAt, id)
	return err
}

// CreateArenaResult 保存竞技结果
func (r *ConfigRepo) CreateArenaResult(result *model.ArenaResult) error {
	query := `INSERT INTO arena_results 
	          (session_id, model_id, content, tokens_used, latency_ms, status, error_message, created_at) 
	          VALUES (?, ?, ?, ?, ?, ?, ?, ?)`

	_, err := r.db.Exec(query,
		result.SessionID,
		result.ModelID,
		result.Content,
		result.TokensUsed,
		result.LatencyMs,
		result.Status,
		result.ErrorMessage,
		result.CreatedAt,
	)
	return err
}
