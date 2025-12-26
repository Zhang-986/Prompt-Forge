package com.zzk.interfaces.controller;

import com.zzk.application.service.ArenaAppService;
import com.zzk.interfaces.dto.request.ArenaCompeteRequest;
import com.zzk.interfaces.dto.response.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

/**
 * 竞技场控制器
 * 
 * <p>提供多模型对比的 SSE 流式接口
 * 
 * @author zzk
 * @since 1.0.0
 */
@Slf4j
@RestController
@RequestMapping("/api/arena")
@RequiredArgsConstructor
@Tag(name = "竞技场", description = "多模型对比接口")
public class ArenaController {

    private final ArenaAppService arenaAppService;

    /**
     * 启动竞技场对比（SSE 流式接口）
     * 
     * <p>前端连接示例：
     * <pre>
     * const eventSource = new EventSource('/api/arena/compete');
     * eventSource.onmessage = (event) => {
     *     const data = JSON.parse(event.data);
     *     console.log(data.modelId, data.content);
     * };
     * </pre>
     */
    @PostMapping(value = "/compete", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "启动竞技场对比", description = "同时调用多个 AI 模型，SSE 流式返回结果")
    public SseEmitter compete(@Valid @RequestBody ArenaCompeteRequest request) {
        log.info("收到竞技场对比请求: versionId={}, models={}", 
                request.getPromptVersionId(), request.getModelIds());

        // TODO: 从 JWT 中获取用户 ID
        Long userId = 1L;

        return arenaAppService.compete(
                request.getPromptVersionId(),
                request.getVariables(),
                request.getModelIds(),
                userId
        );
    }

    /**
     * 获取可用的模型列表
     */
    @GetMapping("/models")
    @Operation(summary = "获取可用模型列表", description = "返回当前已启用的 AI 模型列表")
    public Result<List<String>> getAvailableModels() {
        return Result.success(arenaAppService.getAvailableModels());
    }
}
