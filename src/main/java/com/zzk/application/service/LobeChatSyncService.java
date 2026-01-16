package com.zzk.application.service;

import com.zzk.infrastructure.persistence.mapper.AvailableModelMapper;
import com.zzk.infrastructure.persistence.mapper.ModelProviderMapper;
import com.zzk.infrastructure.persistence.po.AvailableModelPO;
import com.zzk.infrastructure.persistence.po.ModelProviderPO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Lobe Chat 模型配置同步服务
 * 从 Lobe Chat GitHub 仓库同步模型配置
 * 
 * @author zzk
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LobeChatSyncService {

    private final ModelProviderMapper providerMapper;
    private final AvailableModelMapper modelMapper;
    private final RestTemplate restTemplate;

    private static final String LOBE_CHAT_RAW_URL = "https://raw.githubusercontent.com/lobehub/lobe-chat/main/src/config/modelProviders/";

    /**
     * 支持同步的 Provider 列表及其配置
     * 数据来源:
     * https://github.com/lobehub/lobe-chat/tree/main/src/config/modelProviders
     */
    private static final Map<String, ProviderConfig> SYNC_PROVIDERS = new LinkedHashMap<>();

    static {
        // 按优先级排序 - 主流厂商
        SYNC_PROVIDERS.put("openai", new ProviderConfig("OpenAI GPT", "https://api.openai.com/v1", "openai", 1));
        SYNC_PROVIDERS.put("anthropic",
                new ProviderConfig("Anthropic Claude", "https://api.anthropic.com", "anthropic", 2));
        SYNC_PROVIDERS.put("google",
                new ProviderConfig("Google Gemini", "https://generativelanguage.googleapis.com", "google", 3));
        SYNC_PROVIDERS.put("deepseek", new ProviderConfig("DeepSeek", "https://api.deepseek.com", "openai", 4));
        SYNC_PROVIDERS.put("zhipu", new ProviderConfig("智谱 GLM", "https://open.bigmodel.cn/api/paas/v4", "openai", 5));
        SYNC_PROVIDERS.put("qwen",
                new ProviderConfig("通义千问 (Qwen)", "https://dashscope.aliyuncs.com/compatible-mode/v1", "openai", 6));
        SYNC_PROVIDERS.put("moonshot", new ProviderConfig("Moonshot Kimi", "https://api.moonshot.cn/v1", "openai", 7));
        SYNC_PROVIDERS.put("hunyuan",
                new ProviderConfig("腾讯混元", "https://api.hunyuan.cloud.tencent.com/v1", "openai", 8));

        // 云服务厂商
        SYNC_PROVIDERS.put("azure",
                new ProviderConfig("Azure OpenAI", "https://{RESOURCE_NAME}.openai.azure.com", "openai", 10));
        SYNC_PROVIDERS.put("bedrock",
                new ProviderConfig("AWS Bedrock", "https://bedrock-runtime.{REGION}.amazonaws.com", "openai", 11));
        SYNC_PROVIDERS.put("cloudflare", new ProviderConfig("Cloudflare Workers AI",
                "https://api.cloudflare.com/client/v4/accounts/{ACCOUNT_ID}/ai/v1", "openai", 12));
        SYNC_PROVIDERS.put("github",
                new ProviderConfig("GitHub Models", "https://models.inference.ai.azure.com", "openai", 13));

        // 国内厂商
        SYNC_PROVIDERS.put("baichuan",
                new ProviderConfig("百川 Baichuan", "https://api.baichuan-ai.com/v1", "openai", 20));
        SYNC_PROVIDERS.put("minimax", new ProviderConfig("MiniMax", "https://api.minimax.chat/v1", "openai", 21));
        SYNC_PROVIDERS.put("stepfun", new ProviderConfig("阶跃星辰 Step", "https://api.stepfun.com/v1", "openai", 22));
        SYNC_PROVIDERS.put("sensenova",
                new ProviderConfig("商汤日日新", "https://api.sensenova.cn/compatible-mode/v1", "openai", 25));

        // 海外厂商
        SYNC_PROVIDERS.put("mistral", new ProviderConfig("Mistral AI", "https://api.mistral.ai/v1", "openai", 30));
        SYNC_PROVIDERS.put("perplexity", new ProviderConfig("Perplexity", "https://api.perplexity.ai", "openai", 31));
        SYNC_PROVIDERS.put("groq", new ProviderConfig("Groq", "https://api.groq.com/openai/v1", "openai", 32));
        SYNC_PROVIDERS.put("cohere", new ProviderConfig("Cohere", "https://api.cohere.ai/v1", "openai", 33));

        SYNC_PROVIDERS.put("novita", new ProviderConfig("Novita AI", "https://api.novita.ai/v3/openai", "openai", 36));
        SYNC_PROVIDERS.put("togetherai",
                new ProviderConfig("Together.ai", "https://api.together.xyz/v1", "openai", 37));

        // 本地/开源
        SYNC_PROVIDERS.put("ollama", new ProviderConfig("Ollama (本地)", "http://localhost:11434/v1", "openai", 50));
        SYNC_PROVIDERS.put("openrouter",
                new ProviderConfig("OpenRouter", "https://openrouter.ai/api/v1", "openai", 51));
    }

    /**
     * 从 Lobe Chat 同步所有 Provider 的模型配置
     * 
     * @return 同步结果统计
     */
    @Transactional
    public SyncResult syncAllProviders() {
        log.info("开始从 Lobe Chat 同步模型配置...");

        int successCount = 0;
        int failCount = 0;
        List<String> failedProviders = new ArrayList<>();

        for (Map.Entry<String, ProviderConfig> entry : SYNC_PROVIDERS.entrySet()) {
            String providerId = entry.getKey();
            ProviderConfig config = entry.getValue();

            try {
                syncSingleProvider(providerId, config);
                successCount++;
                log.info("同步 {} 成功", providerId);
            } catch (Exception e) {
                failCount++;
                failedProviders.add(providerId);
                log.error("同步 {} 失败: {}", providerId, e.getMessage());
            }
        }

        log.info("Lobe Chat 同步完成: 成功={}, 失败={}", successCount, failCount);
        return new SyncResult(successCount, failCount, failedProviders);
    }

    /**
     * 同步单个 Provider
     */
    @Transactional
    public void syncSingleProvider(String providerId, ProviderConfig config) {
        // 1. 获取 TypeScript 文件内容
        String url = LOBE_CHAT_RAW_URL + providerId + ".ts";
        String tsContent;
        try {
            tsContent = restTemplate.getForObject(url, String.class);
        } catch (Exception e) {
            throw new RuntimeException("无法获取 " + url + ": " + e.getMessage());
        }

        if (tsContent == null || tsContent.isEmpty()) {
            throw new RuntimeException("获取到的内容为空: " + url);
        }

        // 2. 解析模型列表
        List<ParsedModel> models = parseTypeScriptModels(tsContent);
        if (models.isEmpty()) {
            log.warn("{} 解析到 0 个模型，跳过", providerId);
            return;
        }

        // 3. 保存或更新 Provider
        ModelProviderPO providerPO = providerMapper.selectById(providerId);
        if (providerPO == null) {
            providerPO = new ModelProviderPO();
            providerPO.setId(providerId);
            providerPO.setName(config.displayName);
            providerPO.setDefaultBaseUrl(config.defaultBaseUrl);
            providerPO.setSdkType(config.sdkType);
            providerPO.setSortOrder(config.sortOrder);
            providerPO.setEnabled(1);
            providerPO.setSyncedAt(LocalDateTime.now());
            providerPO.setCreatedAt(LocalDateTime.now());
            providerPO.setUpdatedAt(LocalDateTime.now());
            providerMapper.insert(providerPO);
        } else {
            providerPO.setSyncedAt(LocalDateTime.now());
            providerMapper.updateById(providerPO);
        }

        // 4. 保存或更新模型
        int sortOrder = 0;
        for (ParsedModel model : models) {
            // 过滤：如果 modelId 与 providerId 相同（大小写不敏感），则跳过（避免占位模型）
            if (model.id.equalsIgnoreCase(providerId)) {
                log.info("跳过占位模型: provider={}, model={}", providerId, model.id);
                continue;
            }

            AvailableModelPO existingModel = modelMapper.findByProviderIdAndModelId(providerId, model.id);

            if (existingModel == null) {
                // 新增模型
                AvailableModelPO newModel = new AvailableModelPO();
                newModel.setProviderId(providerId);
                newModel.setModelId(model.id);
                newModel.setDisplayName(model.displayName);
                newModel.setDescription(model.description);
                newModel.setContextWindow(model.contextWindow);
                newModel.setSupportsVision(model.vision ? 1 : 0);
                newModel.setSupportsFunctionCall(model.functionCall ? 1 : 0);
                newModel.setEnabled(model.enabled ? 1 : 0);
                newModel.setSortOrder(sortOrder++);
                newModel.setSource("sync");
                newModel.setCreatedAt(LocalDateTime.now());
                newModel.setUpdatedAt(LocalDateTime.now());
                modelMapper.insert(newModel);
            } else {
                // 更新现有模型（保留用户的 enabled 设置）
                existingModel.setDisplayName(model.displayName);
                existingModel.setDescription(model.description);
                existingModel.setContextWindow(model.contextWindow);
                existingModel.setSupportsVision(model.vision ? 1 : 0);
                existingModel.setSupportsFunctionCall(model.functionCall ? 1 : 0);
                existingModel.setSource("sync");
                modelMapper.updateById(existingModel);
            }
        }

        log.info("Provider {} 同步完成: {} 个模型", providerId, models.size());
    }

    /**
     * 解析 TypeScript 文件中的模型定义
     */
    private List<ParsedModel> parseTypeScriptModels(String tsContent) {
        List<ParsedModel> models = new ArrayList<>();

        // 先找到 chatModels 数组区域
        int chatModelsStart = tsContent.indexOf("chatModels:");
        if (chatModelsStart == -1) {
            chatModelsStart = tsContent.indexOf("chatModels :");
        }
        if (chatModelsStart == -1) {
            log.warn("未找到 chatModels 字段");
            return models;
        }

        // 从 chatModels 开始
        String chatModelsSection = tsContent.substring(chatModelsStart);

        // 简化解析：逐行提取关键属性
        String[] lines = chatModelsSection.split("\n");
        ParsedModel currentModel = null;

        for (String line : lines) {
            line = line.trim();

            // 检测新模型块开始
            if (line.contains("id:") && line.contains("'")) {
                if (currentModel != null && currentModel.id != null) {
                    models.add(currentModel);
                }
                currentModel = new ParsedModel();

                // 提取 id
                String id = extractStringProperty(line, "id");
                if (id != null) {
                    currentModel.id = id;
                }
            }

            if (currentModel != null) {
                // 提取各个属性
                String displayName = extractStringProperty(line, "displayName");
                if (displayName != null)
                    currentModel.displayName = displayName;

                String description = extractStringProperty(line, "description");
                if (description != null)
                    currentModel.description = truncate(description, 500);

                Integer contextWindow = extractIntProperty(line, "contextWindowTokens");
                if (contextWindow != null)
                    currentModel.contextWindow = contextWindow;

                Boolean enabled = extractBoolProperty(line, "enabled");
                if (enabled != null)
                    currentModel.enabled = enabled;

                Boolean functionCall = extractBoolProperty(line, "functionCall");
                if (functionCall != null)
                    currentModel.functionCall = functionCall;

                Boolean vision = extractBoolProperty(line, "vision");
                if (vision != null)
                    currentModel.vision = vision;
            }
        }

        // 添加最后一个模型
        if (currentModel != null && currentModel.id != null) {
            models.add(currentModel);
        }

        // 过滤：只保留有 id 的模型
        models.removeIf(m -> m.id == null || m.id.isEmpty());

        // 如果 displayName 为空，使用 id
        for (ParsedModel model : models) {
            if (model.displayName == null || model.displayName.isEmpty()) {
                model.displayName = model.id;
            }
        }

        return models;
    }

    private String extractStringProperty(String line, String propName) {
        Pattern pattern = Pattern.compile(propName + ":\\s*['\"`]([^'\"`]*)['\"`]");
        Matcher matcher = pattern.matcher(line);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }

    private Integer extractIntProperty(String line, String propName) {
        Pattern pattern = Pattern.compile(propName + ":\\s*([\\d_]+)");
        Matcher matcher = pattern.matcher(line);
        if (matcher.find()) {
            try {
                String value = matcher.group(1).replace("_", "");
                return Integer.parseInt(value);
            } catch (NumberFormatException e) {
                // 忽略解析错误
            }
        }
        return null;
    }

    private Boolean extractBoolProperty(String line, String propName) {
        Pattern pattern = Pattern.compile(propName + ":\\s*(true|false)");
        Matcher matcher = pattern.matcher(line);
        if (matcher.find()) {
            return "true".equals(matcher.group(1));
        }
        return null;
    }

    private String truncate(String s, int maxLength) {
        if (s == null)
            return null;
        return s.length() > maxLength ? s.substring(0, maxLength) : s;
    }

    /**
     * Provider 配置
     */
    private record ProviderConfig(String displayName, String defaultBaseUrl, String sdkType, int sortOrder) {
    }

    /**
     * 解析出的模型信息
     */
    private static class ParsedModel {
        String id;
        String displayName;
        String description;
        Integer contextWindow;
        boolean enabled = true;
        boolean functionCall = false;
        boolean vision = false;
    }

    /**
     * 同步结果
     */
    public record SyncResult(int successCount, int failCount, List<String> failedProviders) {
    }
}
