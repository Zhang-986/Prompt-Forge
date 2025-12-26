package com.zzk.infrastructure.ai.router;

import com.zzk.infrastructure.ai.strategy.LlmGenerationStrategy;
import com.zzk.infrastructure.exception.BusinessException;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * LLM 策略路由器
 * 
 * <p>使用 Spring 自动注入实现策略路由，避免 if-else 硬编码
 * 
 * @author zzk
 * @since 1.0.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LlmStrategyRouter {

    /**
     * 所有 LLM 策略（Spring 自动注入）
     */
    private final List<LlmGenerationStrategy> strategies;

    /**
     * 策略映射表：modelId -> strategy
     */
    private Map<String, LlmGenerationStrategy> strategyMap;

    @PostConstruct
    public void init() {
        // 将策略列表转换为 Map，以 modelId 为 key
        strategyMap = strategies.stream()
                .collect(Collectors.toMap(
                        LlmGenerationStrategy::getModelId,
                        Function.identity()
                ));
        
        log.info("LLM 策略路由器初始化完成，已注册 {} 个策略: {}", 
                strategyMap.size(), strategyMap.keySet());
    }

    /**
     * 根据模型 ID 获取策略
     * 
     * @param modelId 模型标识（如 gpt-4, deepseek, claude）
     * @return 对应的策略
     * @throws BusinessException 如果策略不存在或未启用
     */
    public LlmGenerationStrategy getStrategy(String modelId) {
        LlmGenerationStrategy strategy = strategyMap.get(modelId);
        
        if (strategy == null) {
            throw new BusinessException("不支持的模型: " + modelId + 
                    "，可用模型: " + strategyMap.keySet());
        }
        
        if (!strategy.isEnabled()) {
            throw new BusinessException("模型 " + modelId + " 当前未启用");
        }
        
        return strategy;
    }

    /**
     * 获取所有已启用的策略
     * 
     * @return 已启用的策略列表
     */
    public List<LlmGenerationStrategy> getEnabledStrategies() {
        return strategies.stream()
                .filter(LlmGenerationStrategy::isEnabled)
                .collect(Collectors.toList());
    }

    /**
     * 获取所有已注册的模型 ID
     * 
     * @return 模型 ID 列表
     */
    public List<String> getAllModelIds() {
        return strategies.stream()
                .map(LlmGenerationStrategy::getModelId)
                .collect(Collectors.toList());
    }

    /**
     * 获取所有已启用的模型 ID
     * 
     * @return 已启用的模型 ID 列表
     */
    public List<String> getEnabledModelIds() {
        return strategies.stream()
                .filter(LlmGenerationStrategy::isEnabled)
                .map(LlmGenerationStrategy::getModelId)
                .collect(Collectors.toList());
    }

    /**
     * 检查模型是否存在
     */
    public boolean hasModel(String modelId) {
        return strategyMap.containsKey(modelId);
    }

    /**
     * 检查模型是否启用
     */
    public boolean isModelEnabled(String modelId) {
        LlmGenerationStrategy strategy = strategyMap.get(modelId);
        return strategy != null && strategy.isEnabled();
    }
}
