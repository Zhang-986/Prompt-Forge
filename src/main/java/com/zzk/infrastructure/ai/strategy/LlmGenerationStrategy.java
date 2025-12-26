package com.zzk.infrastructure.ai.strategy;

import reactor.core.publisher.Flux;

/**
 * LLM 生成策略接口
 * 
 * <p>采用策略模式，支持多种 AI 模型的统一调用
 * 
 * @author zzk
 * @since 1.0.0
 */
public interface LlmGenerationStrategy {

    /**
     * 获取模型标识
     * 
     * @return 模型 ID（如 gpt-4, deepseek, claude）
     */
    String getModelId();

    /**
     * 获取模型显示名称
     * 
     * @return 显示名称
     */
    String getModelName();

    /**
     * 是否启用
     * 
     * @return 是否启用
     */
    boolean isEnabled();

    /**
     * 生成内容（流式）
     * 
     * @param prompt 提示词
     * @return 流式内容
     */
    Flux<String> generateStream(String prompt);

    /**
     * 生成内容（同步）
     * 
     * @param prompt 提示词
     * @return 生成的内容
     */
    String generate(String prompt);
}
