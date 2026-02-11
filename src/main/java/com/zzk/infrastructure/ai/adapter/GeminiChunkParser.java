package com.zzk.infrastructure.ai.adapter;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Google Gemini 响应解析器
 * 
 * <p>
 * 解析 Gemini 特有的响应结构：
 * - candidates[0].content.parts[0].text: 正式回答
 * - candidates[0].content.parts[0].thought: 深度思考（2.0 Flash Thinking）
 * 
 * @author zzk
 * @since 1.0.0
 */
@Slf4j
@Component
public class GeminiChunkParser implements StreamChunkParser {

    @Override
    public String[] getSupportedProviders() {
        return new String[] { "google", "gemini" };
    }

    @Override
    public List<StreamChunk> parse(String rawChunk) {
        List<StreamChunk> result = new ArrayList<>();

        try {
            String jsonStr = rawChunk;
            if (rawChunk.startsWith("data:")) {
                jsonStr = rawChunk.substring(5).trim();
            }
            if (jsonStr.isEmpty() || jsonStr.equals("[DONE]")) {
                return result;
            }

            // 处理 JSON 数组格式（Gemini 有时返回数组）
            if (jsonStr.startsWith("[")) {
                JSONArray array = JSON.parseArray(jsonStr);
                for (int i = 0; i < array.size(); i++) {
                    parseGeminiObject(array.getJSONObject(i), result);
                }
            } else if (jsonStr.startsWith("{")) {
                JSONObject json = JSON.parseObject(jsonStr);
                parseGeminiObject(json, result);
            }

        } catch (Exception e) {
            log.warn("解析 Gemini 响应失败: {}", e.getMessage());
        }

        return result;
    }

    private void parseGeminiObject(JSONObject json, List<StreamChunk> result) {
        JSONArray candidates = json.getJSONArray("candidates");
        if (candidates == null || candidates.isEmpty()) {
            return;
        }

        JSONObject candidate = candidates.getJSONObject(0);
        JSONObject content = candidate.getJSONObject("content");
        if (content == null) {
            return;
        }

        JSONArray parts = content.getJSONArray("parts");
        if (parts == null || parts.isEmpty()) {
            return;
        }

        for (int i = 0; i < parts.size(); i++) {
            JSONObject part = parts.getJSONObject(i);

            // 深度思考（Gemini 2.0 Flash Thinking）
            if (part.containsKey("thought")) {
                String thought = part.getString("thought");
                if (thought != null && !thought.isEmpty()) {
                    result.add(StreamChunk.reasoning(thought));
                }
            }

            // 正式回答
            if (part.containsKey("text")) {
                String text = part.getString("text");
                if (text != null && !text.isEmpty()) {
                    result.add(StreamChunk.content(text));
                }
            }

            // 函数调用
            if (part.containsKey("functionCall")) {
                JSONObject functionCall = part.getJSONObject("functionCall");
                if (functionCall != null) {
                    result.add(StreamChunk.toolCall(functionCall.toString()));
                }
            }
        }
    }
}
