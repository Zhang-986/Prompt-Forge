package com.zzk.infrastructure.ai.strategy;

import com.zzk.domain.model.entity.UserModelConfig;
import reactor.core.publisher.Flux;

/**
 * LLM 流式生成策略接口
 * 
 * <p>定义了不同 AI 厂商的流式生成能力标准接口，
 * 各厂商根据自己的 API 协议实现具体策略。
 * 
 * <p>设计模式：策略模式（Strategy Pattern）
 * - 封装算法族（不同厂商的 API 调用逻辑）
 * - 让算法独立于使用它的客户端变化
 * 
 * @author zzk
 * @since 1.0.0
 */
public interface LlmStreamStrategy {

    /**
     * 流式生成文本
     * 
     * @param config 用户模型配置（包含 API Key、Base URL、模型名等）
     * @param prompt 用户提示词
     * @return 响应式文本流（每个元素为一个文本片段）
     */
    Flux<String> generateStream(UserModelConfig config, String prompt);

    /**
     * 获取策略支持的厂商标识符
     * 
     * @return 厂商名称列表（如 ["openai"], ["google"], ["claude", "anthropic"]）
     */
    String[] getSupportedProviders();
}
