package com.zzk.infrastructure.ai.strategy;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.zzk.domain.model.entity.UserModelConfig;
import com.zzk.infrastructure.ai.adapter.StreamChunk;
import com.zzk.infrastructure.ai.adapter.StreamChunkParser;
import com.zzk.infrastructure.ai.adapter.StreamChunkParserRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;

import java.util.List;

/**
 * Google Gemini 流式生成策略
 * 
 * <p>
 * Google Gemini 使用独特的 API 格式：
 * - URL: /v1beta/models/{model}:streamGenerateContent
 * - 认证: Query String 传递 API Key
 * - 请求体: contents[{parts[{text}]}] 结构
 * - 响应: candidates[{content{parts[{text}]}}] 结构
 * 
 * @author zzk
 * @since 1.0.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class GoogleGeminiStreamStrategy implements LlmStreamStrategy {

    private final WebClient.Builder webClientBuilder;
    private final StreamChunkParserRegistry parserRegistry;

    @Override
    public Flux<String> generateStream(UserModelConfig config, String prompt) {
        String baseUrl = config.getEffectiveBaseUrl();
        String model = config.getEffectiveModelName();
        String url = String.format("%s/v1beta/models/%s:streamGenerateContent?key=%s&alt=sse",
                baseUrl, model, config.getApiKey());

        log.info("[Google] 调用 API: model={}", model);

        // Google 特殊的请求体结构
        JSONObject requestBody = new JSONObject();
        JSONObject part = new JSONObject();
        part.put("text", prompt);
        JSONObject content = new JSONObject();
        content.put("parts", List.of(part));
        requestBody.put("contents", List.of(content));

        return webClientBuilder.build()
                .post()
                .uri(url)
                .bodyValue(requestBody)
                .retrieve()
                .onStatus(status -> status.value() == 429, response -> {
                    log.warn("[Google] 触发速率限制 (429)，请稍后重试");
                    return response.bodyToMono(String.class)
                            .flatMap(body -> reactor.core.publisher.Mono.error(
                                    new RuntimeException("API 请求过于频繁，请等待几秒后重试")));
                })
                .onStatus(status -> status.is4xxClientError() || status.is5xxServerError(), response -> {
                    return response.bodyToMono(String.class)
                            .flatMap(body -> {
                                log.error("[Google] API 错误: status={}, body={}", response.statusCode(), body);
                                return reactor.core.publisher.Mono.error(
                                        new RuntimeException("API 调用失败: " + response.statusCode() + " - " + body));
                            });
                })
                .bodyToFlux(String.class)
                .retryWhen(reactor.util.retry.Retry.backoff(3, java.time.Duration.ofSeconds(5))
                        .filter(e -> e.getMessage() != null && e.getMessage().contains("429")))
                .flatMap(this::parseGoogleResponse);
    }

    @Override
    public String[] getSupportedProviders() {
        return new String[] { "google", "gemini" };
    }

    @Override
    public Flux<StreamChunk> generateStreamChunks(UserModelConfig config, String prompt) {
        String baseUrl = config.getEffectiveBaseUrl();
        String model = config.getEffectiveModelName();
        String provider = config.getProvider();
        String url = String.format("%s/v1beta/models/%s:streamGenerateContent?key=%s&alt=sse",
                baseUrl, model, config.getApiKey());

        log.info("[Google] 调用 API (StreamChunk): model={}", model);

        JSONObject requestBody = new JSONObject();
        JSONObject part = new JSONObject();
        part.put("text", prompt);
        JSONObject content = new JSONObject();
        content.put("parts", List.of(part));
        requestBody.put("contents", List.of(content));

        return webClientBuilder.build()
                .post()
                .uri(url)
                .bodyValue(requestBody)
                .retrieve()
                .onStatus(status -> status.value() == 429, response -> response.bodyToMono(String.class)
                        .flatMap(body -> reactor.core.publisher.Mono.error(
                                new RuntimeException("API 请求过于频繁，请稍后再试"))))
                .onStatus(status -> status.is4xxClientError() || status.is5xxServerError(),
                        response -> response.bodyToMono(String.class)
                                .flatMap(body -> {
                                    log.error("[Google] API 错误: status={}, body={}", response.statusCode(), body);
                                    return reactor.core.publisher.Mono.error(
                                            new RuntimeException("API 调用失败: " + response.statusCode() + " - " + body));
                                }))
                .bodyToFlux(String.class)
                .retryWhen(reactor.util.retry.Retry.backoff(3, java.time.Duration.ofSeconds(5))
                        .filter(e -> e.getMessage() != null && e.getMessage().contains("429")))
                .flatMapIterable(this::parseRawToChunks)
                .filter(StreamChunk::hasContent);
    }

    /**
     * 将原始响应切分后使用 parser 解析
     */
    private List<StreamChunk> parseRawToChunks(String chunk) {
        if (chunk == null || chunk.isEmpty()) {
            return List.of();
        }

        String trimmed = chunk.trim();
        if (trimmed.startsWith("data:")) {
            trimmed = trimmed.substring(5).trim();
        }

        if (trimmed.isEmpty() || trimmed.equals("[DONE]")) {
            return List.of();
        }

        // 复用注册表中的 GeminiChunkParser
        StreamChunkParser parser = parserRegistry.getParser("google");
        return parser.parse(trimmed);
    }

    /**
     * 解析 Google 的 SSE 响应格式
     */
    private Flux<String> parseGoogleResponse(String chunk) {
        if (chunk == null || chunk.isEmpty()) {
            return Flux.empty();
        }

        String trimmed = chunk.trim();
        if (trimmed.startsWith("{") || trimmed.startsWith("[")) {
            String content = extractGoogleContent(trimmed);
            return content.isEmpty() ? Flux.empty() : Flux.just(content);
        }

        // SSE 格式
        String[] lines = chunk.split("\n");
        return Flux.fromArray(lines)
                .map(String::trim)
                .filter(line -> line.startsWith("data:"))
                .map(line -> line.substring(5).trim())
                .filter(json -> !json.equals("[DONE]"))
                .map(this::extractGoogleContent)
                .filter(content -> !content.isEmpty());
    }

    /**
     * 从 Google 响应 JSON 中提取文本内容
     */
    private String extractGoogleContent(String jsonResponse) {
        try {
            if (jsonResponse.trim().startsWith("{")) {
                JSONObject response = JSON.parseObject(jsonResponse);
                JSONArray candidates = response.getJSONArray("candidates");
                if (candidates != null && !candidates.isEmpty()) {
                    JSONObject candidate = candidates.getJSONObject(0);
                    JSONObject content = candidate.getJSONObject("content");
                    if (content != null) {
                        JSONArray parts = content.getJSONArray("parts");
                        if (parts != null && !parts.isEmpty()) {
                            return parts.getJSONObject(0).getString("text");
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.warn("解析 Google 响应失败: {}", e.getMessage());
        }
        return "";
    }
}
