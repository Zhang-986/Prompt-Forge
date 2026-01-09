package com.zzk.infrastructure.ai.tool;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import lombok.extern.slf4j.Slf4j;

/**
 * Web 搜索工具
 * 
 * <p>
 * 通过搜索引擎获取最新技术文档和 API 信息
 * 
 * @author zzk
 * @since 1.0.0
 */
@Slf4j
@Component
public class WebSearchTool {

    private final WebClient webClient;

    public WebSearchTool(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.build();
    }

    /**
     * 执行网络搜索获取最新技术信息
     * 
     * @param query 搜索关键词
     * @return 搜索结果摘要
     */
    @Tool(description = "搜索互联网获取最新技术文档、API 信息或框架版本。当用户询问最新版本、API 用法或需要实时信息时使用。")
    public String search(String query) {
        log.info("[WebSearchTool] 执行搜索: {}", query);

        // TODO: 集成实际搜索 API (Tavily/Brave/SerpAPI)
        // 目前返回模拟结果用于测试
        return """
                搜索结果摘要 (query: %s):
                1. Spring Boot 3.3 于 2024年5月发布，主要特性包括...
                2. 官方文档链接: https://docs.spring.io/spring-boot/...
                3. 推荐使用 Java 17 或 21...

                注意: 这是模拟搜索结果，请集成实际搜索 API。
                """.formatted(query);
    }
}
