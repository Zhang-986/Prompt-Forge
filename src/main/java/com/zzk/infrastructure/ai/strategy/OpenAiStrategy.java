package com.zzk.infrastructure.ai.strategy;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

/**
 * OpenAI (GPT-4) 策略实现
 * 
 * <p>集成 Resilience4j 实现熔断、限流、重试
 * 
 * @author zzk
 * @since 1.0.0
 */
@Slf4j
@Component
public class OpenAiStrategy implements LlmGenerationStrategy {

    private static final String MODEL_ID = "gpt-4";
    private static final String MODEL_NAME = "GPT-4";

    private final ChatClient chatClient;

    @Value("${llm.models.openai.enabled:true}")
    private boolean enabled;

    public OpenAiStrategy(ChatModel chatModel) {
        this.chatClient = ChatClient.builder(chatModel).build();
    }

    @Override
    public String getModelId() {
        return MODEL_ID;
    }

    @Override
    public String getModelName() {
        return MODEL_NAME;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    @CircuitBreaker(name = "llmService", fallbackMethod = "generateStreamFallback")
    @RateLimiter(name = "llmService")
    @Retry(name = "llmService")
    public Flux<String> generateStream(String prompt) {
        log.debug("[{}] 开始流式生成: {}", MODEL_ID, prompt.substring(0, Math.min(100, prompt.length())));
        
        return chatClient.prompt()
                .user(prompt)
                .stream()
                .content();
    }

    @Override
    @CircuitBreaker(name = "llmService", fallbackMethod = "generateFallback")
    @RateLimiter(name = "llmService")
    @Retry(name = "llmService")
    public String generate(String prompt) {
        log.debug("[{}] 开始同步生成: {}", MODEL_ID, prompt.substring(0, Math.min(100, prompt.length())));
        
        return chatClient.prompt()
                .user(prompt)
                .call()
                .content();
    }

    /**
     * 流式生成降级方法
     */
    public Flux<String> generateStreamFallback(String prompt, Throwable t) {
        log.error("[{}] 流式生成失败，触发降级: {}", MODEL_ID, t.getMessage());
        return Flux.just("【" + MODEL_NAME + " 服务繁忙，请稍后重试】");
    }

    /**
     * 同步生成降级方法
     */
    public String generateFallback(String prompt, Throwable t) {
        log.error("[{}] 同步生成失败，触发降级: {}", MODEL_ID, t.getMessage());
        return "【" + MODEL_NAME + " 服务繁忙，请稍后重试】";
    }
}
