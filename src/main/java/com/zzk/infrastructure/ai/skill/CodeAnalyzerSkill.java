package com.zzk.infrastructure.ai.skill;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * GitHub 仓库分析 Skill
 * 
 * <p>
 * 分析 GitHub 仓库的结构、技术栈和代码风格
 * 
 * @author zzk
 * @since 1.0.0
 */
@Slf4j
@Component("codeAnalyzerExecutor")
public class CodeAnalyzerSkill implements SkillExecutor {

    private final WebClient webClient;

    @Value("${github.token:}")
    private String githubToken;

    private static final String GITHUB_API_BASE = "https://api.github.com";
    private static final Duration TIMEOUT = Duration.ofSeconds(10);

    public CodeAnalyzerSkill(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder
                .baseUrl(GITHUB_API_BASE)
                .defaultHeader("Accept", "application/vnd.github.v3+json")
                .defaultHeader("User-Agent", "PromptForge-Agent")
                .build();
    }

    @Override
    public String getName() {
        return "code-analyzer";
    }

    @Override
    public SkillResult execute(Map<String, Object> params, Map<String, Object> context) {
        String repoUrl = (String) params.get("repoUrl");
        if (repoUrl == null || repoUrl.isBlank()) {
            return SkillResult.error("GitHub 仓库 URL 不能为空");
        }
        String result = analyzeRepo(repoUrl);
        return SkillResult.success(result);
    }

    /**
     * 分析 GitHub 仓库
     */
    public String analyzeRepo(String repoUrl) {
        log.info("[CodeAnalyzerSkill] 分析仓库: {}", repoUrl);

        String[] parts = parseGitHubUrl(repoUrl);
        if (parts == null) {
            return "无法解析 GitHub URL，请确保格式正确（如 https://github.com/owner/repo）";
        }

        String owner = parts[0];
        String repo = parts[1];

        try {
            JSONObject repoInfo = fetchRepoInfo(owner, repo);
            if (repoInfo == null) {
                return "无法获取仓库信息，请检查仓库是否存在或是否为私有仓库。";
            }

            Map<String, Long> languages = fetchLanguages(owner, repo);
            return formatAnalysisReport(owner, repo, repoInfo, languages);

        } catch (Exception e) {
            log.error("[CodeAnalyzerSkill] 分析失败: {}", e.getMessage(), e);
            return "分析仓库时发生错误: " + e.getMessage();
        }
    }

    private JSONObject fetchRepoInfo(String owner, String repo) {
        try {
            String response = webClient.get()
                    .uri("/repos/{owner}/{repo}", owner, repo)
                    .headers(h -> {
                        if (githubToken != null && !githubToken.isEmpty()) {
                            h.setBearerAuth(githubToken);
                        }
                    })
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(TIMEOUT)
                    .block();
            return JSON.parseObject(response);
        } catch (Exception e) {
            log.warn("获取仓库信息失败: {}/{} - {}", owner, repo, e.getMessage());
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Long> fetchLanguages(String owner, String repo) {
        try {
            String response = webClient.get()
                    .uri("/repos/{owner}/{repo}/languages", owner, repo)
                    .headers(h -> {
                        if (githubToken != null && !githubToken.isEmpty()) {
                            h.setBearerAuth(githubToken);
                        }
                    })
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(TIMEOUT)
                    .block();

            JSONObject json = JSON.parseObject(response);
            return json.entrySet().stream()
                    .collect(Collectors.toMap(
                            Map.Entry::getKey,
                            e -> ((Number) e.getValue()).longValue()));
        } catch (Exception e) {
            log.warn("获取语言统计失败: {}/{} - {}", owner, repo, e.getMessage());
            return Map.of();
        }
    }

    private String formatAnalysisReport(String owner, String repo, JSONObject info, Map<String, Long> languages) {
        StringBuilder sb = new StringBuilder();
        sb.append("# 仓库分析报告: ").append(owner).append("/").append(repo).append("\n\n");

        sb.append("## 📊 基本信息\n");
        sb.append("- **描述**: ").append(info.getString("description") != null ? info.getString("description") : "无")
                .append("\n");
        sb.append("- **Stars**: ⭐ ").append(info.getIntValue("stargazers_count")).append("\n");
        sb.append("- **Forks**: 🍴 ").append(info.getIntValue("forks_count")).append("\n");
        sb.append("- **License**: ")
                .append(info.getJSONObject("license") != null ? info.getJSONObject("license").getString("name") : "未指定")
                .append("\n\n");

        sb.append("## 💻 技术栈\n");
        if (languages.isEmpty()) {
            sb.append("- 无法获取语言信息\n");
        } else {
            long total = languages.values().stream().mapToLong(Long::longValue).sum();
            languages.entrySet().stream()
                    .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                    .limit(5)
                    .forEach(e -> {
                        double percentage = (e.getValue() * 100.0) / total;
                        sb.append(String.format("- **%s**: %.1f%%\n", e.getKey(), percentage));
                    });
        }

        sb.append("\n仓库链接: https://github.com/").append(owner).append("/").append(repo);
        return sb.toString();
    }

    private String[] parseGitHubUrl(String url) {
        try {
            if (url.contains("github.com")) {
                String path = url.replace("https://github.com/", "").replace("http://github.com/", "");
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
