package com.zzk.infrastructure.ai.client;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.zzk.application.service.AgentMonitorService;
import com.zzk.domain.model.entity.UserModelConfig;
import com.zzk.infrastructure.ai.skill.core.SkillExecutor;
import com.zzk.infrastructure.ai.skill.core.SkillMetadata;
import com.zzk.infrastructure.ai.skill.registry.SkillRegistry;
import com.zzk.infrastructure.ai.skill.core.SkillResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.LocalDateTime;
import java.util.*;

/**
 * 手搓 Function Calling 客户端
 * 
 * 支持所有 OpenAI 兼容厂商（包括智谱、DeepSeek 等国内厂商）。
 * 绕过 Spring AI 的路径限制，自行控制 API 路径和协议解析。
 * 
 * @author zzk
 * @since 1.0.0
 */
@Slf4j
@Component
public class FunctionCallingClient {

    private final WebClient.Builder webClientBuilder;
    private final SkillRegistry skillRegistry;
    private final AgentMonitorService monitorService;

    private static final int MAX_TOOL_CALL_ROUNDS = 5; // 防止无限循环

    public FunctionCallingClient(WebClient.Builder webClientBuilder,
            SkillRegistry skillRegistry,
            AgentMonitorService monitorService) {
        this.webClientBuilder = webClientBuilder;
        this.skillRegistry = skillRegistry;
        this.monitorService = monitorService;
    }

    /**
     * 带 Function Calling 的对话
     * 
     * @param config   用户模型配置
     * @param messages 消息列表
     * @param skills   选中的 Skill 列表
     * @param context  执行上下文
     * @return AI 最终回复
     */
    public String chat(UserModelConfig config,
            List<Map<String, Object>> messages,
            List<SkillMetadata> skills,
            Map<String, Object> context) {
        return chat(config, messages, skills, context, null);
    }

    /**
     * 带 Function Calling 的对话 (支持过程回调)
     * 
     * @param config        用户模型配置
     * @param messages      消息列表
     * @param skills        选中的 Skill 列表
     * @param context       执行上下文
     * @param eventCallback 过程事件回调 (nullable)
     * @return AI 最终回复
     */
    public String chat(UserModelConfig config,
            List<Map<String, Object>> messages,
            List<SkillMetadata> skills,
            Map<String, Object> context,
            java.util.function.Consumer<String> eventCallback) {
        return chatWithRounds(config, messages, skills, context, 0, eventCallback);
    }

    /**
     * 递归调用（带轮次限制）
     */
    private String chatWithRounds(UserModelConfig config,
            List<Map<String, Object>> messages,
            List<SkillMetadata> skills,
            Map<String, Object> context,
            int round,
            java.util.function.Consumer<String> eventCallback) {
        if (round >= MAX_TOOL_CALL_ROUNDS) {
            log.warn("[FunctionCallingClient] 达到最大工具调用轮次 {}", MAX_TOOL_CALL_ROUNDS);
            return "抱歉，工具调用次数过多，请尝试简化请求。";
        }

        String baseUrl = config.getEffectiveBaseUrl();
        String model = config.getEffectiveModelName();
        String chatPath = determineChatPath(config.getProvider());
        String fullUrl = normalizeUrl(baseUrl) + chatPath;

        log.info("[FunctionCallingClient] round={}, provider={}, model={}, skills={}",
                round, config.getProvider(), model, skills.stream().map(SkillMetadata::getName).toList());

        // 发送思考中事件
        if (eventCallback != null && round == 0) {
            eventCallback.accept("event: THOUGHT");
            eventCallback.accept("正在思考任务规划...");
        } else if (eventCallback != null) {
            eventCallback.accept("event: THOUGHT");
            eventCallback.accept("正在分析工具执行结果...");
        }

        // 构建请求体
        Map<String, Object> requestBody = new LinkedHashMap<>();
        requestBody.put("model", model);
        requestBody.put("messages", messages);

        // 只有有 Skill 时才添加 tools
        if (skills != null && !skills.isEmpty()) {
            requestBody.put("tools", skillRegistry.toOpenAiTools(skills));
            requestBody.put("tool_choice", "auto");
        }

        LocalDateTime startTime = LocalDateTime.now();

        // 发送请求
        String response;
        try {
            response = callApi(fullUrl, config.getApiKey(), config.getProvider(), requestBody);
        } catch (Exception e) {
            log.error("[FunctionCallingClient] API 调用失败: {}", e.getMessage(), e);
            monitorService.logLlmCall(getUserId(context), getSessionId(context), model,
                    startTime, "FAILURE", 0, 0, e.getMessage());
            return "AI 服务调用失败: " + e.getMessage();
        }

        // 解析响应
        JSONObject json;
        try {
            json = JSON.parseObject(response);
        } catch (Exception e) {
            log.error("[FunctionCallingClient] 解析响应失败: {}", response);
            monitorService.logLlmCall(getUserId(context), getSessionId(context), model,
                    startTime, "FAILURE", 0, 0, "Response Parse Error");
            return "解析 AI 响应失败";
        }

        // 记录 LLM 调用日志
        recordLlmUsage(context, model, startTime, json);

        JSONArray choices = json.getJSONArray("choices");
        if (choices == null || choices.isEmpty()) {
            log.error("[FunctionCallingClient] 响应中没有 choices: {}", response);
            return "AI 未返回有效内容";
        }

        JSONObject choice = choices.getJSONObject(0);
        JSONObject message = choice.getJSONObject("message");

        if (message == null) {
            log.error("[FunctionCallingClient] 响应中没有 message: {}", response);
            return "AI 未返回有效消息";
        }

        // 检查是否有 tool_calls
        JSONArray toolCalls = message.getJSONArray("tool_calls");
        if (toolCalls != null && !toolCalls.isEmpty()) {
            log.info("[FunctionCallingClient] 检测到 {} 个工具调用", toolCalls.size());

            // 构建新的消息列表
            List<Map<String, Object>> newMessages = new ArrayList<>(messages);

            // 添加 assistant 消息（包含 tool_calls）
            Map<String, Object> assistantMessage = new LinkedHashMap<>();
            assistantMessage.put("role", "assistant");
            assistantMessage.put("content", message.getString("content"));
            assistantMessage.put("tool_calls", toolCalls);
            newMessages.add(assistantMessage);

            // 执行每个工具调用
            for (int i = 0; i < toolCalls.size(); i++) {
                JSONObject toolCall = toolCalls.getJSONObject(i);
                String toolId = toolCall.getString("id");
                JSONObject function = toolCall.getJSONObject("function");
                String toolName = function.getString("name");
                String argsJson = function.getString("arguments");

                log.info("[FunctionCallingClient] 执行工具: {} ({})", toolName, toolId);

                // 发送工具调用事件（带中文显示名和参数预览）
                if (eventCallback != null) {
                    String displayName = getToolDisplayName(toolName);
                    String paramsPreview = getParamsPreview(argsJson);
                    eventCallback.accept("event: TOOL_START");
                    eventCallback.accept("{\"name\":\"" + toolName + "\",\"display\":\""
                            + displayName + "\",\"params\":" + escapeJson(paramsPreview) + "}");
                }

                // 解析参数
                Map<String, Object> args = new LinkedHashMap<>();
                if (argsJson != null && !argsJson.isBlank()) {
                    try {
                        args = JSON.parseObject(argsJson, Map.class);
                    } catch (Exception e) {
                        log.warn("[FunctionCallingClient] 解析工具参数失败: {}", argsJson);
                    }
                }

                // 执行技能
                LocalDateTime skillStart = LocalDateTime.now();
                SkillExecutor executor = skillRegistry.getExecutor(toolName);
                String toolResult;
                String status = "SUCCESS";
                String error = null;

                if (executor != null) {
                    try {
                        SkillResult result = executor.execute(args, context);
                        toolResult = result.getContent();
                        log.info("[FunctionCallingClient] 工具执行成功: {} -> {} chars",
                                toolName, toolResult != null ? toolResult.length() : 0);
                        if (!result.isSuccess()) {
                            status = "FAILURE";
                            error = result.getError();
                        }
                    } catch (Exception e) {
                        log.error("[FunctionCallingClient] 工具执行异常: {}", toolName, e);
                        toolResult = "工具执行失败: " + e.getMessage();
                        status = "FAILURE";
                        error = e.getMessage();
                    }
                } else {
                    log.warn("[FunctionCallingClient] 未找到执行器: {}", toolName);
                    toolResult = "工具未找到: " + toolName;
                    status = "FAILURE";
                    error = "Executor Not Found";
                }

                // 记录 Skill 执行日志
                monitorService.logSkillExecution(
                        getUserId(context), getSessionId(context), toolName,
                        skillStart, status, error, argsJson, toolResult);

                // 发送工具完成事件（带结果长度和摘要）
                if (eventCallback != null) {
                    String displayName = getToolDisplayName(toolName);
                    int resultLen = toolResult != null ? toolResult.length() : 0;
                    String preview = getResultPreview(toolResult);
                    eventCallback.accept("event: TOOL_END");
                    eventCallback.accept("{\"name\":\"" + toolName + "\",\"display\":\""
                            + displayName + "\",\"length\":" + resultLen + ",\"preview\":" + escapeJson(preview)
                            + "}");
                }

                // 添加 tool 结果消息
                Map<String, Object> toolMessage = new LinkedHashMap<>();
                toolMessage.put("role", "tool");
                toolMessage.put("tool_call_id", toolId);
                toolMessage.put("content", toolResult);
                newMessages.add(toolMessage);
            }

            // 递归调用，让 LLM 基于工具结果生成最终回复
            return chatWithRounds(config, newMessages, skills, context, round + 1, eventCallback);
        }

        // 没有 tool_calls，返回最终内容
        String content = message.getString("content");
        log.info("[FunctionCallingClient] 获取最终回复: {} chars", content != null ? content.length() : 0);
        return content != null ? content : "";
    }

    private void recordLlmUsage(Map<String, Object> context, String model, LocalDateTime startTime, JSONObject json) {
        try {
            JSONObject usage = json.getJSONObject("usage");
            int promptTokens = 0;
            int completionTokens = 0;

            if (usage != null) {
                promptTokens = usage.getIntValue("prompt_tokens");
                completionTokens = usage.getIntValue("completion_tokens");
            }

            monitorService.logLlmCall(
                    getUserId(context),
                    getSessionId(context),
                    model,
                    startTime,
                    "SUCCESS",
                    promptTokens,
                    completionTokens,
                    null);
        } catch (Exception e) {
            log.warn("[FunctionCallingClient] 记录 LLM Usage 失败: {}", e.getMessage());
        }
    }

    private Long getUserId(Map<String, Object> context) {
        if (context != null && context.get("userId") instanceof Long) {
            return (Long) context.get("userId");
        }
        return 0L;
    }

    private String getSessionId(Map<String, Object> context) {
        if (context != null && context.get("sessionId") instanceof String) {
            return (String) context.get("sessionId");
        }
        return "unknown";
    }

    /**
     * 根据厂商确定正确的 API 路径
     */
    private String determineChatPath(String provider) {
        if (provider == null) {
            return "/v1/chat/completions";
        }

        return switch (provider.toLowerCase()) {
            // 这些厂商的 baseUrl 已包含版本路径，直接用 /chat/completions
            case "zhipu", "hunyuan", "baichuan", "moonshot", "qwen", "aliyun",
                    "deepseek", "minimax", "stepfun", "spark", "yi", "sensenova",
                    "mistral", "perplexity", "groq", "cohere", "novita", "togetherai",
                    "ollama", "openrouter" ->
                "/chat/completions";
            // Azure 风格
            case "github" -> "/chat/completions?api-version=2024-12-01-preview";
            // 标准 OpenAI 格式
            default -> "/v1/chat/completions";
        };
    }

    /**
     * 标准化 URL（移除尾部斜杠）
     */
    private String normalizeUrl(String url) {
        if (url == null) {
            return "https://api.openai.com";
        }
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }

    /**
     * 调用 API
     */
    private String callApi(String url, String apiKey, String provider, Map<String, Object> body) {
        log.debug("[FunctionCallingClient] 调用 API: {}", url);

        WebClient.RequestBodySpec request = webClientBuilder.build()
                .post()
                .uri(url)
                .contentType(MediaType.APPLICATION_JSON);

        // 根据厂商设置认证头
        if ("anthropic".equalsIgnoreCase(provider) || "claude".equalsIgnoreCase(provider)) {
            request = (WebClient.RequestBodySpec) request
                    .header("x-api-key", apiKey)
                    .header("anthropic-version", "2023-06-01");
        } else {
            request = (WebClient.RequestBodySpec) request.header("Authorization", "Bearer " + apiKey);
        }

        String response = request
                .bodyValue(body)
                .retrieve()
                .bodyToMono(String.class)
                .block();

        log.debug("[FunctionCallingClient] API 响应: {} chars", response != null ? response.length() : 0);
        return response;
    }

    /**
     * 获取工具的中文显示名称
     */
    private String getToolDisplayName(String toolName) {
        return switch (toolName) {
            case "web-search" -> "网络搜索";
            case "url-fetch" -> "读取网页";
            case "code-analyzer" -> "代码分析";
            case "code-generator" -> "代码生成";
            case "evaluation" -> "Prompt质量评估";
            case "prompt-library" -> "Prompt库搜索";
            case "version-analyzer" -> "版本分析";
            case "structurization" -> "Prompt结构化";
            case "optimization" -> "Prompt优化";
            default -> toolName;
        };
    }

    /**
     * 获取参数预览（最多 50 字符）
     */
    private String getParamsPreview(String argsJson) {
        if (argsJson == null || argsJson.isBlank()) {
            return "{}";
        }
        if (argsJson.length() <= 100) {
            return argsJson;
        }
        return argsJson.substring(0, 97) + "...";
    }

    /**
     * 获取结果预览（最多 100 字符）
     */
    private String getResultPreview(String result) {
        if (result == null || result.isBlank()) {
            return "";
        }
        // 提取第一行有意义的内容
        String firstLine = result.split("\n")[0].trim();
        if (firstLine.length() <= 100) {
            return firstLine;
        }
        return firstLine.substring(0, 97) + "...";
    }

    /**
     * 转义 JSON 字符串中的特殊字符
     */
    private String escapeJson(String str) {
        if (str == null)
            return "\"\"";
        return "\"" + str
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t") + "\"";
    }
}
