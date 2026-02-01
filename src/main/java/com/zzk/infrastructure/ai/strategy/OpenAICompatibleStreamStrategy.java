package com.zzk.infrastructure.ai.strategy;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.zzk.domain.model.entity.UserModelConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Map;

/**
 * OpenAI 兼容格式流式生成策略
 * 
 * <p>适用于所有遵循 OpenAI API 规范的厂商：
 * - OpenAI (GPT-4, GPT-3.5)
 * - 智谱 AI (GLM-4)
 * - DeepSeek
 * - 通义千问 (Qwen)
 * - Moonshot (Kimi)
 * - 以及其他 20+ 兼容厂商
 * 
 * @author zzk
 * @since 1.0.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OpenAICompatibleStreamStrategy implements LlmStreamStrategy {

    private final WebClient.Builder webClientBuilder;

    @Override
    public Flux<String> generateStream(UserModelConfig config, String prompt) {
        String baseUrl = config.getEffectiveBaseUrl();
        String model = config.getEffectiveModelName();
        String provider = config.getProvider();

        log.info("[{}] [{}] 调用 API: model={}", 
                Thread.currentThread().getName(), provider, model);

        // 构建请求体
        Map<String, Object> requestBody = new java.util.HashMap<>();
        requestBody.put("model", model);
        requestBody.put("messages", List.of(Map.of("role", "user", "content", prompt)));
        requestBody.put("stream", true);

        // Cloudflare 默认 max_tokens 较小，需要显式设置
        if ("cloudflare".equals(provider)) {
            requestBody.put("max_tokens", 4096);
        }

        // 根据不同厂商确定 API 端点
        String chatEndpoint = determineChatEndpoint(provider, model);

        return webClientBuilder.build()
                .post()
                .uri(baseUrl + chatEndpoint)
                .header("Authorization", "Bearer " + config.getApiKey())
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(requestBody)
                .retrieve()
                .onStatus(status -> status.value() == 429, response -> {
                    log.warn("[{}] 触发速率限制 (429)", provider);
                    return response.bodyToMono(String.class)
                            .flatMap(body -> reactor.core.publisher.Mono.error(
                                    new RuntimeException("API 请求过于频繁，请等待几秒后重试")));
                })
                .onStatus(status -> status.is4xxClientError() || status.is5xxServerError(), response -> {
                    return response.bodyToMono(String.class)
                            .flatMap(body -> {
                                log.error("[{}] API 错误: status={}, body={}", provider,
                                        response.statusCode(), body);
                                String userFriendlyMessage = sanitizeApiError(response.statusCode().value(), body);
                                return reactor.core.publisher.Mono.error(
                                        new RuntimeException(userFriendlyMessage));
                            });
                })
                .bodyToFlux(String.class)
                .doOnSubscribe(sub -> log.info("    [{}] WebClient开始接收流式数据", Thread.currentThread().getName()))
                .retryWhen(reactor.util.retry.Retry.backoff(2, java.time.Duration.ofSeconds(2))
                        .filter(e -> e.getMessage() != null && e.getMessage().contains("429")))
                .filter(chunk -> !chunk.equals("[DONE]") && !chunk.trim().isEmpty())
                .map(this::parseOpenAIContent)
                .filter(content -> content != null && !content.isEmpty())
                .doOnComplete(() -> log.info("    [{}] WebClient流式接收完成", Thread.currentThread().getName()));
    }

    @Override
    public String[] getSupportedProviders() {
        return new String[]{
                "openai", "zhipu", "deepseek", "aliyun", "qwen", "moonshot", 
                "cloudflare", "github", "hunyuan", "azure", "bedrock", 
                "baichuan", "minimax", "stepfun", "yi", "sensenova",
                "mistral", "perplexity", "groq", "cohere", "novita", 
                "togetherai", "ollama", "openrouter"
        };
    }

    /**
     * 根据厂商和模型确定 API 端点
     */
    private String determineChatEndpoint(String provider, String model) {
        return switch (provider) {
            case "github" -> "/chat/completions?api-version=2024-12-01-preview";
            case "azure" -> "/openai/deployments/" + model + "/chat/completions?api-version=2024-02-01";
            case "bedrock" -> "/model/" + model + "/invoke";
            default -> "/chat/completions";
        };
    }

    /**
     * 解析 OpenAI 格式的流式响应
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
                if (delta != null) {
                    StringBuilder sb = new StringBuilder();

                    // 支持 DeepSeek R1 的推理内容
                    if (delta.containsKey("reasoning_content")) {
                        String reasoning = delta.getString("reasoning_content");
                        if (reasoning != null) {
                            sb.append(reasoning);
                        }
                    }

                    // 标准内容
                    if (delta.containsKey("content")) {
                        String content = delta.getString("content");
                        if (content != null) {
                            sb.append(content);
                        }
                    }
                    return sb.toString();
                }
            }
        } catch (Exception e) {
            log.warn("解析 OpenAI 响应失败: {}", e.getMessage());
        }
        return "";
    }

    /**
     * 对 API 错误信息进行脱敏处理
     */
    private String sanitizeApiError(int statusCode, String rawBody) {
        return switch (statusCode) {
            case 401 -> "API Key 无效或已过期，请在「模型配置」中检查您的 API Key";
            case 403 -> "API Key 权限不足，请确认是否已开通对应模型的访问权限";
            case 404 -> (rawBody != null && rawBody.contains("model"))
                    ? "请求的模型不存在或未开通，请检查模型配置"
                    : "API 接口不存在，请检查配置的 Base URL 是否正确";
            case 429 -> "API 请求过于频繁，请稍后再试";
            case 500, 502, 503 -> "AI 服务暂时不可用，请稍后再试";
            default -> "AI 服务调用失败 (错误码: " + statusCode + ")，请稍后重试或联系管理员";
        };
    }
}
