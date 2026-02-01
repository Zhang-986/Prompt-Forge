package com.zzk.infrastructure.ai.factory;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.zzk.domain.model.entity.UserModelConfig;
import com.zzk.infrastructure.ai.strategy.LlmStreamStrategy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 动态 LLM 客户端工厂
 * 
 * <p>设计模式：工厂模式 + 策略模式
 * - 工厂模式：根据 provider 选择合适的策略
 * - 策略模式：不同厂商的 API 调用逻辑封装为独立策略类
 * 
 * <p>职责分离：
 * - Factory（本类）：负责策略选择和路由
 * - Strategy（策略类）：负责具体厂商的 API 调用实现
 * 
 * @author zzk
 * @since 1.0.0
 */
@Slf4j
@Component
public class DynamicLlmClientFactory {

    private final WebClient.Builder webClientBuilder;
    private final Map<String, LlmStreamStrategy> strategyMap = new ConcurrentHashMap<>();

    /**
     * 构造函数：自动注册所有策略实现
     * 
     * @param webClientBuilder WebClient 构建器
     * @param strategies Spring 容器中的所有策略实现（自动注入）
     */
    public DynamicLlmClientFactory(WebClient.Builder webClientBuilder, 
                                   List<LlmStreamStrategy> strategies) {
        this.webClientBuilder = webClientBuilder;
        
        // 注册所有策略到映射表
        strategies.forEach(strategy -> {
            for (String provider : strategy.getSupportedProviders()) {
                strategyMap.put(provider, strategy);
                log.debug("[DynamicLlmClientFactory] 注册策略: {} -> {}", 
                        provider, strategy.getClass().getSimpleName());
            }
        });
        
        log.info("[DynamicLlmClientFactory] 初始化完成，已注册 {} 个厂商策略", 
                strategyMap.size());
    }

    /**
     * 使用用户配置创建流式生成
     * 
     * <p>工厂方法：根据 provider 从策略映射中选择对应策略
     * 
     * @param config 用户模型配置
     * @param prompt 用户提示词
     * @return 响应式文本流
     */
    public Flux<String> generateStream(UserModelConfig config, String prompt) {
        String provider = config.getProvider();
        
        // 从策略映射中获取对应策略
        LlmStreamStrategy strategy = strategyMap.get(provider);
        
        if (strategy == null) {
            log.warn("未找到厂商 '{}' 的策略实现，尝试使用 OpenAI 兼容模式", provider);
            // 降级策略：使用 OpenAI 兼容策略（大多数厂商都兼容）
            strategy = strategyMap.get("openai");
        }
        
        if (strategy == null) {
            return Flux.error(new RuntimeException("无法找到合适的 AI 调用策略"));
        }
        
        // 委托给具体策略执行
        return strategy.generateStream(config, prompt);
    }

    /**
     * 获取可用模型列表 (仅限 OpenAI 兼容接口)
     * 
     * <p>注意：此方法未使用策略模式，因为只有少数厂商支持模型列表 API
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
