package com.zzk.application.service;

import com.zzk.domain.model.entity.UserModelConfig;
import com.zzk.domain.repository.UserModelConfigRepository;
import com.zzk.infrastructure.ai.factory.DynamicLlmClientFactory;
import com.zzk.infrastructure.persistence.mapper.AvailableModelMapper;
import com.zzk.infrastructure.persistence.po.AvailableModelPO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeoutException;

/**
 * 模型验证服务
 * 
 * <p>
 * 批量测试模型可用性，自动禁用 404 的模型
 * </p>
 * 
 * @author zzk
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ModelValidationService {

    private final AvailableModelMapper availableModelMapper;
    private final UserModelConfigRepository userConfigRepository;
    private final DynamicLlmClientFactory dynamicLlmFactory;

    // 测试用的极简 Prompt
    private static final String TEST_PROMPT = "回复 1";
    // 单个模型测试超时时间
    private static final Duration TEST_TIMEOUT = Duration.ofSeconds(30);

    /**
     * 模型验证结果
     */
    public record ValidationResult(
            String modelId,
            String displayName,
            boolean success,
            String errorCode, // "404", "429", "403", "TIMEOUT", "SUCCESS", etc.
            String errorMessage,
            long latencyMs) {
    }

    /**
     * 批量验证结果
     */
    public record BatchValidationResult(
            String providerId,
            int totalModels,
            int successCount,
            int failedCount,
            int disabledCount, // 404 被禁用的数量
            List<ValidationResult> results) {
    }

    /**
     * 验证指定厂商的所有模型
     * 
     * @param providerId 厂商 ID (如 "google", "openai")
     * @param userId     用户 ID (用于获取 API Key 配置)
     * @return 批量验证结果
     */
    public BatchValidationResult validateProvider(String providerId, Long userId) {
        log.info("开始验证厂商 {} 的所有模型, userId={}", providerId, userId);

        // 1. 获取用户的厂商配置 (API Key)
        UserModelConfig userConfig = userConfigRepository.findEnabledByUserId(userId)
                .stream()
                .filter(c -> c.getProvider().equals(providerId))
                .findFirst()
                .orElse(null);

        if (userConfig == null) {
            log.warn("用户未配置厂商 {} 的 API Key", providerId);
            return new BatchValidationResult(providerId, 0, 0, 0, 0, List.of());
        }

        // 2. 获取该厂商的所有启用模型
        List<AvailableModelPO> models = availableModelMapper.findEnabledByProviderId(providerId);
        log.info("找到 {} 个已启用的模型", models.size());

        List<ValidationResult> results = new ArrayList<>();
        int successCount = 0;
        int failedCount = 0;
        int disabledCount = 0;

        // 3. 逐个测试模型
        for (AvailableModelPO model : models) {
            ValidationResult result = testSingleModel(userConfig, model);
            results.add(result);

            if (result.success()) {
                successCount++;
            } else {
                failedCount++;
                // 如果模型不存在，自动禁用
                // 404 = 标准不存在, 410 = 已下架, UNKNOWN_MODEL = GitHub 特殊格式
                if ("404".equals(result.errorCode())
                        || "410".equals(result.errorCode())
                        || "UNKNOWN_MODEL".equals(result.errorCode())) {
                    disableModel(model);
                    disabledCount++;
                }
            }

            // 每次测试间隔 500ms，避免触发限流
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }

        log.info("厂商 {} 验证完成: 总数={}, 成功={}, 失败={}, 已禁用={}",
                providerId, models.size(), successCount, failedCount, disabledCount);

        return new BatchValidationResult(providerId, models.size(), successCount, failedCount, disabledCount, results);
    }

    /**
     * 测试单个模型
     */
    private ValidationResult testSingleModel(UserModelConfig baseConfig, AvailableModelPO model) {
        String fullModelId = model.getProviderId() + ":" + model.getModelId();
        log.info("测试模型: {}", fullModelId);

        long startTime = System.currentTimeMillis();

        try {
            // 创建临时配置，使用要测试的模型
            UserModelConfig testConfig = UserModelConfig.builder()
                    .id(baseConfig.getId())
                    .userId(baseConfig.getUserId())
                    .provider(baseConfig.getProvider())
                    .apiKey(baseConfig.getApiKey())
                    .baseUrl(baseConfig.getBaseUrl())
                    .modelName(model.getModelId()) // 使用具体模型 ID
                    .enabled(baseConfig.getEnabled())
                    .build();

            // 调用模型并等待结果
            String response = dynamicLlmFactory.generateStream(testConfig, TEST_PROMPT)
                    .timeout(TEST_TIMEOUT)
                    .collectList()
                    .map(chunks -> String.join("", chunks))
                    .block();

            long latency = System.currentTimeMillis() - startTime;
            log.info("模型 {} 测试成功, 耗时 {}ms", fullModelId, latency);

            return new ValidationResult(fullModelId, model.getDisplayName(), true, "SUCCESS", null, latency);

        } catch (Exception e) {
            long latency = System.currentTimeMillis() - startTime;
            String errorMessage = e.getMessage();
            String errorCode = parseErrorCode(errorMessage, e);

            log.warn("模型 {} 测试失败: {} ({}ms)", fullModelId, errorMessage, latency);

            return new ValidationResult(fullModelId, model.getDisplayName(), false, errorCode, errorMessage, latency);
        }
    }

    /**
     * 解析错误码
     */
    private String parseErrorCode(String message, Exception e) {
        if (message == null) {
            if (e instanceof TimeoutException) {
                return "TIMEOUT";
            }
            return "UNKNOWN";
        }
        if (message.contains("404"))
            return "404";
        if (message.contains("410"))
            return "410"; // GONE - 模型已下架
        // GitHub Models 特殊处理: 400 + unknown_model = 模型不存在
        if (message.contains("unknown_model") || message.contains("Unknown model"))
            return "UNKNOWN_MODEL";
        if (message.contains("429"))
            return "429";
        if (message.contains("403"))
            return "403";
        if (message.contains("401"))
            return "401";
        if (message.contains("500"))
            return "500";
        if (e.getCause() instanceof TimeoutException)
            return "TIMEOUT";
        return "ERROR";
    }

    /**
     * 禁用模型
     */
    private void disableModel(AvailableModelPO model) {
        log.info("禁用不存在的模型: {} / {}", model.getProviderId(), model.getModelId());
        model.setEnabled(0);
        availableModelMapper.updateById(model);
    }
}
