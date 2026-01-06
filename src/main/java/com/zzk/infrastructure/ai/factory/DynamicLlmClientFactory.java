package com.zzk.infrastructure.ai.factory;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.zzk.domain.model.entity.UserModelConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Map;

/**
 * 动态 LLM 客户端工厂
 * 
 * <p>根据用户配置动态创建 LLM 调用客户端
 * 
 * @author zzk
 * @since 1.0.0
 */
@Slf4j
@Component
public class DynamicLlmClientFactory {

    private final WebClient.Builder webClientBuilder;

    public DynamicLlmClientFactory(WebClient.Builder webClientBuilder) {
        this.webClientBuilder = webClientBuilder;
    }

    /**
     * 使用用户配置创建流式生成
     */
    public Flux<String> generateStream(UserModelConfig config, String prompt) {
        return switch (config.getProvider()) {
            case "google" -> generateGoogleStream(config, prompt);
            case "zhipu", "deepseek", "openai", "aliyun", "moonshot", "cloudflare", "github", "hunyuan" -> generateOpenAICompatibleStream(config, prompt);
            case "claude" -> generateClaudeStream(config, prompt);
            default -> Flux.error(new RuntimeException("不支持的提供商: " + config.getProvider()));
        };
    }

    /**
     * Google Gemini 流式生成
     */
    private Flux<String> generateGoogleStream(UserModelConfig config, String prompt) {
        String baseUrl = config.getEffectiveBaseUrl();
        String model = config.getEffectiveModelName();
        String url = String.format("%s/v1beta/models/%s:streamGenerateContent?key=%s&alt=sse", 
                baseUrl, model, config.getApiKey());

        log.info("[Google] 调用 API: model={}", model);

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
                                        new RuntimeException("API 调用失败: " + response.statusCode()));
                            });
                })
                .bodyToFlux(String.class)
                .retryWhen(reactor.util.retry.Retry.backoff(3, java.time.Duration.ofSeconds(5))
                        .filter(e -> e.getMessage() != null && e.getMessage().contains("429")))
                .flatMap(this::parseGoogleResponse);
    }

    /**
     * OpenAI 兼容格式流式生成 (适用于 Zhipu, DeepSeek, OpenAI)
     */
    private Flux<String> generateOpenAICompatibleStream(UserModelConfig config, String prompt) {
        String baseUrl = config.getEffectiveBaseUrl();
        String model = config.getEffectiveModelName();

        log.info("[{}] 调用 API: model={}", config.getProvider(), model);

        // 构建请求体
        Map<String, Object> requestBody = new java.util.HashMap<>();
        requestBody.put("model", model);
        requestBody.put("messages", List.of(Map.of("role", "user", "content", prompt)));
        requestBody.put("stream", true);
        
        // Cloudflare 默认 max_tokens 较小，需要显式设置
        if (config.getProvider().equals("cloudflare")) {
            requestBody.put("max_tokens", 4096);
        }

        // 智谱、阿里云、Moonshot、Cloudflare 的 Base URL 已包含版本路径，其他用 /v1/chat/completions
        String chatEndpoint;
        if (config.getProvider().equals("zhipu")) {
            chatEndpoint = "/chat/completions";
        } else if (config.getProvider().equals("aliyun")) {
            chatEndpoint = "/chat/completions"; // Base URL 已包含 /compatible-mode/v1
        } else if (config.getProvider().equals("moonshot")) {
            chatEndpoint = "/chat/completions"; // Base URL 已包含 /v1
        } else if (config.getProvider().equals("cloudflare")) {
            chatEndpoint = "/chat/completions"; // Base URL 已包含 /ai/v1
        } else if (config.getProvider().equals("github")) {
            chatEndpoint = "/chat/completions?api-version=2024-12-01-preview"; // Azure-style, 需要 api-version
        } else if (config.getProvider().equals("hunyuan")) {
            chatEndpoint = "/chat/completions"; // Base URL 已包含 /v1
        } else {
            chatEndpoint = "/v1/chat/completions";
        }

        return webClientBuilder.build()
                .post()
                .uri(baseUrl + chatEndpoint)
                .header("Authorization", "Bearer " + config.getApiKey())
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(requestBody)
                .retrieve()
                .onStatus(status -> status.value() == 429, response -> {
                    log.warn("[{}] 触发速率限制 (429)", config.getProvider());
                    return response.bodyToMono(String.class)
                            .flatMap(body -> reactor.core.publisher.Mono.error(
                                    new RuntimeException("API 请求过于频繁，请等待几秒后重试")));
                })
                .onStatus(status -> status.is4xxClientError() || status.is5xxServerError(), response -> {
                    return response.bodyToMono(String.class)
                            .flatMap(body -> {
                                log.error("[{}] API 错误: status={}, body={}", config.getProvider(), response.statusCode(), body);
                                return reactor.core.publisher.Mono.error(
                                        new RuntimeException("API 调用失败: " + response.statusCode()));
                            });
                })
                .bodyToFlux(String.class)
                .retryWhen(reactor.util.retry.Retry.backoff(2, java.time.Duration.ofSeconds(2))
                        .filter(e -> e.getMessage() != null && e.getMessage().contains("429")))
                .filter(chunk -> !chunk.equals("[DONE]") && !chunk.trim().isEmpty())
                .map(this::parseOpenAIContent)
                .filter(content -> content != null && !content.isEmpty());
    }

    /**
     * Claude 流式生成
     */
    private Flux<String> generateClaudeStream(UserModelConfig config, String prompt) {
        String baseUrl = config.getEffectiveBaseUrl();
        String model = config.getEffectiveModelName();

        log.info("[Claude] 调用 API: model={}", model);

        Map<String, Object> requestBody = Map.of(
                "model", model,
                "messages", List.of(Map.of("role", "user", "content", prompt)),
                "max_tokens", 4096,
                "stream", true
        );

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
                                        new RuntimeException("API 调用失败: " + response.statusCode()));
                            });
                })
                .bodyToFlux(String.class)
                .retryWhen(reactor.util.retry.Retry.backoff(2, java.time.Duration.ofSeconds(2))
                        .filter(e -> e.getMessage() != null && e.getMessage().contains("429")))
                .filter(chunk -> !chunk.trim().isEmpty())
                .map(this::parseClaudeContent)
                .filter(content -> content != null && !content.isEmpty());
    }

    /**
     * 解析 Google 响应
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

    /**
     * 解析 OpenAI 兼容格式响应
     */
    private String parseOpenAIContent(String chunk) {
        try {
            String jsonStr = chunk;
            if (chunk.startsWith("data:")) {
                jsonStr = chunk.substring(5).trim();
            }
            if (jsonStr.isEmpty() || jsonStr.equals("[DONE]")) {
                return "";
            }

            JSONObject json = JSON.parseObject(jsonStr);
            JSONArray choices = json.getJSONArray("choices");
            if (choices != null && !choices.isEmpty()) {
                JSONObject delta = choices.getJSONObject(0).getJSONObject("delta");
                if (delta != null && delta.containsKey("content")) {
                    return delta.getString("content");
                }
            }
        } catch (Exception e) {
            log.warn("解析 OpenAI 响应失败: {}", e.getMessage());
        }
        return "";
    }

    /**
     * 解析 Claude 响应
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
    /**
     * 获取可用模型列表 (仅限 OpenAI 兼容接口)
     */
    public List<String> fetchAvailableModels(UserModelConfig config) {
        String baseUrl = config.getEffectiveBaseUrl();
        if (baseUrl == null) {
            throw new RuntimeException("Base URL cannot be null");
        }
        if (baseUrl.endsWith("/")) {
            baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
        }

        String response = webClientBuilder.build()
                .get()
                .uri(baseUrl + "/v1/models")
                .header("Authorization", "Bearer " + config.getApiKey())
                .retrieve()
                .bodyToMono(String.class)
                .block(); // Sync call

        JSONObject json = JSON.parseObject(response);
        if (json.containsKey("data")) {
            JSONArray data = json.getJSONArray("data");
            return data.stream()
                    .map(obj -> ((JSONObject) obj).getString("id"))
                    .toList();
        }
        return List.of();
    }
}
