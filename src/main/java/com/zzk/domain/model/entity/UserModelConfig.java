package com.zzk.domain.model.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 用户模型配置实体
 * 
 * @author zzk
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserModelConfig {

    private Long id;
    
    private Long userId;
    
    /**
     * 提供商标识: google, zhipu, deepseek, openai, claude
     */
    private String provider;
    
    /**
     * API Key
     */
    private String apiKey;
    
    /**
     * 自定义 Base URL (可选)
     */
    private String baseUrl;
    
    /**
     * 模型名称 (如 gpt-4, glm-4-flash)
     */
    private String modelName;

    /**
     * 自动获取的可用模型列表 (JSON)
     */
    private String availableModels;
    
    /**
     * 是否启用
     */
    private Boolean enabled;
    
    private LocalDateTime createdAt;
    
    private LocalDateTime updatedAt;

    /**
     * 获取提供商显示名称
     */
    public String getProviderDisplayName() {
        return switch (provider) {
            case "google" -> "Google Gemini";
            case "zhipu" -> "智谱 GLM";
            case "deepseek" -> "DeepSeek";
            case "openai" -> "OpenAI GPT";
            case "claude" -> "Anthropic Claude";
            default -> provider;
        };
    }

    /**
     * 获取默认 Base URL
     */
    public String getEffectiveBaseUrl() {
        if (baseUrl != null && !baseUrl.isBlank()) {
            return baseUrl;
        }
        return switch (provider) {
            case "google" -> "https://generativelanguage.googleapis.com";
            case "zhipu" -> "https://open.bigmodel.cn/api/paas/v4";
            case "deepseek" -> "https://api.deepseek.com";
            case "openai" -> "https://api.openai.com";
            case "claude" -> "https://api.anthropic.com";
            default -> null;
        };
    }

    /**
     * 获取默认模型名称
     */
    public String getEffectiveModelName() {
        if (modelName != null && !modelName.isBlank()) {
            return modelName;
        }
        return switch (provider) {
            case "google" -> "gemini-2.0-flash";
            case "zhipu" -> "glm-4-flash";
            case "deepseek" -> "deepseek-chat";
            case "openai" -> "gpt-4";
            case "claude" -> "claude-3-opus-20240229";
            default -> null;
        };
    }
}
