package factory

import (
	"context"
	"fmt"
	"log"

	"go-ai-gateway/model"
	"go-ai-gateway/strategy"
)

// DynamicLlmClientFactory 动态 LLM 客户端工厂
//
// 设计模式：工厂模式 + 策略模式
// - 工厂模式：根据 provider 选择合适的策略
// - 策略模式：不同厂商的 API 调用逻辑封装为独立策略
type DynamicLlmClientFactory struct {
	strategies map[string]strategy.LlmStreamStrategy
}

// NewFactory 创建工厂并注册所有策略
func NewFactory(strategies ...strategy.LlmStreamStrategy) *DynamicLlmClientFactory {
	f := &DynamicLlmClientFactory{
		strategies: make(map[string]strategy.LlmStreamStrategy),
	}

	for _, s := range strategies {
		for _, provider := range s.SupportedProviders() {
			f.strategies[provider] = s
			log.Printf("[Factory] 注册策略: %s -> %T", provider, s)
		}
	}

	log.Printf("[Factory] 初始化完成，已注册 %d 个厂商策略", len(f.strategies))
	return f
}

// GenerateStream 使用用户配置创建流式生成
//
// 工厂方法：根据 provider 从策略映射中选择对应策略
func (f *DynamicLlmClientFactory) GenerateStream(ctx context.Context, config *model.UserModelConfig, prompt string) (<-chan string, <-chan error) {
	provider := config.Provider

	s, ok := f.strategies[provider]
	if !ok {
		log.Printf("未找到厂商 '%s' 的策略实现，尝试使用 OpenAI 兼容模式", provider)
		// 降级策略：使用 OpenAI 兼容策略
		s, ok = f.strategies["openai"]
	}

	if !ok {
		errCh := make(chan error, 1)
		errCh <- fmt.Errorf("无法找到合适的 AI 调用策略: provider=%s", provider)
		close(errCh)
		ch := make(chan string)
		close(ch)
		return ch, errCh
	}

	return s.GenerateStream(ctx, config, prompt)
}

// GetStrategy 获取指定 provider 的策略（用于测试）
func (f *DynamicLlmClientFactory) GetStrategy(provider string) (strategy.LlmStreamStrategy, bool) {
	s, ok := f.strategies[provider]
	return s, ok
}

// RegisteredProviders 获取所有已注册的 provider 列表
func (f *DynamicLlmClientFactory) RegisteredProviders() []string {
	providers := make([]string, 0, len(f.strategies))
	for p := range f.strategies {
		providers = append(providers, p)
	}
	return providers
}
