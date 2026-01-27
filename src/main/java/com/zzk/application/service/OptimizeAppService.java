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
        你是一位顶尖的 Prompt 工程师。你的任务是将用户的输入提示词进行改写和优化，使其成为高质量、结构清晰且效果极佳的提示。

        请严格遵循以下原则进行优化：
        1. 赋予一个明确的角色（Persona）
        2. 清晰定义任务（Task）与最终目标（Goal）
        3. 加入必要的约束条件（Constraints）或输出格式要求（Format requirements）
        4. 如有动态可替换的部分，适当使用 {{变量名}} 这样的占位符

        用户原始输入：
        %s

        只返回优化后的完整提示词内容，禁止包含任何开头介绍、结尾说明或其他多余文字。
        """;

    /**
     * 优化 Prompt
     * 
     * @param originalContent 原始 Prompt 内容
     * @param userId          用户 ID
     * @param modelId         可选的模型 ID（provider 名称），如 "zhipu"、"google" 等
     */
    public String optimize(String originalContent, Long userId, String modelId) {
        // 1. 获取用户可用的模型配置
        List<UserModelConfig> configs = userConfigRepository.findEnabledByUserId(userId);
        if (configs.isEmpty()) {
            throw new BusinessException("请先在设置中配置至少一个 AI 模型");
        }

        // 2. 选择模型：如果指定了 modelId 则使用指定的，否则自动选择
        // 2. 选择模型：如果指定了 modelId 则使用指定的，否则自动选择
        UserModelConfig selectedConfig;
        if (modelId != null && !modelId.isBlank()) {
            // 检查是否包含具体模型 (format: provider:model)
            String targetProvider;
            String targetModel = null;

            if (modelId.contains(":")) {
                String[] parts = modelId.split(":", 2);
                targetProvider = parts[0];
                targetModel = parts[1];
            } else {
                targetProvider = modelId;
            }

            String finalTargetProvider = targetProvider;
            UserModelConfig baseConfig = configs.stream()
                    .filter(c -> c.getProvider().equalsIgnoreCase(finalTargetProvider))
                    .findFirst()
                    .orElseThrow(() -> new BusinessException("未找到指定的模型配置: " + finalTargetProvider));

            // 如果指定了具体模型，需临时覆盖配置
            if (targetModel != null && !targetModel.isEmpty()) {
                selectedConfig = UserModelConfig.builder()
                        .id(baseConfig.getId())
                        .userId(baseConfig.getUserId())
                        .provider(baseConfig.getProvider())
                        .apiKey(baseConfig.getApiKey())
                        .baseUrl(baseConfig.getBaseUrl())
                        .modelName(targetModel)
                        .enabled(baseConfig.getEnabled())
                        .availableModels(baseConfig.getAvailableModels())
                        .build();
            } else {
                selectedConfig = baseConfig;
            }
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
                .filter(c -> c.getModelName().toLowerCase().contains(keyword)
                        || c.getProvider().equalsIgnoreCase(keyword))
                .findFirst();
    }
}
