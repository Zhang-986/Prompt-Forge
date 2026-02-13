package strategy

import (
	"bufio"
	"bytes"
	"context"
	"encoding/json"
	"fmt"
	"io"
	"log"
	"math"
	"net/http"
	"strings"
	"time"

	"go-ai-gateway/model"
)

// GeminiStrategy Google Gemini 流式生成策略
//
// Google Gemini 使用独特的 API 格式：
// - URL: /v1beta/models/{model}:streamGenerateContent?key=xxx&alt=sse
// - 认证: Query String 传递 API Key
// - 请求体: contents[{parts[{text}]}]
// - 响应: candidates[{content{parts[{text}]}}]
type GeminiStrategy struct {
	client *http.Client
}

// NewGeminiStrategy 创建 Gemini 策略
func NewGeminiStrategy() *GeminiStrategy {
	return &GeminiStrategy{
		client: &http.Client{Timeout: 120 * time.Second},
	}
}

func (s *GeminiStrategy) SupportedProviders() []string {
	return []string{"google"}
}

func (s *GeminiStrategy) GenerateStream(ctx context.Context, config *model.UserModelConfig, prompt string) (<-chan StreamChunk, <-chan error) {
	chunkCh := make(chan StreamChunk, 64)
	errCh := make(chan error, 1)

	go func() {
		defer close(chunkCh)
		defer close(errCh)

		err := s.doStreamWithRetry(ctx, config, prompt, chunkCh)
		if err != nil {
			errCh <- err
		}
	}()

	return chunkCh, errCh
}

func (s *GeminiStrategy) doStreamWithRetry(ctx context.Context, config *model.UserModelConfig, prompt string, ch chan<- StreamChunk) error {
	maxRetries := 3
	for attempt := 0; attempt <= maxRetries; attempt++ {
		err := s.doStream(ctx, config, prompt, ch)
		if err == nil {
			return nil
		}
		if !strings.Contains(err.Error(), "429") || attempt >= maxRetries {
			return err
		}
		backoff := time.Duration(math.Pow(2, float64(attempt))) * 5 * time.Second
		log.Printf("[Google] 429 退避重试: attempt=%d, backoff=%v", attempt+1, backoff)
		select {
		case <-ctx.Done():
			return ctx.Err()
		case <-time.After(backoff):
		}
	}
	return nil
}

func (s *GeminiStrategy) doStream(ctx context.Context, config *model.UserModelConfig, prompt string, ch chan<- StreamChunk) error {
	baseURL := config.GetEffectiveBaseURL()
	modelName := config.GetEffectiveModelName()

	log.Printf("[Google] 调用 API: model=%s", modelName)

	// Google 特殊的 URL 和认证方式 (API Key 在 Query String)
	url := fmt.Sprintf("%s/v1beta/models/%s:streamGenerateContent?key=%s&alt=sse",
		strings.TrimRight(baseURL, "/"), modelName, config.APIKey)

	// Google 特殊的请求体结构
	reqBody := map[string]interface{}{
		"contents": []map[string]interface{}{
			{
				"parts": []map[string]string{
					{"text": prompt},
				},
			},
		},
	}

	bodyBytes, _ := json.Marshal(reqBody)

	req, err := http.NewRequestWithContext(ctx, "POST", url, bytes.NewReader(bodyBytes))
	if err != nil {
		return fmt.Errorf("创建请求失败: %w", err)
	}
	req.Header.Set("Content-Type", "application/json")

	resp, err := s.client.Do(req)
	if err != nil {
		return fmt.Errorf("请求 Gemini API 失败: %w", err)
	}
	defer resp.Body.Close()

	if resp.StatusCode != http.StatusOK {
		body, _ := io.ReadAll(resp.Body)
		if resp.StatusCode == 429 {
			return fmt.Errorf("429: API 请求过于频繁，请稍后重试")
		}
		return fmt.Errorf("Gemini API 错误: status=%d, body=%s", resp.StatusCode, string(body))
	}

	// SSE 解析
	scanner := bufio.NewScanner(resp.Body)
	scanner.Buffer(make([]byte, 0, 64*1024), 1024*1024)

	for scanner.Scan() {
		select {
		case <-ctx.Done():
			return ctx.Err()
		default:
		}

		line := scanner.Text()

		// Google SSE 格式或直接 JSON
		var jsonStr string
		if strings.HasPrefix(line, "data:") {
			jsonStr = strings.TrimSpace(line[5:])
		} else if strings.HasPrefix(strings.TrimSpace(line), "{") {
			jsonStr = strings.TrimSpace(line)
		} else {
			continue
		}

		if jsonStr == "" || jsonStr == "[DONE]" {
			continue
		}

		chunks := extractGeminiChunks(jsonStr)
		for _, chunk := range chunks {
			ch <- chunk
		}
	}

	return scanner.Err()
}

// extractGeminiChunks 从 Gemini 响应中提取 StreamChunk
// 支持 thought (Gemini 2.0 Flash Thinking) 和 text 分离
func extractGeminiChunks(data string) []StreamChunk {
	var payload struct {
		Candidates []struct {
			Content struct {
				Parts []struct {
					Text         string `json:"text"`
					Thought      string `json:"thought"`
					FunctionCall *struct {
						Name string                 `json:"name"`
						Args map[string]interface{} `json:"args"`
					} `json:"functionCall"`
				} `json:"parts"`
			} `json:"content"`
		} `json:"candidates"`
	}

	if err := json.Unmarshal([]byte(data), &payload); err != nil {
		log.Printf("解析 Gemini 响应失败: %v", err)
		return nil
	}

	if len(payload.Candidates) == 0 {
		return nil
	}

	var chunks []StreamChunk
	parts := payload.Candidates[0].Content.Parts

	for _, part := range parts {
		// 深度思考 (Gemini 2.0 Flash Thinking)
		if part.Thought != "" {
			chunks = append(chunks, Reasoning(part.Thought))
		}

		// 正式回答
		if part.Text != "" {
			chunks = append(chunks, Content(part.Text))
		}

		// 函数调用
		if part.FunctionCall != nil {
			toolJSON, _ := json.Marshal(part.FunctionCall)
			chunks = append(chunks, ToolCall(string(toolJSON)))
		}
	}

	return chunks
}
