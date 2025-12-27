package com.zzk.application.service;

import com.zzk.domain.model.entity.UserModelConfig;
import com.zzk.domain.repository.UserModelConfigRepository;
import com.zzk.infrastructure.ai.factory.DynamicLlmClientFactory;
import com.zzk.infrastructure.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * Prompt 优化服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OptimizeAppService {

    private final DynamicLlmClientFactory dynamicLlmFactory;
    private final UserModelConfigRepository userConfigRepository;

    private static final String META_PROMPT_TEMPLATE = """
            You are an expert Prompt Engineer. Your task is to rewrite and optimize the user's input prompt to be high-quality, structured, and effective.
            
            Follow these guidelines:
            1. Assign a clear Role (Persona).
            2. Define the Task and Goal clearly.
            3. Add Constraints or Format requirements.
            4. Use placeholders like {{variable}} for dynamic parts if inferred.
            
            User Input:
            %s
            
            Return ONLY the optimized prompt content. Do not include introductory or concluding remarks.
            """;

    /**
     * 优化 Prompt
     * @param originalContent 原始 Prompt 内容
     * @param userId 用户 ID
     * @param modelId 可选的模型 ID（provider 名称），如 "zhipu"、"google" 等
     */
    public String optimize(String originalContent, Long userId, String modelId) {
        // 1. 获取用户可用的模型配置
        List<UserModelConfig> configs = userConfigRepository.findEnabledByUserId(userId);
        if (configs.isEmpty()) {
            throw new BusinessException("请先在设置中配置至少一个 AI 模型");
        }

        // 2. 选择模型：如果指定了 modelId 则使用指定的，否则自动选择
        UserModelConfig selectedConfig;
        if (modelId != null && !modelId.isBlank()) {
            selectedConfig = configs.stream()
                    .filter(c -> c.getProvider().equalsIgnoreCase(modelId))
                    .findFirst()
                    .orElseThrow(() -> new BusinessException("未找到指定的模型配置: " + modelId));
        } else {
            selectedConfig = selectBestModel(configs);
        }
        log.info("使用模型进行优化: {}", selectedConfig.getProvider());

        // 3. 构建 Prompt
        String metaPrompt = String.format(META_PROMPT_TEMPLATE, originalContent);

        // 4. 调用 AI (阻塞等待结果)
        try {
            StringBuilder result = new StringBuilder();
            dynamicLlmFactory.generateStream(selectedConfig, metaPrompt)
                    .toIterable()
                    .forEach(result::append);
            
            return result.toString().trim();
        } catch (Exception e) {
            log.error("Prompt 优化失败", e);
            throw new BusinessException("优化失败: " + e.getMessage());
        }
    }

    private UserModelConfig selectBestModel(List<UserModelConfig> configs) {
        // 优先级策略
        return findProvider(configs, "gpt-4")
                .or(() -> findProvider(configs, "claude"))
                .or(() -> findProvider(configs, "gemini"))
                .or(() -> findProvider(configs, "deepseek"))
                .or(() -> findProvider(configs, "zhipu"))
                .orElse(configs.get(0));
    }

    private Optional<UserModelConfig> findProvider(List<UserModelConfig> configs, String keyword) {
        return configs.stream()
                .filter(c -> c.getModelName().toLowerCase().contains(keyword) || c.getProvider().equalsIgnoreCase(keyword))
                .findFirst();
    }
}
