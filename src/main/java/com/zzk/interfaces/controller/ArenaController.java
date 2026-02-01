package com.zzk.interfaces.controller;

import com.zzk.application.service.ArenaAppService;
import com.zzk.interfaces.dto.request.ArenaCompeteRequest;
import com.zzk.interfaces.dto.request.ArenaVoteRequest;
import com.zzk.interfaces.dto.response.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Map;

/**
 * 竞技场控制器
 * 
 * <p>
 * 提供多模型对比的 SSE 流式接口
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
     * <p>
     * 前端连接示例：
     * 
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
    public SseEmitter compete(@RequestAttribute("userId") Long userId,
            @Valid @RequestBody ArenaCompeteRequest request) {
        log.info("收到竞技场对比请求: versionId={}, models={}, userId={}",
                request.getPromptVersionId(), request.getModelIds(), userId);

        return arenaAppService.compete(
                request.getPromptVersionId(),
                request.getVariables(),
                request.getModelIds(),
                userId);
    }

    /**
     * 启动竞技场对比（纯 WebFlux 响应式版本）
     * 
     * <p>使用 Flux.merge 并行调用多个模型，完全非阻塞，无需线程池
     * 
     * <p>前端连接示例：
     * <pre>
     * const eventSource = new EventSource('/api/arena/compete/reactive');
     * eventSource.onmessage = (event) => {
     *     const data = JSON.parse(event.data);
     *     console.log(data.modelId, data.content);
     * };
     * </pre>
     */
    @PostMapping(value = "/compete/reactive", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "启动竞技场对比(响应式)", description = "使用 WebFlux 并行调用多个 AI 模型，SSE 流式返回结果")
    public Flux<ServerSentEvent<ArenaAppService.ArenaEventData>> competeReactive(
            @RequestAttribute("userId") Long userId,
            @Valid @RequestBody ArenaCompeteRequest request) {
        log.info("[Reactive] 收到竞技场对比请求: versionId={}, models={}, userId={}",
                request.getPromptVersionId(), request.getModelIds(), userId);

        return arenaAppService.competeReactive(
                request.getPromptVersionId(),
                request.getVariables(),
                request.getModelIds(),
                userId);
    }

    /**
     * 获取用户可用的模型列表（返回详细模型信息）
     */
    @GetMapping("/models")
    @Operation(summary = "获取可用模型列表", description = "返回用户已配置的 AI 模型详细信息")
    public Result<List<ArenaAppService.AvailableModelInfo>> getAvailableModels(
            @RequestAttribute("userId") Long userId) {
        return Result.success(arenaAppService.getAvailableModels(userId));
    }

    /**
     * 提交投票
     */
    @PostMapping("/vote")
    @Operation(summary = "提交投票", description = "投票选择更好的模型输出")
    public Result<Void> submitVote(@RequestAttribute("userId") Long userId,
            @Valid @RequestBody ArenaVoteRequest request) {
        log.info("提交投票: winner={}, loser={}, userId={}",
                request.getWinnerModel(), request.getLoserModel(), userId);
        arenaAppService.submitVote(
                request.getSessionId(),
                request.getWinnerModel(),
                request.getLoserModel(),
                userId);
        return Result.success("投票成功", null);
    }

    /**
     * 获取模型排行榜
     */
    @GetMapping("/leaderboard")
    @Operation(summary = "获取模型排行榜", description = "返回所有模型的胜率排行")
    public Result<List<Map<String, Object>>> getLeaderboard() {
        return Result.success(arenaAppService.getLeaderboard());
    }

    /**
     * 获取用户投票历史
     */
    @GetMapping("/history")
    @Operation(summary = "获取用户投票历史", description = "分页查询用户的投票记录")
    public Result<com.baomidou.mybatisplus.extension.plugins.pagination.Page<com.zzk.interfaces.dto.response.ArenaVoteDTO>> getHistory(
            @RequestAttribute("userId") Long userId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        return Result.success(arenaAppService.getUserVotes(userId, page, size));
    }

    /**
     * 获取竞技详情
     */
    @GetMapping("/session/{sessionId}")
    @Operation(summary = "获取竞技详情", description = "返回竞技会话的详细信息，包括Prompt和模型输出")
    public Result<com.zzk.interfaces.dto.response.ArenaSessionDetailDTO> getSessionDetail(
            @PathVariable Long sessionId) {
        return Result.success(arenaAppService.getSessionDetail(sessionId));
    }
}
