package com.zzk.application.service;

import com.zzk.domain.model.entity.ArenaResult;
import com.zzk.domain.model.entity.ArenaSession;
import com.zzk.domain.model.entity.UserModelConfig;
import com.zzk.domain.model.entity.PromptVersion;
import com.zzk.domain.repository.ArenaResultRepository;
import com.zzk.domain.repository.ArenaSessionRepository;
import com.zzk.domain.repository.PromptVersionRepository;
import com.zzk.domain.repository.UserModelConfigRepository;
import com.zzk.domain.service.ArenaDomainService;
import com.zzk.infrastructure.ai.factory.DynamicLlmClientFactory;
import com.zzk.infrastructure.exception.BusinessException;
import com.zzk.infrastructure.persistence.mapper.ArenaVoteMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.scheduler.Schedulers;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * 竞技场应用服务
 * 
 * <p>
 * 负责业务编排：
 * <ul>
 * <li>解析 Prompt 模板，渲染变量</li>
 * <li>并行调用多个 AI 模型（使用用户配置）</li>
 * <li>SSE 流式推送结果</li>
 * <li>保存竞技历史记录</li>
 * </ul>
 * 
 * @author zzk
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ArenaAppService {

    private final ArenaDomainService arenaDomainService;
    private final PromptVersionRepository versionRepository;
    private final UserModelConfigRepository userConfigRepository;
    private final ArenaSessionRepository arenaSessionRepository;
    private final ArenaResultRepository arenaResultRepository;
    private final DynamicLlmClientFactory dynamicLlmFactory;
    private final ObjectMapper objectMapper;
    private final ArenaVoteMapper arenaVoteMapper;
    private final com.zzk.application.service.UserModelConfigAppService userModelConfigAppService;

    @Qualifier("arenaExecutor")
    private final ThreadPoolExecutor arenaExecutor;

    /**
     * 启动竞技场对比（SSE 流式）- 使用用户配置
     */
    public SseEmitter compete(Long promptVersionId, Map<String, Object> variables,
            List<String> modelIds, Long userId) {
        log.info("启动竞技场对比: versionId={}, models={}, userId={}", promptVersionId, modelIds, userId);

        // 1. 获取 Prompt 版本
        PromptVersion version = versionRepository.findById(promptVersionId)
                .orElseThrow(() -> new BusinessException("Prompt 版本不存在: " + promptVersionId));

        // 2. 渲染 Prompt 模板
        String finalPrompt = arenaDomainService.renderPrompt(version.getContent(), variables);
        log.debug("渲染后的 Prompt: {}", finalPrompt);

        // 3. 获取用户配置（按 provider 索引）
        Map<String, UserModelConfig> userConfigs = userConfigRepository.findEnabledByUserId(userId)
                .stream()
                .collect(Collectors.toMap(UserModelConfig::getProvider, c -> c));

        // ======================== 保存竞技会话 ========================
        String variablesJson = "{}";
        String modelsJson = "[]";
        try {
            variablesJson = objectMapper.writeValueAsString(variables);
            modelsJson = objectMapper.writeValueAsString(modelIds);
        } catch (Exception e) {
            log.warn("序列化变量/模型失败", e);
        }

        ArenaSession session = ArenaSession.builder()
                .promptVersionId(promptVersionId)
                .finalPrompt(finalPrompt)
                .variables(variablesJson)
                .models(modelsJson)
                .status("RUNNING")
                .creatorId(userId)
                .createdAt(LocalDateTime.now())
                .build();
        arenaSessionRepository.save(session);
        final Long sessionId = session.getId();
        log.info("创建竞技会话: sessionId={}", sessionId);
        // ================================================================

        // 4. 创建 SSE 发射器（超时时间 5 分钟）
        SseEmitter emitter = new SseEmitter(5 * 60 * 1000L);

        // 发送会话 ID
        try {
            emitter.send(SseEmitter.event()
                    .name("message")
                    .data("{\"type\":\"session\",\"sessionId\":" + sessionId + "}"));
        } catch (IOException e) {
            log.error("发送 Session ID 失败", e);
        }

        // 5. 跟踪完成状态
        AtomicInteger completedCount = new AtomicInteger(0);
        int totalModels = modelIds.size();
        Map<String, StringBuilder> resultBuffers = new ConcurrentHashMap<>();
        Map<String, Long> startTimes = new ConcurrentHashMap<>();

        // 6. 并行调用所有模型（支持 provider:modelName 格式）
        List<CompletableFuture<Void>> futures = modelIds.stream()
                .map(modelId -> CompletableFuture.runAsync(() -> {
                    startTimes.put(modelId, System.currentTimeMillis());
                    try {
                        // 解析 modelId 格式: provider:modelName 或纯 provider
                        String provider;
                        String specificModel = null;
                        if (modelId.contains(":")) {
                            String[] parts = modelId.split(":", 2);
                            provider = parts[0];
                            specificModel = parts[1];
                        } else {
                            provider = modelId;
                        }

                        UserModelConfig userConfig = userConfigs.get(provider);
                        if (userConfig != null) {
                            // 如果指定了具体模型，临时覆盖配置中的模型名
                            UserModelConfig effectiveConfig = userConfig;
                            if (specificModel != null && !specificModel.isEmpty()) {
                                effectiveConfig = UserModelConfig.builder()
                                        .id(userConfig.getId())
                                        .userId(userConfig.getUserId())
                                        .provider(userConfig.getProvider())
                                        .apiKey(userConfig.getApiKey())
                                        .baseUrl(userConfig.getBaseUrl())
                                        .modelName(specificModel) // 使用指定的模型
                                        .enabled(userConfig.getEnabled())
                                        .availableModels(userConfig.getAvailableModels())
                                        .build();
                            }
                            callModelWithUserConfig(emitter, modelId, finalPrompt, effectiveConfig,
                                    resultBuffers, completedCount, totalModels);
                            // 保存成功结果
                            saveArenaResult(sessionId, modelId, resultBuffers.get(modelId).toString(),
                                    startTimes.get(modelId), "SUCCESS", null);
                        } else {
                            log.warn("用户未配置模型: {}", provider);
                            sendErrorEvent(emitter, modelId, "您没有配置该模型的 API Key，请先在模型配置中添加");
                            saveArenaResult(sessionId, modelId, null, startTimes.get(modelId),
                                    "FAILED", "用户未配置该模型的 API Key");
                        }
                    } catch (Exception e) {
                        log.error("模型调用失败: modelId={}, error={}", modelId, e.getMessage());
                        sendErrorEvent(emitter, modelId, e.getMessage());
                        saveArenaResult(sessionId, modelId, null, startTimes.get(modelId),
                                "FAILED", e.getMessage());
                    }
                }, arenaExecutor))
                .toList();

        // 7. 等待所有模型完成
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                .whenComplete((v, t) -> {
                    if (t != null) {
                        log.error("竞技场执行异常: {}", t.getMessage());
                        arenaSessionRepository.updateStatus(sessionId, "FAILED");
                    } else {
                        arenaSessionRepository.updateStatus(sessionId, "COMPLETED");
                    }
                    try {
                        emitter.send(SseEmitter.event()
                                .name("complete")
                                .data("{\"message\": \"所有模型已完成\"}"));
                        emitter.complete();
                    } catch (IOException e) {
                        log.warn("发送完成事件失败: {}", e.getMessage());
                    }
                    log.info("竞技场对比完成: versionId={}, sessionId={}", promptVersionId, sessionId);
                });

        // 8. 设置超时和错误处理
        emitter.onTimeout(() -> {
            log.warn("SSE 连接超时: versionId={}", promptVersionId);
            arenaSessionRepository.updateStatus(sessionId, "FAILED");
            emitter.complete();
        });
        emitter.onError(e -> {
            log.error("SSE 连接错误: versionId={}, error={}", promptVersionId, e.getMessage());
        });

        return emitter;
    }

    /**
     * 保存竞技结果
     */
    private void saveArenaResult(Long sessionId, String modelId, String content,
            Long startTime, String status, String errorMessage) {
        try {
            int latencyMs = (int) (System.currentTimeMillis() - startTime);
            int tokensUsed = content != null ? estimateTokens(content) : 0;

            ArenaResult result = ArenaResult.builder()
                    .sessionId(sessionId)
                    .modelId(modelId)
                    .content(content)
                    .tokensUsed(tokensUsed)
                    .latencyMs(latencyMs)
                    .status(status)
                    .errorMessage(errorMessage)
                    .createdAt(LocalDateTime.now())
                    .build();
            arenaResultRepository.save(result);
            log.debug("保存竞技结果: sessionId={}, modelId={}", sessionId, modelId);
        } catch (Exception e) {
            log.error("保存竞技结果失败: sessionId={}, modelId={}, error={}", sessionId, modelId, e.getMessage());
        }
    }

    /**
     * 估算 token 数（简单按字符数估算）
     */
    private int estimateTokens(String content) {
        if (content == null)
            return 0;
        // 中文约 1.5 字符/token，英文约 4 字符/token，取平均
        return content.length() / 2;
    }

    /**
     * 使用用户配置调用模型
     */
    private void callModelWithUserConfig(SseEmitter emitter, String modelId, String prompt,
            UserModelConfig config,
            Map<String, StringBuilder> resultBuffers,
            AtomicInteger completedCount, int totalModels) {
        log.debug("使用用户配置调用模型: {}", modelId);

        StringBuilder buffer = new StringBuilder();
        resultBuffers.put(modelId, buffer);

        AtomicInteger sequence = new AtomicInteger(0);
        long startTime = System.currentTimeMillis();

        sendEvent(emitter, modelId, "start", "", sequence.get(), false);

        dynamicLlmFactory.generateStream(config, prompt)
                .subscribeOn(Schedulers.boundedElastic())
                .doOnNext(content -> {
                    buffer.append(content);
                    sendEvent(emitter, modelId, "content", content, sequence.incrementAndGet(), false);
                })
                .doOnComplete(() -> {
                    long latency = System.currentTimeMillis() - startTime;
                    log.info("模型 {} 完成，耗时 {}ms", modelId, latency);
                    sendEvent(emitter, modelId, "finish", "", sequence.get(), true);
                    completedCount.incrementAndGet();
                })
                .doOnError(e -> {
                    log.error("模型 {} 调用失败: {}", modelId, e.getMessage());
                    sendErrorEvent(emitter, modelId, e.getMessage());
                    completedCount.incrementAndGet();
                })
                .blockLast();
    }

    /**
     * 发送 SSE 事件
     */
    private void sendEvent(SseEmitter emitter, String modelId, String eventType,
            String content, int sequence, boolean finished) {
        try {
            String data = String.format(
                    "{\"modelId\":\"%s\",\"type\":\"%s\",\"content\":\"%s\",\"sequence\":%d,\"finished\":%s}",
                    modelId, eventType, escapeJson(content), sequence, finished);

            emitter.send(SseEmitter.event()
                    .name("message")
                    .data(data));
        } catch (IOException e) {
            log.warn("发送 SSE 事件失败: modelId={}, error={}", modelId, e.getMessage());
        }
    }

    /**
     * 发送错误事件
     */
    private void sendErrorEvent(SseEmitter emitter, String modelId, String errorMessage) {
        try {
            String data = String.format(
                    "{\"modelId\":\"%s\",\"type\":\"error\",\"content\":\"%s\",\"finished\":true}",
                    modelId, escapeJson(errorMessage));

            emitter.send(SseEmitter.event()
                    .name("error")
                    .data(data));
        } catch (IOException e) {
            log.warn("发送错误事件失败: modelId={}", modelId);
        }
    }

    /**
     * 转义 JSON 特殊字符
     */
    private String escapeJson(String text) {
        if (text == null) {
            return "";
        }
        return text.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    /**
     * 可用模型信息 DTO
     */
    public record AvailableModelInfo(
            String provider, // 提供商 ID，如 "cloudflare"
            String modelId, // 完整标识，如 "cloudflare:@cf/meta/llama-3.3-70b-instruct-fp8-fast"
            String modelName, // 原始模型名，如 "@cf/meta/llama-3.3-70b-instruct-fp8-fast"
            String displayName // 显示名，如 "Cloudflare - Llama 3.3 70B"
    ) {
    }

    /**
     * 获取用户可用的模型列表（返回详细模型信息）
     */
    /**
     * 获取用户可用的模型列表（返回详细模型信息）
     */
    public List<AvailableModelInfo> getAvailableModels(Long userId) {
        List<UserModelConfig> enabledConfigs = userConfigRepository.findEnabledByUserId(userId);
        List<com.zzk.application.service.UserModelConfigAppService.ProviderInfo> supportedProviders = userModelConfigAppService
                .getSupportedProviders();

        return enabledConfigs.stream()
                .flatMap(config -> {
                    String providerId = config.getProvider();
                    // 查找该提供商的所有支持模型
                    return supportedProviders.stream()
                            .filter(p -> p.id().equals(providerId))
                            .findFirst()
                            .map(providerInfo -> providerInfo.models().stream()
                                    .map(modelInfo -> {
                                        String modelId = providerId + ":" + modelInfo.id();
                                        // 显示名称格式：Provider - ModelName (e.g., OpenAI - GPT-4o)
                                        String displayName = providerInfo.name() + " - " + modelInfo.name();
                                        return new AvailableModelInfo(providerId, modelId, modelInfo.id(), displayName);
                                    }))
                            .orElse(java.util.stream.Stream.of(
                                    // 如果没找到支持列表（可能是自定义或旧数据），至少返回当前配置的默认模型
                                    new AvailableModelInfo(providerId,
                                            providerId + ":" + config.getEffectiveModelName(),
                                            config.getEffectiveModelName(),
                                            getProviderDisplayName(providerId) + " - "
                                                    + getModelShortName(config.getEffectiveModelName()))));
                })
                .toList();
    }

    /**
     * 获取提供商显示名称
     */
    private String getProviderDisplayName(String provider) {
        return switch (provider) {
            case "google" -> "Google Gemini";
            case "zhipu" -> "智谱 GLM";
            case "deepseek" -> "DeepSeek";
            case "openai" -> "OpenAI";
            case "claude" -> "Claude";
            case "aliyun" -> "通义千问";
            case "moonshot" -> "Moonshot";
            case "cloudflare" -> "Cloudflare";
            case "modelscope" -> "ModelScope";
            default -> provider;
        };
    }

    /**
     * 获取模型简短名称（裁剪过长的模型 ID）
     */
    private String getModelShortName(String modelName) {
        if (modelName == null)
            return "Default";
        // 处理 Cloudflare 格式如 @cf/meta/llama-3.3-70b-instruct-fp8-fast
        if (modelName.startsWith("@")) {
            String[] parts = modelName.split("/");
            if (parts.length >= 3) {
                return parts[parts.length - 1]; // 取最后一部分
            }
        }
        // 处理过长的模型名
        if (modelName.length() > 30) {
            return modelName.substring(0, 27) + "...";
        }
        return modelName;
    }

    /**
     * 提交投票
     */
    public void submitVote(Long sessionId, String winnerModel, String loserModel, Long voterId) {
        log.info("提交投票: sessionId={}, winner={}, loser={}, voter={}",
                sessionId, winnerModel, loserModel, voterId);

        arenaVoteMapper.insert(
                com.zzk.infrastructure.persistence.po.ArenaVotePO.builder()
                        .sessionId(sessionId)
                        .winnerModel(winnerModel)
                        .loserModel(loserModel)
                        .voterId(voterId)
                        .createdAt(LocalDateTime.now())
                        .build());
    }

    /**
     * 获取模型排行榜
     */
    public List<Map<String, Object>> getLeaderboard() {
        // 获取胜负统计
        var wins = arenaVoteMapper.countWinsByModel();
        var losses = arenaVoteMapper.countLossesByModel();

        // 合并统计
        Map<String, Long> winMap = wins.stream()
                .collect(Collectors.toMap(
                        w -> w.getModelId(),
                        w -> w.getCount(),
                        (a, b) -> a));
        Map<String, Long> lossMap = losses.stream()
                .collect(Collectors.toMap(
                        l -> l.getModelId(),
                        l -> l.getCount(),
                        (a, b) -> a));

        // 合并所有模型 ID
        java.util.Set<String> allModels = new java.util.HashSet<>();
        allModels.addAll(winMap.keySet());
        allModels.addAll(lossMap.keySet());

        // 计算胜率并排序
        return allModels.stream()
                .map(modelId -> {
                    long w = winMap.getOrDefault(modelId, 0L);
                    long l = lossMap.getOrDefault(modelId, 0L);
                    long total = w + l;
                    double winRate = total > 0 ? (double) w / total * 100 : 0;

                    Map<String, Object> result = new java.util.HashMap<>();
                    result.put("modelId", modelId);
                    result.put("wins", w);
                    result.put("losses", l);
                    result.put("total", total);
                    result.put("winRate", Math.round(winRate * 10) / 10.0);
                    return result;
                })
                .sorted((a, b) -> Double.compare((Double) b.get("winRate"), (Double) a.get("winRate")))
                .toList();

    }

    /**
     * 获取用户投票历史
     */
    public com.baomidou.mybatisplus.extension.plugins.pagination.Page<com.zzk.interfaces.dto.response.ArenaVoteDTO> getUserVotes(
            Long userId, int page, int size) {
        com.baomidou.mybatisplus.extension.plugins.pagination.Page<com.zzk.interfaces.dto.response.ArenaVoteDTO> pageParam = new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(
                page, size);
        List<com.zzk.interfaces.dto.response.ArenaVoteDTO> records = arenaVoteMapper.selectVotesByUserId(pageParam,
                userId);

        // 尝试解析模型显示名称
        List<AvailableModelInfo> availableModels = getAvailableModels(userId);
        Map<String, String> modelNameMap = availableModels.stream()
                .collect(Collectors.toMap(AvailableModelInfo::modelId, AvailableModelInfo::displayName, (a, b) -> a));

        records.forEach(record -> {
            record.setWinnerModel(
                    modelNameMap.getOrDefault(record.getWinnerModel(), getModelShortName(record.getWinnerModel())));
            record.setLoserModel(
                    modelNameMap.getOrDefault(record.getLoserModel(), getModelShortName(record.getLoserModel())));
            // 截断过长的 Prompt
            if (record.getPrompt() != null && record.getPrompt().length() > 50) {
                record.setPrompt(record.getPrompt().substring(0, 50) + "...");
            }
        });

        return pageParam.setRecords(records);
    }

    /**
     * 获取竞技详情
     */
    public com.zzk.interfaces.dto.response.ArenaSessionDetailDTO getSessionDetail(Long sessionId) {
        com.zzk.domain.model.entity.ArenaSession session = arenaSessionRepository.findById(sessionId)
                .orElseThrow(() -> new BusinessException("Session not found"));

        List<com.zzk.domain.model.entity.ArenaResult> results = arenaResultRepository.findBySessionId(sessionId);

        // Convert JSON strings to Map/List
        Map<String, Object> variables = new java.util.HashMap<>();
        List<String> models = new java.util.ArrayList<>();
        try {
            if (session.getVariables() != null) {
                variables = objectMapper.readValue(session.getVariables(),
                        new com.fasterxml.jackson.core.type.TypeReference<Map<String, Object>>() {
                        });
            }
            if (session.getModels() != null) {
                models = objectMapper.readValue(session.getModels(),
                        new com.fasterxml.jackson.core.type.TypeReference<List<String>>() {
                        });
            }
        } catch (Exception e) {
            log.error("Failed to parse session data", e);
        }

        // Fetch promptId from version
        PromptVersion version = versionRepository.findById(session.getPromptVersionId())
                .orElse(null);
        Long promptId = version != null ? version.getPromptId() : null;

        return com.zzk.interfaces.dto.response.ArenaSessionDetailDTO.builder()
                .id(session.getId())
                .promptId(promptId)
                .promptVersionId(session.getPromptVersionId())
                .finalPrompt(session.getFinalPrompt())
                .variables(variables)
                .models(models)
                .status(session.getStatus())
                .createdAt(session.getCreatedAt())
                .completedAt(session.getCompletedAt())
                .results(results.stream()
                        .map(r -> com.zzk.interfaces.dto.response.ArenaSessionDetailDTO.ResultDTO.builder()
                                .modelId(r.getModelId())
                                .content(r.getContent())
                                .error(r.getErrorMessage())
                                .latencyMs(r.getLatencyMs())
                                .tokensUsed(r.getTokensUsed())
                                .build())
                        .toList())
                .build();
    }
}
