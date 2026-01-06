package com.zzk.interfaces.controller;

import com.zzk.application.service.PromptCoachAppService;
import com.zzk.domain.model.entity.PromptCoachSession;
import com.zzk.interfaces.dto.request.CoachChatRequest;
import com.zzk.interfaces.dto.request.ConfirmPromptRequest;
import com.zzk.interfaces.dto.request.StartCoachRequest;
import com.zzk.interfaces.dto.response.CoachSessionResponse;
import com.zzk.interfaces.dto.response.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import org.springframework.http.codec.ServerSentEvent;

/**
 * Prompt Coach 控制器
 * 
 * <p>
 * 多轮对话式 Prompt 引导优化 API
 * 
 * @author zzk
 * @since 1.0.0
 */
@Slf4j
@RestController
@RequestMapping("/api/prompt-coach")
@RequiredArgsConstructor
@Tag(name = "Prompt Coach", description = "多轮对话式 Prompt 引导优化")
public class PromptCoachController {

    private final PromptCoachAppService coachService;

    /**
     * 开始新的 Coach 会话
     */
    @PostMapping("/start")
    @Operation(summary = "开始 Coach 会话", description = "创建新的多轮对话会话，返回首轮 AI 引导")
    public Result<CoachSessionResponse> startSession(
            @Valid @RequestBody StartCoachRequest request,
            HttpServletRequest httpRequest) {

        Long userId = (Long) httpRequest.getAttribute("userId");
        log.info("开始 Coach 会话: userId={}, initialInput={}", userId, request.getInitialInput());

        PromptCoachSession session = coachService.startSession(
                userId,
                request.getInitialInput(),
                request.getProvider());

        return Result.success(CoachSessionResponse.from(session));
    }

    /**
     * 发送消息（SSE 流式返回）
     */
    @PostMapping(value = "/chat", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "发送消息", description = "发送用户消息，SSE 流式返回 AI 回复")
    public Flux<ServerSentEvent<String>> chat(@Valid @RequestBody CoachChatRequest request) {
        log.info("Coach 对话: sessionId={}, message={}", request.getSessionId(), request.getMessage());

        return coachService.chat(request.getSessionId(), request.getMessage())
                .map(chunk -> ServerSentEvent.<String>builder()
                        .data(chunk.replace("\n", "\\n"))
                        .build())
                .concatWith(Flux.just(ServerSentEvent.<String>builder()
                        .data("[DONE]")
                        .build()));
    }

    /**
     * 获取会话状态
     */
    @GetMapping("/session/{sessionId}")
    @Operation(summary = "获取会话状态", description = "获取当前会话的详细状态")
    public Result<CoachSessionResponse> getSession(@PathVariable String sessionId) {
        PromptCoachSession session = coachService.getSession(sessionId);
        return Result.success(CoachSessionResponse.from(session));
    }

    /**
     * 确认并保存 Prompt
     */
    @PostMapping("/confirm")
    @Operation(summary = "确认保存 Prompt", description = "确认当前生成的 Prompt 并保存到指定模板")
    public Result<String> confirmAndSave(
            @Valid @RequestBody ConfirmPromptRequest request,
            HttpServletRequest httpRequest) {

        Long userId = (Long) httpRequest.getAttribute("userId");
        log.info("确认保存 Prompt: userId={}, sessionId={}, templateId={}",
                userId, request.getSessionId(), request.getPromptTemplateId());

        String prompt = coachService.confirmAndSave(request.getSessionId(), request.getPromptTemplateId());
        return Result.success(prompt);
    }
}
