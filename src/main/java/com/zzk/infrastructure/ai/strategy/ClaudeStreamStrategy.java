package com.zzk.infrastructure.ai.strategy;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.zzk.domain.model.entity.UserModelConfig;
import com.zzk.infrastructure.ai.adapter.StreamChunk;
import com.zzk.infrastructure.ai.adapter.StreamChunkParser;
import com.zzk.infrastructure.ai.adapter.StreamChunkParserRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Map;

/**
 * Anthropic Claude 流式生成策略
 * 
 * <p>
 * Claude 使用 Anthropic 自有的 API 格式：
 * - URL: /v1/messages
 * - 认证: Header 使用 x-api-key
 * - 请求头: anthropic-version: 2023-06-01
 * - 响应: SSE 流，事件类型为 content_block_delta
 * 
 * @author zzk
 * @since 1.0.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ClaudeStreamStrategy implements LlmStreamStrategy {

    private final WebClient.Builder webClientBuilder;
    private final StreamChunkParserRegistry parserRegistry;

    @Override
    public Flux<String> generateStream(UserModelConfig config, String prompt) {
        String baseUrl = config.getEffectiveBaseUrl();
        String model = config.getEffectiveModelName();

        log.info("[Claude] 调用 API: model={}", model);

        Map<String, Object> requestBody = Map.of(
                "model", model,
                "messages", List.of(Map.of("role", "user", "content", prompt)),
                "max_tokens", 4096,
                "stream", true);

        return webClientBuilder.build()
                .post()
                .uri(baseUrl + "/v1/messages")
                .header("x-api-key", config.getApiKey())
                .header("anthropic-version", "2023-06-01")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(requestBody)
                .retrieve()
                .onStatus(status -> status.value() == 429, response -> {
                    log.warn("[Claude] 触发速率限制 (429)");
                    return response.bodyToMono(String.class)
                            .flatMap(body -> reactor.core.publisher.Mono.error(
                                    new RuntimeException("API 请求过于频繁，请等待几秒后重试")));
                })
                .onStatus(status -> status.is4xxClientError() || status.is5xxServerError(), response -> {
                    return response.bodyToMono(String.class)
                            .flatMap(body -> {
                                log.error("[Claude] API 错误: status={}, body={}", response.statusCode(), body);
                                return reactor.core.publisher.Mono.error(
                                        new RuntimeException("API 调用失败: " + response.statusCode() + " - " + body));
                            });
                })
                .bodyToFlux(String.class)
                .retryWhen(reactor.util.retry.Retry.backoff(2, java.time.Duration.ofSeconds(2))
                        .filter(e -> e.getMessage() != null && e.getMessage().contains("429")))
                .filter(chunk -> !chunk.trim().isEmpty())
                .map(this::parseClaudeContent)
                .filter(content -> content != null && !content.isEmpty());
    }

    @Override
    public String[] getSupportedProviders() {
        return new String[] { "claude", "anthropic" };
    }

    @Override
    public Flux<StreamChunk> generateStreamChunks(UserModelConfig config, String prompt) {
        String baseUrl = config.getEffectiveBaseUrl();
        String model = config.getEffectiveModelName();
        String provider = config.getProvider();

        log.info("[Claude] 调用 API (StreamChunk): model={}", model);

        Map<String, Object> requestBody = Map.of(
                "model", model,
                "messages", List.of(Map.of("role", "user", "content", prompt)),
                "max_tokens", 4096,
                "stream", true);

        StreamChunkParser parser = parserRegistry.getParser(provider);

        return webClientBuilder.build()
                .post()
                .uri(baseUrl + "/v1/messages")
                .header("x-api-key", config.getApiKey())
                .header("anthropic-version", "2023-06-01")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(requestBody)
                .retrieve()
                .onStatus(status -> status.value() == 429, response -> response.bodyToMono(String.class)
                        .flatMap(body -> reactor.core.publisher.Mono.error(
                                new RuntimeException("API 请求过于频繁，请等待几秒后重试"))))
                .onStatus(status -> status.is4xxClientError() || status.is5xxServerError(),
                        response -> response.bodyToMono(String.class)
                                .flatMap(body -> {
                                    log.error("[Claude] API 错误: status={}, body={}", response.statusCode(), body);
                                    return reactor.core.publisher.Mono.error(
                                            new RuntimeException("API 调用失败: " + response.statusCode() + " - " + body));
                                }))
                .bodyToFlux(String.class)
                .retryWhen(reactor.util.retry.Retry.backoff(2, java.time.Duration.ofSeconds(2))
                        .filter(e -> e.getMessage() != null && e.getMessage().contains("429")))
                .filter(chunk -> !chunk.trim().isEmpty())
                .flatMapIterable(parser::parse)
                .filter(StreamChunk::hasContent);
    }

    /**
     * 解析 Claude 的 SSE 响应格式
     */
    private String parseClaudeContent(String chunk) {
        try {
            String jsonStr = chunk;
            if (chunk.startsWith("data:")) {
                jsonStr = chunk.substring(5).trim();
            }
            if (jsonStr.isEmpty()) {
                return "";
            }

            JSONObject json = JSON.parseObject(jsonStr);
            String type = json.getString("type");
            if ("content_block_delta".equals(type)) {
                JSONObject delta = json.getJSONObject("delta");
                if (delta != null) {
                    return delta.getString("text");
                }
            }
        } catch (Exception e) {
            log.warn("解析 Claude 响应失败: {}", e.getMessage());
        }
        return "";
    }
}
