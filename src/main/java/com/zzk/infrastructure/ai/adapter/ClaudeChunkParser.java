package com.zzk.infrastructure.ai.adapter;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Anthropic Claude 响应解析器
 * 
 * <p>
 * 解析 Claude 特有的 SSE 事件格式：
 * - content_block_delta: 内容增量
 * - thinking_delta: 深度思考（Extended Thinking）
 * 
 * @author zzk
 * @since 1.0.0
 */
@Slf4j
@Component
public class ClaudeChunkParser implements StreamChunkParser {

    @Override
    public String[] getSupportedProviders() {
        return new String[] { "claude", "anthropic" };
    }

    @Override
    public List<StreamChunk> parse(String rawChunk) {
        List<StreamChunk> result = new ArrayList<>();

        try {
            String jsonStr = rawChunk;
            if (rawChunk.startsWith("data:")) {
                jsonStr = rawChunk.substring(5).trim();
            }
            if (jsonStr.isEmpty()) {
                return result;
            }

            JSONObject json = JSON.parseObject(jsonStr);
            String eventType = json.getString("type");

            if ("content_block_delta".equals(eventType)) {
                JSONObject delta = json.getJSONObject("delta");
                if (delta != null) {
                    String deltaType = delta.getString("type");

                    switch (deltaType) {
                        case "thinking_delta" -> {
                            // Claude Extended Thinking 深度思考
                            String thinking = delta.getString("thinking");
                            if (thinking != null && !thinking.isEmpty()) {
                                result.add(StreamChunk.reasoning(thinking));
                            }
                        }
                        case "text_delta" -> {
                            // 正式回答
                            String text = delta.getString("text");
                            if (text != null && !text.isEmpty()) {
                                result.add(StreamChunk.content(text));
                            }
                        }
                        case "input_json_delta" -> {
                            // 工具调用参数
                            String partialJson = delta.getString("partial_json");
                            if (partialJson != null && !partialJson.isEmpty()) {
                                result.add(StreamChunk.toolCall(partialJson));
                            }
                        }
                    }
                }
            }

        } catch (Exception e) {
            log.warn("解析 Claude 响应失败: {}", e.getMessage());
        }

        return result;
    }
}
