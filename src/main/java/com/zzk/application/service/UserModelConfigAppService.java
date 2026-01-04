package com.zzk.application.service;

import com.zzk.domain.model.entity.UserModelConfig;
import com.zzk.domain.repository.UserModelConfigRepository;
import com.zzk.infrastructure.exception.BusinessException;
import com.zzk.infrastructure.ai.factory.DynamicLlmClientFactory;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
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
    private final DynamicLlmClientFactory llmClientFactory;

    /**
     * 支持的提供商列表（从 JSON 文件加载）
     */
    public static List<String> SUPPORTED_PROVIDERS = new ArrayList<>();
    
    /**
     * 缓存的提供商信息
     */
    private List<ProviderInfo> cachedProviders = new ArrayList<>();

    /**
     * 启动时从 JSON 文件加载模型配置
     */
    @PostConstruct
    public void loadProvidersFromJson() {
        try {
            ClassPathResource resource = new ClassPathResource("model-providers.json");
            InputStream is = resource.getInputStream();
            String json = new String(is.readAllBytes(), StandardCharsets.UTF_8);
            
            JSONObject root = JSON.parseObject(json);
            JSONArray providersArray = root.getJSONArray("providers");
            
            List<ProviderInfo> providers = new ArrayList<>();
            List<String> providerIds = new ArrayList<>();
            
            for (int i = 0; i < providersArray.size(); i++) {
                JSONObject p = providersArray.getJSONObject(i);
                String id = p.getString("id");
                String name = p.getString("name");
                String defaultBaseUrl = p.getString("defaultBaseUrl");
                String defaultModel = p.getString("defaultModel");
                
                JSONArray modelsArray = p.getJSONArray("models");
                List<ModelInfo> models = new ArrayList<>();
                for (int j = 0; j < modelsArray.size(); j++) {
                    JSONObject m = modelsArray.getJSONObject(j);
                    models.add(new ModelInfo(
                            m.getString("id"),
                            m.getString("name"),
                            m.getString("description")
                    ));
                }
                
                providers.add(new ProviderInfo(id, name, defaultBaseUrl, defaultModel, models));
                providerIds.add(id);
            }
            
            this.cachedProviders = providers;
            SUPPORTED_PROVIDERS = providerIds;
            log.info("成功加载 {} 个模型提供商配置", providers.size());
            
        } catch (IOException e) {
            log.error("加载 model-providers.json 失败，使用空配置", e);
        }
    }

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
     * 刷新可用模型列表
     */
    @Transactional
    public List<String> refreshAvailableModels(Long id, Long userId) {
        UserModelConfig config = getConfigById(id, userId);
        
        List<String> models = llmClientFactory.fetchAvailableModels(config);
        
        config.setAvailableModels(JSON.toJSONString(models));
        configRepository.update(config);
        log.info("用户 {} 刷新了 {} 配置的模型列表: {}", userId, config.getProvider(), models);
        
        return models;
    }

    /**
     * 获取支持的提供商及其模型列表（从 JSON 配置文件加载）
     */
    public List<ProviderInfo> getSupportedProviders() {
        return cachedProviders;
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
