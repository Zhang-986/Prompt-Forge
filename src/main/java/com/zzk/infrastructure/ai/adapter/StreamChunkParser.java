package com.zzk.infrastructure.ai.adapter;

import java.util.List;

/**
 * 流式响应解析器接口
 * 
 * <p>
 * 定义将厂商原始响应转换为统一 StreamChunk 的标准接口。
 * 各厂商实现各自的解析逻辑。
 * 
 * @author zzk
 * @since 1.0.0
 */
public interface StreamChunkParser {

    /**
     * 获取此解析器支持的厂商标识符列表
     * 
     * @return 厂商 ID 列表，如 ["openai", "deepseek"]
     */
    String[] getSupportedProviders();

    /**
     * 解析原始响应块为统一格式
     * 
     * @param rawChunk 厂商原始 SSE 数据块
     * @return 解析后的统一格式块列表（可能为多个，如同时包含 reasoning 和 content）
     */
    List<StreamChunk> parse(String rawChunk);
}
