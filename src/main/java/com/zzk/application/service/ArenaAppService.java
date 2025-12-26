package com.zzk.application.service;

import com.zzk.domain.model.entity.ArenaResult;
import com.zzk.domain.model.entity.ArenaSession;
import com.zzk.domain.model.entity.PromptVersion;
import com.zzk.domain.repository.PromptVersionRepository;
import com.zzk.domain.service.ArenaDomainService;
import com.zzk.infrastructure.ai.router.LlmStrategyRouter;
import com.zzk.infrastructure.ai.strategy.LlmGenerationStrategy;
import com.zzk.infrastructure.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 竞技场应用服务
 * 
 * <p>负责业务编排：
 * <ul>
 *   <li>解析 Prompt 模板，渲染变量</li>
 *   <li>并行调用多个 AI 模型</li>
 *   <li>SSE 流式推送结果</li>
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
    private final LlmStrategyRouter strategyRouter;
    private final PromptVersionRepository versionRepository;

    @Qualifier("arenaExecutor")
    private final ThreadPoolExecutor arenaExecutor;

    /**
     * 启动竞技场对比（SSE 流式）
     * 
     * <p>核心流程：
     * <ol>
     *   <li>获取 Prompt 版本内容</li>
     *   <li>渲染模板（变量替换）</li>
     *   <li>并行调用所有模型</li>
     *   <li>SSE 实时推送结果</li>
     * </ol>
     * 
     * @param promptVersionId Prompt 版本 ID
     * @param variables 变量值映射
     * @param modelIds 参与的模型 ID 列表
     * @param userId 用户 ID
     * @return SSE 发射器
     */
    public SseEmitter compete(Long promptVersionId, Map<String, Object> variables, 
                               List<String> modelIds, Long userId) {
        log.info("启动竞技场对比: versionId={}, models={}", promptVersionId, modelIds);

        // 1. 获取 Prompt 版本
        PromptVersion version = versionRepository.findById(promptVersionId)
                .orElseThrow(() -> new BusinessException("Prompt 版本不存在: " + promptVersionId));

        // 2. 渲染 Prompt 模板
        String finalPrompt = arenaDomainService.renderPrompt(version.getContent(), variables);
        log.debug("渲染后的 Prompt: {}", finalPrompt);

        // 3. 创建 SSE 发射器（超时时间 5 分钟）
        SseEmitter emitter = new SseEmitter(5 * 60 * 1000L);

        // 4. 跟踪完成状态
        AtomicInteger completedCount = new AtomicInteger(0);
        int totalModels = modelIds.size();
        Map<String, StringBuilder> resultBuffers = new ConcurrentHashMap<>();

        // 5. 并行调用所有模型
        List<CompletableFuture<Void>> futures = modelIds.stream()
                .map(modelId -> CompletableFuture.runAsync(() -> {
                    try {
                        callModelWithStream(emitter, modelId, finalPrompt, resultBuffers, 
                                completedCount, totalModels);
                    } catch (Exception e) {
                        log.error("模型调用失败: modelId={}, error={}", modelId, e.getMessage());
                        sendErrorEvent(emitter, modelId, e.getMessage());
                    }
                }, arenaExecutor))
                .toList();

        // 6. 等待所有模型完成
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                .whenComplete((v, t) -> {
                    if (t != null) {
                        log.error("竞技场执行异常: {}", t.getMessage());
                    }
                    try {
                        // 发送完成事件
                        emitter.send(SseEmitter.event()
                                .name("complete")
                                .data("{\"message\": \"所有模型已完成\"}"));
                        emitter.complete();
                    } catch (IOException e) {
                        log.warn("发送完成事件失败: {}", e.getMessage());
                    }
                    log.info("竞技场对比完成: versionId={}", promptVersionId);
                });

        // 7. 设置超时和错误处理
        emitter.onTimeout(() -> {
            log.warn("SSE 连接超时: versionId={}", promptVersionId);
            emitter.complete();
        });
        emitter.onError(e -> {
            log.error("SSE 连接错误: versionId={}, error={}", promptVersionId, e.getMessage());
        });

        return emitter;
    }

    /**
     * 调用模型并流式推送结果
     */
    private void callModelWithStream(SseEmitter emitter, String modelId, String prompt,
                                      Map<String, StringBuilder> resultBuffers,
                                      AtomicInteger completedCount, int totalModels) {
        log.debug("开始调用模型: {}", modelId);
        
        LlmGenerationStrategy strategy = strategyRouter.getStrategy(modelId);
        StringBuilder buffer = new StringBuilder();
        resultBuffers.put(modelId, buffer);
        
        AtomicInteger sequence = new AtomicInteger(0);
        long startTime = System.currentTimeMillis();

        // 发送开始事件
        sendEvent(emitter, modelId, "start", "", sequence.get(), false);

        // 流式调用模型
        strategy.generateStream(prompt)
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
                .blockLast(); // 阻塞等待完成
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
     * 获取可用的模型列表
     */
    public List<String> getAvailableModels() {
        return strategyRouter.getEnabledModelIds();
    }
}
