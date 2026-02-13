package strategy

import (
	"context"
	"go-ai-gateway/model"
)

// LlmStreamStrategy LLM 流式生成策略接口
//
// 设计模式：策略模式 (Strategy Pattern)
// 封装不同 AI 厂商的 API 调用逻辑，让算法独立于使用它的客户端变化
type LlmStreamStrategy interface {
	// GenerateStream 流式生成文本
	// 返回一个只读 channel 用于接收 StreamChunk（区分 reasoning/content），以及一个 error channel
	// channel 关闭表示流结束
	GenerateStream(ctx context.Context, config *model.UserModelConfig, prompt string) (<-chan StreamChunk, <-chan error)

	// SupportedProviders 返回此策略支持的厂商标识符列表
	SupportedProviders() []string
}
