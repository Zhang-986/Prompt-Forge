package handler

import (
	"context"
	"database/sql"
	"encoding/json"
	"fmt"
	"log"
	"strings"
	"sync"
	"time"

	"github.com/gin-gonic/gin"
	"go-ai-gateway/factory"
	"go-ai-gateway/model"
	"go-ai-gateway/repository"
)

// ArenaRequest 竞技场请求 (匹配前端 Payload)
type ArenaRequest struct {
	PromptVersionID int64             `json:"promptVersionId"`
	Variables       map[string]string `json:"variables"`
	ModelIDs        []string          `json:"modelIds"` // ["openai:gpt-4", "google:gemini-1.5"]
}

// ArenaHandler 处理竞技场请求 (SSE)
func ArenaHandler(f *factory.DynamicLlmClientFactory, repo *repository.ConfigRepo) gin.HandlerFunc {
	return func(c *gin.Context) {
		var req ArenaRequest
		if err := c.ShouldBindJSON(&req); err != nil {
			c.JSON(400, gin.H{"error": "无效的请求参数: " + err.Error()})
			return
		}

		userID := c.GetInt64("userId") // 从 Auth 中间件获取

		// 1. 获取 Prompt 版本
		pv, err := repo.GetPromptVersionByID(req.PromptVersionID)
		if err != nil {
			c.JSON(404, gin.H{"error": "Prompt 版本不存在"})
			return
		}

		// 2. 渲染 Prompt
		finalPrompt := renderPrompt(pv.Content, req.Variables)

		// 3. 创建会话记录
		varsJson, _ := json.Marshal(req.Variables)
		modelsJson, _ := json.Marshal(req.ModelIDs)

		session := &model.ArenaSession{
			PromptVersionID: req.PromptVersionID,
			FinalPrompt:     finalPrompt,
			Variables:       sql.NullString{String: string(varsJson), Valid: true},
			Models:          string(modelsJson),
			Status:          "RUNNING",
			CreatorID:       userID,
			CreatedAt:       time.Now(),
		}

		if err := repo.CreateArenaSession(session); err != nil {
			log.Printf("创建会话失败: %v", err)
			c.JSON(500, gin.H{"error": "创建会话失败"})
			return
		}

		// 设置 SSE Header
		c.Writer.Header().Set("Content-Type", "text/event-stream")
		c.Writer.Header().Set("Cache-Control", "no-cache")
		c.Writer.Header().Set("Connection", "keep-alive")
		c.Writer.Flush()

		// SSE 事件通道 (用于并发写入安全)
		eventChan := make(chan interface{}, 100)
		doneChan := make(chan struct{})

		// 启动写协程
		go func() {
			defer func() {
				// 防止 channel 关闭后 panic
				recover()
				close(doneChan)
			}()

			for data := range eventChan {
				jsonBytes, _ := json.Marshal(data)
				fmt.Fprintf(c.Writer, "data: %s\n\n", jsonBytes)
				c.Writer.Flush()
			}
		}()

		// 发送 Session ID
		eventChan <- map[string]interface{}{
			"type":      "session",
			"sessionId": session.ID,
		}

		var wg sync.WaitGroup

		// 4. 并行调用所有模型
		for _, modelID := range req.ModelIDs {
			wg.Add(1)
			go func(mid string) {
				defer wg.Done()
				processModel(c.Request.Context(), f, repo, session.ID, mid, finalPrompt, userID, eventChan)
			}(modelID)
		}

		wg.Wait()

		// 5. 更新会话状态
		repo.UpdateArenaSessionStatus(session.ID, "COMPLETED")

		// 发送完成事件
		eventChan <- map[string]interface{}{
			"type": "complete",
			"msg":  "All models finished",
		}

		close(eventChan)

		// 等待写协程结束
		<-doneChan
	}
}

// processModel 处理单个模型调用
func processModel(ctx context.Context, f *factory.DynamicLlmClientFactory, repo *repository.ConfigRepo,
	sessionID int64, modelID string, prompt string, userID int64, eventChan chan<- interface{}) {

	startTime := time.Now()

	// 解析 provider:modelName
	parts := strings.SplitN(modelID, ":", 2)
	provider := parts[0]
	specificModel := ""
	if len(parts) > 1 {
		specificModel = parts[1]
	}

	// 获取用户配置
	cfg, err := repo.GetConfigByUserAndProvider(userID, provider)

	// 如果用户未配置
	if err != nil {
		handleModelError(eventChan, repo, sessionID, modelID, "用户未配置该模型的 API Key", startTime)
		return
	}

	// 如果指定了具体模型，临时覆盖配置
	// 注意: GetConfigByUserAndProvider 返回的是指针
	effectiveConfig := *cfg // 浅拷贝
	if specificModel != "" {
		effectiveConfig.ModelName = sql.NullString{String: specificModel, Valid: true}
	}

	// 调用 AI
	contentCh, errCh := f.GenerateStream(ctx, &effectiveConfig, prompt)

	fullContent := ""
	sequence := 0

	// 发送 start 事件
	eventChan <- map[string]interface{}{
		"type":    "start",
		"modelId": modelID,
	}

	for content := range contentCh {
		sequence++
		fullContent += content

		// 发送 content 事件
		eventChan <- map[string]interface{}{
			"type":     "content",
			"modelId":  modelID,
			"content":  content,
			"sequence": sequence,
		}
	}

	// 检查是否有错误
	select {
	case err := <-errCh:
		if err != nil {
			handleModelError(eventChan, repo, sessionID, modelID, err.Error(), startTime)
			return
		}
	default:
	}

	// 记录成功结果
	latency := int(time.Since(startTime).Milliseconds())
	tokens := len(fullContent) / 2 // 简单估算

	result := &model.ArenaResult{
		SessionID:  sessionID,
		ModelID:    modelID,
		Content:    sql.NullString{String: fullContent, Valid: true},
		TokensUsed: tokens,
		LatencyMs:  latency,
		Status:     "SUCCESS",
		CreatedAt:  time.Now(),
	}
	repo.CreateArenaResult(result)

	// 发送 finish 事件
	eventChan <- map[string]interface{}{
		"type":     "finish",
		"modelId":  modelID,
		"finished": true,
	}
}

func handleModelError(eventChan chan<- interface{}, repo *repository.ConfigRepo, sessionID int64, modelID string, errMsg string, startTime time.Time) {
	latency := int(time.Since(startTime).Milliseconds())

	// 记录失败结果
	result := &model.ArenaResult{
		SessionID:    sessionID,
		ModelID:      modelID,
		Status:       "FAILED",
		ErrorMessage: sql.NullString{String: errMsg, Valid: true},
		LatencyMs:    latency,
		CreatedAt:    time.Now(),
	}
	repo.CreateArenaResult(result)

	// 发送 error 事件
	eventChan <- map[string]interface{}{
		"type":     "error",
		"modelId":  modelID,
		"content":  errMsg,
		"finished": true,
	}
}

// renderPrompt 简单的模板变量替换
func renderPrompt(tmpl string, variables map[string]string) string {
	result := tmpl
	for key, val := range variables {
		// 替换 {{key}} 和 {{ key }}
		result = strings.ReplaceAll(result, fmt.Sprintf("{{%s}}", key), val)
		result = strings.ReplaceAll(result, fmt.Sprintf("{{ %s }}", key), val)
	}
	return result
}
