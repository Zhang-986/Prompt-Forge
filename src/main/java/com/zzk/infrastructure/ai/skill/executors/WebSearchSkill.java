package com.zzk.infrastructure.ai.skill.executors;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.zzk.infrastructure.ai.skill.core.SkillExecutor;
import com.zzk.infrastructure.ai.skill.core.SkillResult;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.util.Map;

/**
 * Web 搜索 Skill
 * 
 * <p>
 * 通过 Tavily Search API 获取最新技术文档和 API 信息
 * 
 * @author zzk
 * @since 1.0.0
 */
@Slf4j
@Component("webSearchExecutor")
public class WebSearchSkill implements SkillExecutor {

    private final WebClient webClient;

    @Value("${tavily.api-key:}")
    private String tavilyApiKey;

    private static final String TAVILY_API_BASE = "https://api.tavily.com";
    private static final Duration TIMEOUT = Duration.ofSeconds(15);
    private static final int MAX_RESULTS = 5;

    public WebSearchSkill(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder
                .baseUrl(TAVILY_API_BASE)
                .defaultHeader("Content-Type", "application/json")
                .build();
    }

    @Override
    public String getName() {
        return "web-search";
    }

    @Override
    public SkillResult execute(Map<String, Object> params, Map<String, Object> context) {
        String query = (String) params.get("query");
        if (query == null || query.isBlank()) {
            return SkillResult.error("搜索关键词不能为空");
        }
        String result = search(query);
        return SkillResult.success(result);
    }

    /**
     * 执行网络搜索获取最新技术信息
     */
    public String search(String query) {
        log.info("[WebSearchSkill] 执行搜索: {}", query);

        if (tavilyApiKey == null || tavilyApiKey.isEmpty()) {
            log.warn("[WebSearchSkill] Tavily API Key 未配置，返回降级提示");
            return generateFallbackResponse(query);
        }

        try {
            Map<String, Object> requestBody = Map.of(
                    "api_key", tavilyApiKey,
                    "query", query,
                    "search_depth", "basic",
                    "include_answer", true,
                    "include_raw_content", false,
                    "max_results", MAX_RESULTS);

            String response = webClient.post()
                    .uri("/search")
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(TIMEOUT)
                    .block();

            return parseSearchResults(query, response);

        } catch (Exception e) {
            log.error("[WebSearchSkill] 搜索失败: {}", e.getMessage(), e);
            return "搜索时发生错误: " + e.getMessage();
        }
    }

    private String parseSearchResults(String query, String response) {
        try {
            JSONObject json = JSON.parseObject(response);
            StringBuilder sb = new StringBuilder();
            sb.append("# 搜索结果: \"").append(query).append("\"\n\n");

            String answer = json.getString("answer");
            if (answer != null && !answer.isEmpty()) {
                sb.append("## 📝 摘要\n").append(answer).append("\n\n");
            }

            JSONArray results = json.getJSONArray("results");
            if (results != null && !results.isEmpty()) {
                sb.append("## 🔗 相关链接\n");
                for (int i = 0; i < Math.min(results.size(), MAX_RESULTS); i++) {
                    JSONObject result = results.getJSONObject(i);
                    String title = result.getString("title");
                    String url = result.getString("url");
                    String content = result.getString("content");

                    sb.append(i + 1).append(". **[").append(title).append("](").append(url).append(")**\n");
                    if (content != null && !content.isEmpty()) {
                        String snippet = content.length() > 200 ? content.substring(0, 200) + "..." : content;
                        sb.append("   ").append(snippet).append("\n");
                    }
                    sb.append("\n");
                }
            }

            sb.append("---\n💡 以上信息来自互联网搜索，请根据实际需求进行验证。");
            return sb.toString();

        } catch (Exception e) {
            log.warn("解析搜索结果失败: {}", e.getMessage());
            return "搜索完成，但解析结果时出现问题。原始数据:\n" + response;
        }
    }

    private String generateFallbackResponse(String query) {
        return """
                ⚠️ 网络搜索功能暂未启用

                **搜索关键词**: %s

                要启用此功能，请在配置文件中添加 Tavily API Key:
                ```yaml
                tavily:
                  api-key: your-api-key-here
                ```

                获取免费 API Key: https://tavily.com （每月 1000 次免费搜索）
                """.formatted(query);
    }
}
