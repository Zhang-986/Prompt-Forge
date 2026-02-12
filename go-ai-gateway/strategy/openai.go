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

// OpenAIStrategy OpenAI 兼容格式流式生成策略
//
// 适用于所有遵循 OpenAI API 规范的厂商：
// OpenAI, 智谱, DeepSeek, 通义千问, Moonshot, Cloudflare, Groq 等 20+
type OpenAIStrategy struct {
	client *http.Client
}

// NewOpenAIStrategy 创建 OpenAI 兼容策略
func NewOpenAIStrategy() *OpenAIStrategy {
	return &OpenAIStrategy{
		client: &http.Client{Timeout: 120 * time.Second},
	}
}

func (s *OpenAIStrategy) SupportedProviders() []string {
	return []string{
		"openai", "zhipu", "deepseek", "aliyun", "qwen", "moonshot",
		"cloudflare", "github", "hunyuan", "azure", "bedrock",
		"baichuan", "minimax", "stepfun", "yi", "sensenova",
		"mistral", "perplexity", "groq", "cohere", "novita",
		"togetherai", "ollama", "openrouter",
	}
}

func (s *OpenAIStrategy) GenerateStream(ctx context.Context, config *model.UserModelConfig, prompt string) (<-chan string, <-chan error) {
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

// doStreamWithRetry 带 429 退避重试的流式请求
func (s *OpenAIStrategy) doStreamWithRetry(ctx context.Context, config *model.UserModelConfig, prompt string, ch chan<- string) error {
	maxRetries := 2
	for attempt := 0; attempt <= maxRetries; attempt++ {
		err := s.doStream(ctx, config, prompt, ch)
		if err == nil {
			return nil
		}

		// 仅对 429 错误重试
		if !strings.Contains(err.Error(), "429") || attempt >= maxRetries {
			return err
		}

		// 指数退避
		backoff := time.Duration(math.Pow(2, float64(attempt))) * 2 * time.Second
		log.Printf("[OpenAI] 429 退避重试: attempt=%d, backoff=%v", attempt+1, backoff)

		select {
		case <-ctx.Done():
			return ctx.Err()
		case <-time.After(backoff):
		}
	}
	return nil
}

// doStream 执行单次流式请求
func (s *OpenAIStrategy) doStream(ctx context.Context, config *model.UserModelConfig, prompt string, ch chan<- string) error {
	baseURL := config.GetEffectiveBaseURL()
	modelName := config.GetEffectiveModelName()
	provider := config.Provider

	log.Printf("[%s] 调用 API: model=%s", provider, modelName)

	// 构建请求体
	reqBody := map[string]interface{}{
		"model":    modelName,
		"messages": []map[string]string{{"role": "user", "content": prompt}},
		"stream":   true,
	}

	// Cloudflare 需要显式设置 max_tokens
	if provider == "cloudflare" {
		reqBody["max_tokens"] = 4096
	}

	bodyBytes, _ := json.Marshal(reqBody)
	endpoint := s.determineChatEndpoint(provider, modelName)
	url := strings.TrimRight(baseURL, "/") + endpoint

	req, err := http.NewRequestWithContext(ctx, "POST", url, bytes.NewReader(bodyBytes))
	if err != nil {
		return fmt.Errorf("创建请求失败: %w", err)
	}

	req.Header.Set("Content-Type", "application/json")
	req.Header.Set("Authorization", "Bearer "+config.APIKey)

	resp, err := s.client.Do(req)
	if err != nil {
		return fmt.Errorf("请求 AI API 失败: %w", err)
	}
	defer resp.Body.Close()

	// 错误处理
	if resp.StatusCode != http.StatusOK {
		body, _ := io.ReadAll(resp.Body)
		errMsg := sanitizeAPIError(resp.StatusCode, string(body))
		if resp.StatusCode == 429 {
			return fmt.Errorf("429: %s", errMsg)
		}
		return fmt.Errorf(errMsg)
	}

	// SSE 流式解析
	scanner := bufio.NewScanner(resp.Body)
	// 增大 buffer 防止超长行截断
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
		if data == "" || data == "[DONE]" {
			continue
		}

		content := parseOpenAIContent(data)
		if content != "" {
			ch <- content
		}
	}

	return scanner.Err()
}

// determineChatEndpoint 根据厂商确定 API 端点
func (s *OpenAIStrategy) determineChatEndpoint(provider, modelName string) string {
	switch provider {
	case "github":
		return "/chat/completions?api-version=2024-12-01-preview"
	case "azure":
		return "/openai/deployments/" + modelName + "/chat/completions?api-version=2024-02-01"
	case "bedrock":
		return "/model/" + modelName + "/invoke"
	default:
		return "/chat/completions"
	}
}

// parseOpenAIContent 解析 OpenAI 格式的流式响应
func parseOpenAIContent(data string) string {
	var payload struct {
		Choices []struct {
			Delta struct {
				Content          *string `json:"content"`
				ReasoningContent *string `json:"reasoning_content"`
			} `json:"delta"`
		} `json:"choices"`
	}

	if err := json.Unmarshal([]byte(data), &payload); err != nil {
		log.Printf("解析 OpenAI 响应失败: %v", err)
		return ""
	}

	if len(payload.Choices) == 0 {
		return ""
	}

	var sb strings.Builder
	delta := payload.Choices[0].Delta

	// DeepSeek R1 推理内容
	if delta.ReasoningContent != nil {
		sb.WriteString(*delta.ReasoningContent)
	}
	// 标准内容
	if delta.Content != nil {
		sb.WriteString(*delta.Content)
	}

	return sb.String()
}

// sanitizeAPIError 对 API 错误信息进行脱敏处理
func sanitizeAPIError(statusCode int, rawBody string) string {
	switch statusCode {
	case 401:
		return "API Key 无效或已过期，请在「模型配置」中检查您的 API Key"
	case 403:
		return "API Key 权限不足，请确认是否已开通对应模型的访问权限"
	case 404:
		if strings.Contains(rawBody, "model") {
			return "请求的模型不存在或未开通，请检查模型配置"
		}
		return "API 接口不存在，请检查配置的 Base URL 是否正确"
	case 429:
		return "API 请求过于频繁，请稍后再试"
	case 500, 502, 503:
		return "AI 服务暂时不可用，请稍后再试"
	default:
		return fmt.Sprintf("AI 服务调用失败 (错误码: %d)，请稍后重试或联系管理员", statusCode)
	}
}
