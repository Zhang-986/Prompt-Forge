package com.zzk.infrastructure.ai.strategy;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Map;

/**
 * DeepSeek 策略实现
 * 
 * <p>使用 WebClient 调用 DeepSeek API
 * 
 * @author zzk
 * @since 1.0.0
 */
@Slf4j
@Component
public class DeepSeekStrategy implements LlmGenerationStrategy {

    private static final String MODEL_ID = "deepseek";
    private static final String MODEL_NAME = "DeepSeek";

    private final WebClient webClient;

    @Value("${llm.models.deepseek.enabled:true}")
    private boolean enabled;

    @Value("${llm.models.deepseek.api-key:}")
    private String apiKey;

    @Value("${llm.models.deepseek.model:deepseek-chat}")
    private String model;

    public DeepSeekStrategy(@Value("${llm.models.deepseek.base-url:https://api.deepseek.com}") String baseUrl) {
        this.webClient = WebClient.builder()
                .baseUrl(baseUrl)
                .build();
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

        Map<String, Object> requestBody = Map.of(
                "model", model,
                "messages", List.of(Map.of("role", "user", "content", prompt)),
                "stream", true
        );

        return webClient.post()
                .uri("/v1/chat/completions")
                .header("Authorization", "Bearer " + apiKey)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(requestBody)
                .retrieve()
                .bodyToFlux(String.class)
                .filter(chunk -> !chunk.equals("[DONE]"))
                .map(this::extractContent)
                .filter(content -> content != null && !content.isEmpty());
    }

    @Override
    @CircuitBreaker(name = "llmService", fallbackMethod = "generateFallback")
    @RateLimiter(name = "llmService")
    @Retry(name = "llmService")
    public String generate(String prompt) {
        log.debug("[{}] 开始同步生成: {}", MODEL_ID, prompt.substring(0, Math.min(100, prompt.length())));

        Map<String, Object> requestBody = Map.of(
                "model", model,
                "messages", List.of(Map.of("role", "user", "content", prompt)),
                "stream", false
        );

        String response = webClient.post()
                .uri("/v1/chat/completions")
                .header("Authorization", "Bearer " + apiKey)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(String.class)
                .block();

        return extractContentFromResponse(response);
    }

    /**
     * 从 SSE 数据块中提取内容
     */
    private String extractContent(String chunk) {
        try {
            // 解析 SSE 格式: data: {"choices": [{"delta": {"content": "xxx"}}]}
            if (chunk.startsWith("data: ")) {
                chunk = chunk.substring(6);
            }
            // 简化处理，实际应使用 JSON 解析
            int contentStart = chunk.indexOf("\"content\":\"");
            if (contentStart != -1) {
                contentStart += 11;
                int contentEnd = chunk.indexOf("\"", contentStart);
                if (contentEnd != -1) {
                    return chunk.substring(contentStart, contentEnd);
                }
            }
        } catch (Exception e) {
            log.warn("解析 DeepSeek 响应失败: {}", e.getMessage());
        }
        return "";
    }

    /**
     * 从完整响应中提取内容
     */
    private String extractContentFromResponse(String response) {
        try {
            // 简化处理
            int contentStart = response.indexOf("\"content\":\"");
            if (contentStart != -1) {
                contentStart += 11;
                int contentEnd = response.indexOf("\"", contentStart);
                if (contentEnd != -1) {
                    return response.substring(contentStart, contentEnd);
                }
            }
        } catch (Exception e) {
            log.warn("解析 DeepSeek 响应失败: {}", e.getMessage());
        }
        return response;
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
