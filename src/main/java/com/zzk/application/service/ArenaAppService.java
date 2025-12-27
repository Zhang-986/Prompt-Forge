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
 * <p>负责业务编排：
 * <ul>
 *   <li>解析 Prompt 模板，渲染变量</li>
 *   <li>并行调用多个 AI 模型（使用用户配置）</li>
 *   <li>SSE 流式推送结果</li>
 *   <li>保存竞技历史记录</li>
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

        // 3. 获取用户配置
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

        // 5. 跟踪完成状态
        AtomicInteger completedCount = new AtomicInteger(0);
        int totalModels = modelIds.size();
        Map<String, StringBuilder> resultBuffers = new ConcurrentHashMap<>();
        Map<String, Long> startTimes = new ConcurrentHashMap<>();

        // 6. 并行调用所有模型（只使用用户配置）
        List<CompletableFuture<Void>> futures = modelIds.stream()
                .map(modelId -> CompletableFuture.runAsync(() -> {
                    startTimes.put(modelId, System.currentTimeMillis());
                    try {
                        UserModelConfig userConfig = userConfigs.get(modelId);
                        if (userConfig != null) {
                            callModelWithUserConfig(emitter, modelId, finalPrompt, userConfig, 
                                    resultBuffers, completedCount, totalModels);
                            // 保存成功结果
                            saveArenaResult(sessionId, modelId, resultBuffers.get(modelId).toString(),
                                    startTimes.get(modelId), "SUCCESS", null);
                        } else {
                            log.warn("用户未配置模型: {}", modelId);
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
        if (content == null) return 0;
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
     * 获取用户可用的模型列表（只返回用户已配置的模型）
     */
    public List<String> getAvailableModels(Long userId) {
        return userConfigRepository.findEnabledByUserId(userId)
                .stream()
                .map(UserModelConfig::getProvider)
                .toList();
    }
}
