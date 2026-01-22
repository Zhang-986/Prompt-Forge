package com.zzk.application.service;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.TypeReference;
import com.zzk.domain.model.entity.UserModelConfig;
import com.zzk.infrastructure.ai.factory.DynamicLlmClientFactory;
import com.zzk.infrastructure.ai.skill.core.SkillMetadata;
import com.zzk.infrastructure.ai.skill.registry.SkillRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Skill 智能推断服务
 * 
 * <p>
 * 使用 LLM 自主推断用户消息需要哪些 Skills，实现按需加载。
 * 这是 Agent 架构的核心组件，让 AI 自己决定使用什么工具。
 * 
 * @author zzk
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SkillSelectorService {

    private final DynamicLlmClientFactory llmFactory;
    private final SkillRegistry skillRegistry;

    /**
     * 使用 LLM 推断用户消息需要哪些 Skills
     * 
     * @param userMessage 用户输入
     * @param modelConfig 用于推断的模型配置
     * @return 推断出的 Skill 名称列表
     */
    public List<String> inferRequiredSkills(String userMessage, UserModelConfig modelConfig) {
        // 获取所有可用 Skills
        List<SkillMetadata> allSkills = skillRegistry.getAllEnabledSkills();
        if (allSkills.isEmpty()) {
            log.info("[SkillSelector] 无可用 Skills，跳过推断");
            return List.of();
        }

        // 构建推断 Prompt
        String systemPrompt = buildInferencePrompt(allSkills);

        log.info("[SkillSelector] 开始推断所需 Skills，用户消息长度: {}", userMessage.length());

        try {
            // 调用 LLM 进行推断（同步阻塞，快速完成）
            StringBuilder result = new StringBuilder();
            llmFactory.generateStream(modelConfig, systemPrompt + "\n\n用户消息: " + userMessage)
                    .toIterable()
                    .forEach(result::append);

            String response = result.toString().trim();
            log.debug("[SkillSelector] LLM 原始响应: {}", response);

            // 解析 JSON 数组
            List<String> inferredSkills = parseSkillList(response, allSkills);
            log.info("[SkillSelector] 推断结果: {}", inferredSkills);
            return inferredSkills;

        } catch (Exception e) {
            log.warn("[SkillSelector] 推断失败，返回空列表: {}", e.getMessage());
            return List.of();
        }
    }

    /**
     * 构建 Skill 推断的 System Prompt
     */
    private String buildInferencePrompt(List<SkillMetadata> skills) {
        String skillDescriptions = skills.stream()
                .map(s -> String.format("- %s: %s", s.getName(), s.getDescription()))
                .collect(Collectors.joining("\n"));

        return """
                你是一个意图分析器。根据用户消息，判断需要调用哪些工具来辅助回答。

                可用工具：
                %s

                判断规则：
                1. 如果用户给了普通网页 URL（非 GitHub），返回 ["url-fetch"]
                2. 如果用户给了 GitHub 仓库链接，返回 ["code-analyzer"]
                3. 如果用户询问最新信息、技术动态、框架版本等需要联网查询的内容，返回 ["web-search"]
                4. 如果用户要求评估 Prompt 质量，返回 ["prompt-evaluator"]
                5. 如果不需要任何工具，返回 []
                6. 可以同时返回多个工具，如 ["url-fetch", "web-search"]

                重要：
                - 只返回 JSON 数组格式，如：["tool1", "tool2"]
                - 不要有任何其他文字说明
                """.formatted(skillDescriptions);
    }

    /**
     * 解析 LLM 返回的 Skill 列表
     */
    private List<String> parseSkillList(String response, List<SkillMetadata> allSkills) {
        // 提取 JSON 数组部分
        String jsonPart = extractJsonArray(response);
        if (jsonPart == null || jsonPart.isBlank()) {
            return List.of();
        }

        try {
            List<String> parsed = JSON.parseObject(jsonPart, new TypeReference<List<String>>() {
            });
            if (parsed == null) {
                return List.of();
            }

            // 过滤掉不存在的 Skill 名称
            List<String> validSkillNames = allSkills.stream()
                    .map(SkillMetadata::getName)
                    .toList();

            return parsed.stream()
                    .filter(validSkillNames::contains)
                    .toList();

        } catch (Exception e) {
            log.warn("[SkillSelector] 解析 JSON 失败: {}", e.getMessage());
            return List.of();
        }
    }

    /**
     * 从响应中提取 JSON 数组
     */
    private String extractJsonArray(String response) {
        // 尝试直接解析
        String trimmed = response.trim();
        if (trimmed.startsWith("[") && trimmed.endsWith("]")) {
            return trimmed;
        }

        // 尝试提取 [...] 部分
        int start = response.indexOf('[');
        int end = response.lastIndexOf(']');
        if (start != -1 && end > start) {
            return response.substring(start, end + 1);
        }

        return null;
    }
}
