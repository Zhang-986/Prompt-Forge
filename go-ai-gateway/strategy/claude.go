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

// ClaudeStrategy Anthropic Claude 流式生成策略
//
// Claude 使用 Anthropic 自有 API 格式：
// - URL: /v1/messages
// - 认证: Header x-api-key
// - 请求头: anthropic-version: 2023-06-01
// - 响应: SSE 流，事件类型 content_block_delta
type ClaudeStrategy struct {
	client *http.Client
}

// NewClaudeStrategy 创建 Claude 策略
func NewClaudeStrategy() *ClaudeStrategy {
	return &ClaudeStrategy{
		client: &http.Client{Timeout: 120 * time.Second},
	}
}

func (s *ClaudeStrategy) SupportedProviders() []string {
	return []string{"claude", "anthropic"}
}

func (s *ClaudeStrategy) GenerateStream(ctx context.Context, config *model.UserModelConfig, prompt string) (<-chan string, <-chan error) {
	contentCh := make(chan string, 64)
	errCh := make(chan error, 1)

	go func() {
		defer close(contentCh)
		defer close(errCh)

		err := s.doStreamWithRetry(ctx, config, prompt, contentCh)
		if err != nil {
			errCh <- err
		}
	}()

	return contentCh, errCh
}

func (s *ClaudeStrategy) doStreamWithRetry(ctx context.Context, config *model.UserModelConfig, prompt string, ch chan<- string) error {
	maxRetries := 2
	for attempt := 0; attempt <= maxRetries; attempt++ {
		err := s.doStream(ctx, config, prompt, ch)
		if err == nil {
			return nil
		}
		if !strings.Contains(err.Error(), "429") || attempt >= maxRetries {
			return err
		}
		backoff := time.Duration(math.Pow(2, float64(attempt))) * 2 * time.Second
		log.Printf("[Claude] 429 退避重试: attempt=%d, backoff=%v", attempt+1, backoff)
		select {
		case <-ctx.Done():
			return ctx.Err()
		case <-time.After(backoff):
		}
	}
	return nil
}

func (s *ClaudeStrategy) doStream(ctx context.Context, config *model.UserModelConfig, prompt string, ch chan<- string) error {
	baseURL := config.GetEffectiveBaseURL()
	modelName := config.GetEffectiveModelName()

	log.Printf("[Claude] 调用 API: model=%s", modelName)

	reqBody := map[string]interface{}{
		"model":      modelName,
		"messages":   []map[string]string{{"role": "user", "content": prompt}},
		"max_tokens": 4096,
		"stream":     true,
	}

	bodyBytes, _ := json.Marshal(reqBody)
	url := strings.TrimRight(baseURL, "/") + "/v1/messages"

	req, err := http.NewRequestWithContext(ctx, "POST", url, bytes.NewReader(bodyBytes))
	if err != nil {
		return fmt.Errorf("创建请求失败: %w", err)
	}

	// Claude 专有认证头
	req.Header.Set("Content-Type", "application/json")
	req.Header.Set("x-api-key", config.APIKey)
	req.Header.Set("anthropic-version", "2023-06-01")

	resp, err := s.client.Do(req)
	if err != nil {
		return fmt.Errorf("请求 Claude API 失败: %w", err)
	}
	defer resp.Body.Close()

	if resp.StatusCode != http.StatusOK {
		body, _ := io.ReadAll(resp.Body)
		if resp.StatusCode == 429 {
			return fmt.Errorf("429: API 请求过于频繁")
		}
		return fmt.Errorf("Claude API 错误: status=%d, body=%s", resp.StatusCode, string(body))
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
		if !strings.HasPrefix(line, "data:") {
			continue
		}

		data := strings.TrimSpace(line[5:])
		if data == "" {
			continue
		}

		content := parseClaudeContent(data)
		if content != "" {
			ch <- content
		}
	}

	return scanner.Err()
}

// parseClaudeContent 解析 Claude SSE 响应
// Claude 使用 content_block_delta 事件传递内容
func parseClaudeContent(data string) string {
	var payload struct {
		Type  string `json:"type"`
		Delta struct {
			Type string `json:"type"`
			Text string `json:"text"`
		} `json:"delta"`
	}

	if err := json.Unmarshal([]byte(data), &payload); err != nil {
		return ""
	}

	if payload.Type == "content_block_delta" {
		return payload.Delta.Text
	}

	return ""
}
