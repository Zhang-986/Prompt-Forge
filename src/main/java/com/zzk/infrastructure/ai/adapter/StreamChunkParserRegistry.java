package com.zzk.infrastructure.ai.adapter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 流式响应解析器注册表
 * 
 * <p>
 * Spring 自动收集所有 StreamChunkParser 实现，
 * 并根据厂商 ID 提供快速查找。
 * 
 * <p>
 * 设计模式：注册表模式 + 依赖注入
 * - 集合注入自动收集所有实现
 * - 运行时根据 provider 动态选择
 * 
 * @author zzk
 * @since 1.0.0
 */
@Slf4j
@Component
public class StreamChunkParserRegistry {

    private final Map<String, StreamChunkParser> parserMap;
    private final StreamChunkParser defaultParser;

    /**
     * Spring 自动注入所有 StreamChunkParser 实现
     */
    public StreamChunkParserRegistry(List<StreamChunkParser> parsers) {
        this.parserMap = new HashMap<>();

        for (StreamChunkParser parser : parsers) {
            for (String provider : parser.getSupportedProviders()) {
                parserMap.put(provider.toLowerCase(), parser);
                log.debug("注册解析器: {} -> {}", provider, parser.getClass().getSimpleName());
            }
        }

        // 默认使用 OpenAI 格式解析器
        this.defaultParser = parsers.stream()
                .filter(p -> p instanceof OpenAIChunkParser)
                .findFirst()
                .orElse(parsers.isEmpty() ? null : parsers.get(0));

        log.info("解析器注册完成，共 {} 个厂商映射", parserMap.size());
    }

    /**
     * 根据厂商 ID 获取对应的解析器
     * 
     * @param provider 厂商标识符（如 "openai", "claude"）
     * @return 对应的解析器，未找到时返回默认解析器
     */
    public StreamChunkParser getParser(String provider) {
        if (provider == null) {
            return defaultParser;
        }
        return parserMap.getOrDefault(provider.toLowerCase(), defaultParser);
    }

    /**
     * 检查是否支持指定厂商
     */
    public boolean supports(String provider) {
        return provider != null && parserMap.containsKey(provider.toLowerCase());
    }

    /**
     * 获取所有已注册的厂商列表
     */
    public List<String> getRegisteredProviders() {
        return List.copyOf(parserMap.keySet());
    }
}
