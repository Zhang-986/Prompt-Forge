package com.zzk.infrastructure.ai.skill.executors;

import com.zzk.infrastructure.ai.skill.core.SkillExecutor;
import com.zzk.infrastructure.ai.skill.core.SkillResult;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.Map;

/**
 * URL 内容读取 Skill
 * 
 * <p>
 * 读取指定 URL 的网页内容，提取正文文本。
 * 
 * @author zzk
 * @since 1.0.0
 */
@Slf4j
@Component("urlFetchExecutor")
public class UrlFetchSkill implements SkillExecutor {

    private final WebClient webClient;

    private static final Duration TIMEOUT = Duration.ofSeconds(15);
    private static final int MAX_CONTENT_LENGTH = 10000; // 限制返回内容长度

    public UrlFetchSkill(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(5 * 1024 * 1024)) // 5MB
                .build();
    }

    @Override
    public String getName() {
        return "url-fetch";
    }

    @Override
    public SkillResult execute(Map<String, Object> params, Map<String, Object> context) {
        String url = (String) params.get("url");
        if (url == null || url.isBlank()) {
            return SkillResult.error("URL 不能为空");
        }

        // 确保 URL 有协议
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            url = "https://" + url;
        }

        try {
            String content = fetchAndExtract(url);
            return SkillResult.success(content);
        } catch (Exception e) {
            log.error("[UrlFetchSkill] 读取 URL 失败: {}", e.getMessage(), e);
            return SkillResult.error("读取网页失败: " + e.getMessage());
        }
    }

    /**
     * 获取网页并提取正文
     */
    private String fetchAndExtract(String url) {
        log.info("[UrlFetchSkill] 读取 URL: {}", url);

        String html = webClient.get()
                .uri(url)
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                .retrieve()
                .bodyToMono(String.class)
                .timeout(TIMEOUT)
                .block();

        if (html == null || html.isBlank()) {
            return "无法获取网页内容";
        }

        // 使用 Jsoup 解析并提取正文
        Document doc = Jsoup.parse(html);

        // 移除脚本、样式等
        doc.select("script, style, nav, footer, header, aside, .sidebar, .menu, .advertisement").remove();

        // 获取标题
        String title = doc.title();

        // 获取正文内容
        String text = doc.body().text();

        // 清理多余空格
        text = text.replaceAll("\\s+", " ").trim();

        // 限制长度
        if (text.length() > MAX_CONTENT_LENGTH) {
            text = text.substring(0, MAX_CONTENT_LENGTH) + "\n\n... (内容已截断，共 " + text.length() + " 字符)";
        }

        StringBuilder result = new StringBuilder();
        result.append("# ").append(title).append("\n\n");
        result.append("**URL**: ").append(url).append("\n\n");
        result.append("## 网页内容\n\n");
        result.append(text);

        log.info("[UrlFetchSkill] 成功读取，内容长度: {} 字符", text.length());
        return result.toString();
    }
}
