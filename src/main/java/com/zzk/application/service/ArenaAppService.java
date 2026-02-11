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
import com.zzk.infrastructure.persistence.mapper.AvailableModelMapper;
import com.zzk.infrastructure.persistence.po.AvailableModelPO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
    private final AvailableModelMapper availableModelMapper;

    @Qualifier("arenaExecutor")
    private final ThreadPoolExecutor arenaExecutor;

    // 模型测试结果日志文件路径
    private static final String MODEL_TEST_LOG_FILE = "model_test_results.log";
    private static final DateTimeFormatter LOG_TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * 记录模型测试结果到本地文件（仅记录 404 模型不存在的错误）
     */
    private void logModelTestResult(String modelId, boolean success, long latencyMs, String errorMessage) {
        // 只记录 404 错误（模型不存在）
        if (success || errorMessage == null || !errorMessage.contains("404")) {
            return;
        }

        try {
            String timestamp = LocalDateTime.now().format(LOG_TIME_FORMAT);
            String line = String.format("[%s] ❌ 404 MODEL_NOT_FOUND | %s | 耗时: %dms | 错误: %s%n",
                    timestamp, modelId, latencyMs, errorMessage);

            Path logPath = Paths.get(MODEL_TEST_LOG_FILE);
            Files.writeString(logPath, line, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            log.info("已记录不存在的模型到文件: {}", modelId);
        } catch (IOException e) {
            log.warn("写入模型测试日志失败: {}", e.getMessage());
        }
    }

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
                .createdAt(LocalDateTime.now(java.time.ZoneId.of("Asia/Shanghai")))
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
                            // 保存成功结果并记录到文件
                            long latencyMs = System.currentTimeMillis() - startTimes.get(modelId);
                            logModelTestResult(modelId, true, latencyMs, null);
                            saveArenaResult(sessionId, modelId, resultBuffers.get(modelId).toString(),
                                    startTimes.get(modelId), "SUCCESS", null);
                        } else {
                            log.warn("用户未配置模型: {}", provider);
                            sendErrorEvent(emitter, modelId, "您没有配置该模型的 API Key，请先在模型配置中添加");
                            long latencyMs = System.currentTimeMillis() - startTimes.get(modelId);
                            logModelTestResult(modelId, false, latencyMs, "用户未配置该模型的 API Key");
                            saveArenaResult(sessionId, modelId, null, startTimes.get(modelId),
                                    "FAILED", "用户未配置该模型的 API Key");
                        }
                    } catch (Exception e) {
                        log.error("模型调用失败: modelId={}, error={}", modelId, e.getMessage());
                        sendErrorEvent(emitter, modelId, e.getMessage());
                        long latencyMs = System.currentTimeMillis() - startTimes.get(modelId);
                        logModelTestResult(modelId, false, latencyMs, e.getMessage());
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

    // ======================== 纯 WebFlux 响应式版本 ========================

    /**
     * 竞技场 SSE 事件数据结构
     */
    public record ArenaEventData(
            String modelId,
            String type, // session, start, content, finish, error, complete
            String content,
            int sequence,
            boolean finished,
            Long sessionId) {
        public static ArenaEventData session(Long sessionId) {
            return new ArenaEventData(null, "session", null, 0, false, sessionId);
        }

        public static ArenaEventData start(String modelId) {
            return new ArenaEventData(modelId, "start", "", 0, false, null);
        }

        public static ArenaEventData content(String modelId, String content, int seq) {
            return new ArenaEventData(modelId, "content", content, seq, false, null);
        }

        public static ArenaEventData finish(String modelId, int seq) {
            return new ArenaEventData(modelId, "finish", "", seq, true, null);
        }

        public static ArenaEventData reasoning(String modelId, String content, int seq) {
            return new ArenaEventData(modelId, "reasoning", content, seq, false, null);
        }

        public static ArenaEventData error(String modelId, String errorMsg) {
            return new ArenaEventData(modelId, "error", errorMsg, 0, true, null);
        }

        public static ArenaEventData complete() {
            return new ArenaEventData(null, "complete", "所有模型已完成", 0, true, null);
        }
    }

    /**
     * 启动竞技场对比（纯 WebFlux 响应式版本）
     * 
     * <p>
     * 使用 Flux.merge 并行调用多个模型，完全非阻塞
     * 
     * @param promptVersionId Prompt 版本 ID
     * @param variables       变量替换 Map
     * @param modelIds        要对比的模型 ID 列表
     * @param userId          用户 ID
     * @return SSE 事件流
     */
    public Flux<ServerSentEvent<ArenaEventData>> competeReactive(
            Long promptVersionId,
            Map<String, Object> variables,
            List<String> modelIds,
            Long userId) {

        log.info("[Reactive] 启动竞技场对比: versionId={}, models={}, userId={}",
                promptVersionId, modelIds, userId);

        // 1. 获取 Prompt 版本
        PromptVersion version = versionRepository.findById(promptVersionId)
                .orElseThrow(() -> new BusinessException("Prompt 版本不存在: " + promptVersionId));

        // 2. 渲染 Prompt 模板
        String finalPrompt = arenaDomainService.renderPrompt(version.getContent(), variables);

        // 3. 获取用户配置
        Map<String, UserModelConfig> userConfigs = userConfigRepository.findEnabledByUserId(userId)
                .stream()
                .collect(Collectors.toMap(UserModelConfig::getProvider, c -> c));

        // 4. 保存竞技会话
        Long sessionId = saveArenaSession(promptVersionId, finalPrompt, variables, modelIds, userId);

        // 5. 用于收集每个模型的完整输出（保存到数据库用）
        Map<String, StringBuilder> resultBuffers = new ConcurrentHashMap<>();
        Map<String, Long> startTimes = new ConcurrentHashMap<>();

        // 6. 构建每个模型的响应流
        List<Flux<ServerSentEvent<ArenaEventData>>> modelFluxes = modelIds.stream()
                .map(modelId -> createModelFlux(modelId, finalPrompt, userConfigs,
                        sessionId, resultBuffers, startTimes))
                .toList();

        // 7. 先发送 Session ID，然后合并所有模型的流，最后发送完成事件
        Flux<ServerSentEvent<ArenaEventData>> sessionEvent = Flux.just(
                ServerSentEvent.<ArenaEventData>builder()
                        .event("message")
                        .data(ArenaEventData.session(sessionId))
                        .build());

        Flux<ServerSentEvent<ArenaEventData>> mergedModelFlux = Flux.merge(modelFluxes)
                .subscribeOn(Schedulers.boundedElastic());

        Flux<ServerSentEvent<ArenaEventData>> completeEvent = Flux.defer(() -> {
            arenaSessionRepository.updateStatus(sessionId, "COMPLETED");
            log.info("[Reactive] 竞技场对比完成: sessionId={}", sessionId);
            return Flux.just(
                    ServerSentEvent.<ArenaEventData>builder()
                            .event("complete")
                            .data(ArenaEventData.complete())
                            .build());
        });

        return Flux.concat(sessionEvent, mergedModelFlux, completeEvent)
                .doOnError(e -> {
                    log.error("[Reactive] 竞技场执行异常: {}", e.getMessage());
                    arenaSessionRepository.updateStatus(sessionId, "FAILED");
                });
    }

    /**
     * 保存竞技会话并返回 ID
     */
    private Long saveArenaSession(Long promptVersionId, String finalPrompt,
            Map<String, Object> variables, List<String> modelIds, Long userId) {
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
                .createdAt(LocalDateTime.now(java.time.ZoneId.of("Asia/Shanghai")))
                .build();
        arenaSessionRepository.save(session);
        log.info("[Reactive] 创建竞技会话: sessionId={}", session.getId());
        return session.getId();
    }

    /**
     * 为单个模型创建响应式流
     */
    private Flux<ServerSentEvent<ArenaEventData>> createModelFlux(
            String modelId,
            String finalPrompt,
            Map<String, UserModelConfig> userConfigs,
            Long sessionId,
            Map<String, StringBuilder> resultBuffers,
            Map<String, Long> startTimes) {

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
        if (userConfig == null) {
            // 用户未配置该模型
            log.warn("[Reactive] 用户未配置模型: {}", provider);
            saveArenaResult(sessionId, modelId, null, System.currentTimeMillis(),
                    "FAILED", "用户未配置该模型的 API Key");
            return Flux.just(
                    ServerSentEvent.<ArenaEventData>builder()
                            .event("error")
                            .data(ArenaEventData.error(modelId, "您没有配置该模型的 API Key"))
                            .build());
        }

        // 构建有效配置
        UserModelConfig effectiveConfig = userConfig;
        if (specificModel != null && !specificModel.isEmpty()) {
            effectiveConfig = UserModelConfig.builder()
                    .id(userConfig.getId())
                    .userId(userConfig.getUserId())
                    .provider(userConfig.getProvider())
                    .apiKey(userConfig.getApiKey())
                    .baseUrl(userConfig.getBaseUrl())
                    .modelName(specificModel)
                    .enabled(userConfig.getEnabled())
                    .availableModels(userConfig.getAvailableModels())
                    .build();
        }

        // 初始化缓冲区和计时
        StringBuilder buffer = new StringBuilder();
        resultBuffers.put(modelId, buffer);
        startTimes.put(modelId, System.currentTimeMillis());
        AtomicInteger sequence = new AtomicInteger(0);

        // 构建模型调用流
        Flux<ServerSentEvent<ArenaEventData>> startEvent = Flux.just(
                ServerSentEvent.<ArenaEventData>builder()
                        .event("message")
                        .data(ArenaEventData.start(modelId))
                        .build());

        final UserModelConfig finalConfig = effectiveConfig;
        Flux<ServerSentEvent<ArenaEventData>> contentFlux = dynamicLlmFactory
                .generateStreamChunks(finalConfig, finalPrompt)
                .publishOn(Schedulers.boundedElastic()) // 切换到弹性线程池处理后续操作
                .filter(chunk -> chunk.type() != com.zzk.infrastructure.ai.adapter.StreamChunk.ChunkType.DONE)
                .map(chunk -> {
                    // 仅将 CONTENT 类型内容追加到最终结果（REASONING 不计入最终输出）
                    if (chunk.type() == com.zzk.infrastructure.ai.adapter.StreamChunk.ChunkType.CONTENT) {
                        buffer.append(chunk.content());
                    }
                    // 根据 chunk 类型生成对应的 SSE 事件
                    ArenaEventData eventData = switch (chunk.type()) {
                        case REASONING ->
                            ArenaEventData.reasoning(modelId, chunk.content(), sequence.incrementAndGet());
                        case CONTENT -> ArenaEventData.content(modelId, chunk.content(), sequence.incrementAndGet());
                        case TOOL_CALL -> ArenaEventData.content(modelId, "[Tool Call: " + chunk.content() + "]",
                                sequence.incrementAndGet());
                        case DONE -> null; // 已被过滤
                    };
                    return ServerSentEvent.<ArenaEventData>builder()
                            .event("message")
                            .data(eventData)
                            .build();
                })
                .doOnComplete(() -> {
                    long latencyMs = System.currentTimeMillis() - startTimes.get(modelId);
                    log.info("[Reactive] 模型 {} 完成，耗时 {}ms", modelId, latencyMs);
                    // 异步保存，不阻塞响应式流
                    Mono.fromRunnable(() -> saveArenaResult(sessionId, modelId, buffer.toString(),
                            startTimes.get(modelId), "SUCCESS", null))
                            .subscribeOn(Schedulers.boundedElastic())
                            .subscribe();
                })
                .doOnError(e -> {
                    long latencyMs = System.currentTimeMillis() - startTimes.get(modelId);
                    log.error("[Reactive] 模型 {} 调用失败: {}", modelId, e.getMessage());
                    // 异步保存，不阻塞响应式流
                    Mono.fromRunnable(() -> saveArenaResult(sessionId, modelId, null,
                            startTimes.get(modelId), "FAILED", e.getMessage()))
                            .subscribeOn(Schedulers.boundedElastic())
                            .subscribe();
                })
                .onErrorResume(e -> Flux.just(
                        ServerSentEvent.<ArenaEventData>builder()
                                .event("error")
                                .data(ArenaEventData.error(modelId, e.getMessage()))
                                .build()));

        Flux<ServerSentEvent<ArenaEventData>> finishEvent = Flux.defer(() -> Flux.just(
                ServerSentEvent.<ArenaEventData>builder()
                        .event("message")
                        .data(ArenaEventData.finish(modelId, sequence.get()))
                        .build()));

        return Flux.concat(startEvent, contentFlux, finishEvent);
    }

    // ======================== 纯 WebFlux 响应式版本结束 ========================

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
                    .createdAt(LocalDateTime.now(java.time.ZoneId.of("Asia/Shanghai")))
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

        dynamicLlmFactory.generateStreamChunks(config, prompt)
                .subscribeOn(Schedulers.boundedElastic())
                .doOnNext(chunk -> {
                    // 根据 chunk 类型发送不同事件
                    String eventType = switch (chunk.type()) {
                        case REASONING -> "reasoning";
                        case CONTENT -> {
                            buffer.append(chunk.content()); // 仅 CONTENT 追加到最终结果
                            yield "content";
                        }
                        case TOOL_CALL -> "content"; // TOOL_CALL 作为 content 发送
                        case DONE -> null;
                    };
                    if (eventType != null && chunk.hasContent()) {
                        sendEvent(emitter, modelId, eventType, chunk.content(), sequence.incrementAndGet(), false);
                    }
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
    public List<AvailableModelInfo> getAvailableModels(Long userId) {
        // 1. 获取用户已启用配置中的 Provider ID 列表
        List<UserModelConfig> enabledConfigs = userConfigRepository.findEnabledByUserId(userId);
        if (enabledConfigs.isEmpty()) {
            return Collections.emptyList();
        }

        Set<String> enabledProviderIds = enabledConfigs.stream()
                .map(UserModelConfig::getProvider)
                .collect(Collectors.toSet());

        // 2. 从数据库查询这些 Provider 下的所有启用模型
        List<AvailableModelPO> availableModels = availableModelMapper.selectList(
                new LambdaQueryWrapper<AvailableModelPO>()
                        .in(AvailableModelPO::getProviderId, enabledProviderIds)
                        .eq(AvailableModelPO::getEnabled, 1)
                        .orderByAsc(AvailableModelPO::getSortOrder));

        // 3. 转换为 DTO
        return availableModels.stream()
                .map(po -> {
                    String providerId = po.getProviderId();
                    String modelCode = po.getModelId(); // 实际 API 调用的 ID
                    // 组合 ID: provider + ":" + modelCode
                    String fullId = providerId + ":" + modelCode;

                    return new AvailableModelInfo(
                            providerId,
                            fullId,
                            modelCode,
                            po.getDisplayName());
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
                        .createdAt(LocalDateTime.now(java.time.ZoneId.of("Asia/Shanghai")))
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
                .sorted((a, b) -> {
                    // Sort by Win Rate DESC, then Total Battles DESC
                    int rateCompare = Double.compare((Double) b.get("winRate"), (Double) a.get("winRate"));
                    if (rateCompare != 0)
                        return rateCompare;

                    // If win rates are equal, prefer more battles
                    return Long.compare((Long) b.get("total"), (Long) a.get("total"));
                })
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
        } catch (

        Exception e) {
            log.error("Failed to parse session data", e);
        }

        // Fetch promptId from version
        PromptVersion version = versionRepository.findById(session.getPromptVersionId())
                .orElse(null);
        Long promptId = version != null ? version.getPromptId() : null;

        return com.zzk.interfaces.dto.response.ArenaSessionDetailDTO.builder().id(session.getId()).promptId(promptId)
                .promptVersionId(session.getPromptVersionId()).finalPrompt(session.getFinalPrompt())
                .variables(variables).models(models).status(session.getStatus()).createdAt(session.getCreatedAt())
                .completedAt(session.getCompletedAt())
                .results(results.stream()
                        .map(r -> com.zzk.interfaces.dto.response.ArenaSessionDetailDTO.ResultDTO.builder()
                                .modelId(r.getModelId()).content(r.getContent()).error(r.getErrorMessage())
                                .latencyMs(r.getLatencyMs()).tokensUsed(r.getTokensUsed()).build())
                        .toList())
                .build();
    }
}
