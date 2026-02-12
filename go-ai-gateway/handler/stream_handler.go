package handler

import (
	"encoding/json"
	"fmt"
	"log"
	"net/http"

	"github.com/gin-gonic/gin"
	"go-ai-gateway/factory"
	"go-ai-gateway/repository"
)

// StreamRequest 流式生成请求
type StreamRequest struct {
	Provider string `json:"provider" binding:"required"`
	Prompt   string `json:"prompt" binding:"required"`
	ConfigID int64  `json:"config_id"` // 可选，指定配置 ID
}

// StreamHandler SSE 流式生成端点
//
// POST /api/ai/stream
// Content-Type: application/json → text/event-stream
//
// 前端直连此端点获取 AI 流式响应
func StreamHandler(f *factory.DynamicLlmClientFactory, repo *repository.ConfigRepo) gin.HandlerFunc {
	return func(c *gin.Context) {
		var req StreamRequest
		if err := c.ShouldBindJSON(&req); err != nil {
			c.JSON(http.StatusBadRequest, gin.H{"error": "参数错误: " + err.Error()})
			return
		}

		userID := c.GetInt64("userId")

		// 查询用户配置
		config, err := repo.GetConfigByUserAndProvider(userID, req.Provider)
		if req.ConfigID > 0 {
			config, err = repo.GetConfigByID(req.ConfigID)
		}
		if err != nil {
			c.JSON(http.StatusBadRequest, gin.H{"error": err.Error()})
			return
		}

		log.Printf("[StreamHandler] userId=%d, provider=%s, model=%s",
			userID, config.Provider, config.GetEffectiveModelName())

		// 设置 SSE 响应头
		c.Header("Content-Type", "text/event-stream")
		c.Header("Cache-Control", "no-cache")
		c.Header("Connection", "keep-alive")
		c.Header("X-Accel-Buffering", "no") // Nginx SSE 必需

		ctx := c.Request.Context()
		contentCh, errCh := f.GenerateStream(ctx, config, req.Prompt)

		flusher := c.Writer

		// 逐 chunk 推送 SSE
		for content := range contentCh {
			data, _ := json.Marshal(content)
			fmt.Fprintf(flusher, "data: %s\n\n", data)
			flusher.Flush()
		}

		// 检查是否有错误
		select {
		case err = <-errCh:
			if err != nil {
				log.Printf("[StreamHandler] 流式生成错误: %v", err)
				fmt.Fprintf(flusher, "data: {\"error\":\"%s\"}\n\n", err.Error())
				flusher.Flush()
			}
		default:
		}

		// 发送结束标记
		fmt.Fprintf(flusher, "data: [DONE]\n\n")
		flusher.Flush()
	}
}
