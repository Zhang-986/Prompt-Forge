package com.zzk.infrastructure.ai.adapter;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * OpenAI 兼容格式响应解析器
 * 
 * <p>
 * 支持所有遵循 OpenAI API 规范的厂商，包括：
 * - OpenAI (GPT-4, o1, o3)
 * - DeepSeek (R1 支持 reasoning_content)
 * - 智谱 AI, 通义千问, Moonshot 等
 * 
 * @author zzk
 * @since 1.0.0
 */
@Slf4j
@Component
public class OpenAIChunkParser implements StreamChunkParser {

    @Override
    public String[] getSupportedProviders() {
        return new String[] {
                "openai", "zhipu", "deepseek", "aliyun", "qwen", "moonshot",
                "cloudflare", "github", "hunyuan", "azure", "bedrock",
                "baichuan", "minimax", "stepfun", "yi", "sensenova",
                "mistral", "perplexity", "groq", "cohere", "novita",
                "togetherai", "ollama", "openrouter"
        };
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

            JSONObject json = JSON.parseObject(jsonStr);
            JSONArray choices = json.getJSONArray("choices");
            if (choices == null || choices.isEmpty()) {
                return result;
            }

            JSONObject delta = choices.getJSONObject(0).getJSONObject("delta");
            if (delta == null) {
                return result;
            }

            // 解析推理内容（DeepSeek R1, OpenAI o1/o3）
            // 注意：DeepSeek R1 可能使用 "reasoning_content" 或 "reasoning" 字段
            String reasoning = null;
            if (delta.containsKey("reasoning_content")) {
                reasoning = delta.getString("reasoning_content");
            } else if (delta.containsKey("reasoning")) {
                reasoning = delta.getString("reasoning");
            }
            if (reasoning != null && !reasoning.isEmpty()) {
                result.add(StreamChunk.reasoning(reasoning));
            }

            // 解析正式回答内容
            if (delta.containsKey("content")) {
                String content = delta.getString("content");
                if (content != null && !content.isEmpty()) {
                    result.add(StreamChunk.content(content));
                }
            }

            // 解析工具调用
            if (delta.containsKey("tool_calls")) {
                JSONArray toolCalls = delta.getJSONArray("tool_calls");
                if (toolCalls != null && !toolCalls.isEmpty()) {
                    result.add(StreamChunk.toolCall(toolCalls.toString()));
                }
            }

        } catch (Exception e) {
            log.warn("解析 OpenAI 响应失败: {}", e.getMessage());
        }

        return result;
    }
}
