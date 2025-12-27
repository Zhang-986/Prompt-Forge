package com.zzk.application.service;

import com.zzk.domain.model.entity.UserModelConfig;
import com.zzk.domain.repository.UserModelConfigRepository;
import com.zzk.infrastructure.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;

/**
 * 用户模型配置应用服务
 * 
 * @author zzk
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserModelConfigAppService {

    private final UserModelConfigRepository configRepository;

    /**
     * 支持的提供商列表
     */
    public static final List<String> SUPPORTED_PROVIDERS = Arrays.asList(
            "google", "zhipu", "deepseek", "openai", "claude"
    );

    /**
     * 获取用户的所有配置
     */
    public List<UserModelConfig> getUserConfigs(Long userId) {
        return configRepository.findByUserId(userId);
    }

    /**
     * 获取用户启用的配置
     */
    public List<UserModelConfig> getEnabledConfigs(Long userId) {
        return configRepository.findEnabledByUserId(userId);
    }

    /**
     * 根据ID获取配置
     */
    public UserModelConfig getConfigById(Long id, Long userId) {
        UserModelConfig config = configRepository.findById(id)
                .orElseThrow(() -> new BusinessException("配置不存在"));
        if (!config.getUserId().equals(userId)) {
            throw new BusinessException("无权访问此配置");
        }
        return config;
    }

    /**
     * 创建配置
     */
    @Transactional
    public UserModelConfig createConfig(Long userId, String provider, String apiKey, 
                                         String baseUrl, String modelName) {
        // 验证提供商
        if (!SUPPORTED_PROVIDERS.contains(provider)) {
            throw new BusinessException("不支持的提供商: " + provider);
        }

        // 检查是否已存在
        if (configRepository.findByUserIdAndProvider(userId, provider).isPresent()) {
            throw new BusinessException("该提供商配置已存在，请直接编辑");
        }

        UserModelConfig config = UserModelConfig.builder()
                .userId(userId)
                .provider(provider)
                .apiKey(apiKey)
                .baseUrl(baseUrl)
                .modelName(modelName)
                .enabled(true)
                .build();

        configRepository.save(config);
        log.info("用户 {} 创建了 {} 配置", userId, provider);
        return config;
    }

    /**
     * 更新配置
     */
    @Transactional
    public UserModelConfig updateConfig(Long id, Long userId, String apiKey, 
                                         String baseUrl, String modelName, Boolean enabled) {
        UserModelConfig config = getConfigById(id, userId);

        if (apiKey != null && !apiKey.isBlank()) {
            config.setApiKey(apiKey);
        }
        config.setBaseUrl(baseUrl);
        config.setModelName(modelName);
        if (enabled != null) {
            config.setEnabled(enabled);
        }

        configRepository.update(config);
        log.info("用户 {} 更新了 {} 配置", userId, config.getProvider());
        return config;
    }

    /**
     * 删除配置
     */
    @Transactional
    public void deleteConfig(Long id, Long userId) {
        UserModelConfig config = getConfigById(id, userId);
        configRepository.deleteById(id);
        log.info("用户 {} 删除了 {} 配置", userId, config.getProvider());
    }

    /**
     * 切换启用状态
     */
    @Transactional
    public UserModelConfig toggleEnabled(Long id, Long userId) {
        UserModelConfig config = getConfigById(id, userId);
        config.setEnabled(!config.getEnabled());
        configRepository.update(config);
        log.info("用户 {} {} 了 {} 配置", userId, config.getEnabled() ? "启用" : "禁用", config.getProvider());
        return config;
    }

    /**
     * 获取支持的提供商及其模型列表
     */
    public List<ProviderInfo> getSupportedProviders() {
        return Arrays.asList(
                new ProviderInfo("google", "Google Gemini", 
                        "https://generativelanguage.googleapis.com", 
                        "gemini-2.0-flash",
                        Arrays.asList(
                                new ModelInfo("gemini-2.0-flash", "Gemini 2.0 Flash (推荐)", "快速、高效的通用模型"),
                                new ModelInfo("gemini-2.0-flash-lite", "Gemini 2.0 Flash-Lite", "轻量级高效模型"),
                                new ModelInfo("gemini-1.5-pro", "Gemini 1.5 Pro", "高性能推理模型"),
                                new ModelInfo("gemini-1.5-flash", "Gemini 1.5 Flash", "均衡型模型")
                        )),
                new ProviderInfo("zhipu", "智谱 GLM", 
                        "https://open.bigmodel.cn/api/paas/v4", 
                        "glm-4-flash",
                        Arrays.asList(
                                new ModelInfo("glm-4-flash", "GLM-4-Flash (推荐)", "免费快速模型"),
                                new ModelInfo("glm-4-flashx", "GLM-4-FlashX", "增强版 Flash"),
                                new ModelInfo("glm-4-air", "GLM-4-Air", "轻量级模型"),
                                new ModelInfo("glm-4-airx", "GLM-4-AirX", "增强版 Air"),
                                new ModelInfo("glm-4-plus", "GLM-4-Plus", "高性能旗舰模型"),
                                new ModelInfo("glm-4-long", "GLM-4-Long", "长文本模型"),
                                new ModelInfo("glm-4", "GLM-4", "标准版模型")
                        )),
                new ProviderInfo("deepseek", "DeepSeek", 
                        "https://api.deepseek.com", 
                        "deepseek-chat",
                        Arrays.asList(
                                new ModelInfo("deepseek-chat", "DeepSeek-Chat (推荐)", "通用对话模型 (V3)"),
                                new ModelInfo("deepseek-reasoner", "DeepSeek-Reasoner", "推理专用模型 (R1)")
                        )),
                new ProviderInfo("openai", "OpenAI GPT", 
                        "https://api.openai.com", 
                        "gpt-4o-mini",
                        Arrays.asList(
                                new ModelInfo("gpt-4o-mini", "GPT-4o Mini (推荐)", "高性价比多模态模型"),
                                new ModelInfo("gpt-4o", "GPT-4o", "旗舰多模态模型"),
                                new ModelInfo("gpt-4-turbo", "GPT-4 Turbo", "高性能版 GPT-4"),
                                new ModelInfo("gpt-4", "GPT-4", "标准版 GPT-4"),
                                new ModelInfo("gpt-3.5-turbo", "GPT-3.5 Turbo", "经济型模型"),
                                new ModelInfo("o1-mini", "o1-Mini", "推理模型")
                        )),
                new ProviderInfo("claude", "Anthropic Claude", 
                        "https://api.anthropic.com", 
                        "claude-3-5-sonnet-20241022",
                        Arrays.asList(
                                new ModelInfo("claude-3-5-sonnet-20241022", "Claude 3.5 Sonnet (推荐)", "均衡型旗舰模型"),
                                new ModelInfo("claude-3-5-haiku-20241022", "Claude 3.5 Haiku", "快速经济型模型"),
                                new ModelInfo("claude-3-opus-20240229", "Claude 3 Opus", "最强智能模型"),
                                new ModelInfo("claude-3-sonnet-20240229", "Claude 3 Sonnet", "均衡型模型"),
                                new ModelInfo("claude-3-haiku-20240307", "Claude 3 Haiku", "快速模型")
                        ))
        );
    }

    /**
     * 提供商信息
     */
    public record ProviderInfo(
            String id, 
            String name, 
            String defaultBaseUrl, 
            String defaultModel,
            List<ModelInfo> models
    ) {}

    /**
     * 模型信息
     */
    public record ModelInfo(
            String id,
            String name,
            String description
    ) {}
}
