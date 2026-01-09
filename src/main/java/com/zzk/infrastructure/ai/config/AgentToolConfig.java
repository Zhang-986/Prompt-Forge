package com.zzk.infrastructure.ai.config;

import com.zzk.infrastructure.ai.tool.WebSearchTool;
import com.zzk.infrastructure.ai.tool.GitRepoAnalyzerTool;
import com.zzk.infrastructure.ai.tool.PromptLibraryTool;
import com.zzk.infrastructure.ai.skill.StructurizationSkill;
import com.zzk.infrastructure.ai.skill.OptimizationSkill;
import com.zzk.infrastructure.ai.skill.EvaluationSkill;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbacks;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

/**
 * AI 工具配置类
 * 
 * <p>
 * 统一注册所有 Tools 和 Skills 供 Agent 使用
 * 
 * @author zzk
 * @since 1.0.0
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class AgentToolConfig {

    private final WebSearchTool webSearchTool;
    private final GitRepoAnalyzerTool gitRepoAnalyzerTool;
    private final PromptLibraryTool promptLibraryTool;
    private final StructurizationSkill structurizationSkill;
    private final OptimizationSkill optimizationSkill;
    private final EvaluationSkill evaluationSkill;

    /**
     * 注册所有可用工具
     */
    @Bean
    public List<ToolCallback> agentTools() {
        log.info("注册 Agent Tools 和 Skills...");

        List<ToolCallback> tools = Stream.of(
                // Tools (数据抓手)
                ToolCallbacks.from(webSearchTool),
                ToolCallbacks.from(gitRepoAnalyzerTool),
                ToolCallbacks.from(promptLibraryTool),
                // Skills (内部能力)
                ToolCallbacks.from(structurizationSkill),
                ToolCallbacks.from(optimizationSkill),
                ToolCallbacks.from(evaluationSkill)).flatMap(Arrays::stream).toList();

        log.info("已注册 {} 个工具/技能", tools.size());
        return tools;
    }
}
