package com.zzk.infrastructure.ai.tool;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import lombok.extern.slf4j.Slf4j;

/**
 * GitHub 仓库分析工具
 * 
 * <p>
 * 分析 GitHub 仓库的结构、技术栈和代码风格
 * 
 * @author zzk
 * @since 1.0.0
 */
@Slf4j
@Component
public class GitRepoAnalyzerTool {

    private final WebClient webClient;

    public GitRepoAnalyzerTool(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.build();
    }

    /**
     * 分析 GitHub 仓库
     * 
     * @param repoUrl GitHub 仓库 URL
     * @return 仓库分析报告
     */
    @Tool(description = "分析 GitHub 仓库的项目结构、技术栈、依赖版本。当用户提供 GitHub 链接或想参考开源项目时使用。")
    public String analyzeRepo(String repoUrl) {
        log.info("[GitRepoAnalyzerTool] 分析仓库: {}", repoUrl);

        // 解析仓库信息
        String[] parts = parseGitHubUrl(repoUrl);
        if (parts == null) {
            return "无法解析 GitHub URL，请确保格式正确。";
        }

        String owner = parts[0];
        String repo = parts[1];

        // TODO: 调用 GitHub API 获取真实数据
        // 目前返回模拟结果
        return """
                仓库分析报告: %s/%s

                📂 项目结构:
                - 主要语言: Java (推测)
                - 构建工具: Maven/Gradle (推测)
                - 框架: Spring Boot (推测)

                📦 关键依赖:
                - spring-boot-starter-web
                - spring-boot-starter-data-jpa

                💡 代码风格:
                - 使用 Lombok 注解
                - 遵循 RESTful 规范

                注意: 这是模拟分析结果，请集成 GitHub API。
                """.formatted(owner, repo);
    }

    private String[] parseGitHubUrl(String url) {
        try {
            // 支持格式: https://github.com/owner/repo
            if (url.contains("github.com")) {
                String path = url.replace("https://github.com/", "")
                        .replace("http://github.com/", "");
                String[] parts = path.split("/");
                if (parts.length >= 2) {
                    return new String[] { parts[0], parts[1].replace(".git", "") };
                }
            }
        } catch (Exception e) {
            log.warn("解析 GitHub URL 失败: {}", url);
        }
        return null;
    }
}
