package com.zzk.interfaces.controller;

import com.zzk.application.service.AgentCoachService;
import com.zzk.domain.model.entity.PromptCoachSession;
import com.zzk.infrastructure.ai.skill.registry.SkillRegistry;
import com.zzk.interfaces.dto.request.CoachChatRequest;
import com.zzk.interfaces.dto.request.ConfirmPromptRequest;
import com.zzk.interfaces.dto.request.StartCoachRequest;
import com.zzk.interfaces.dto.response.CoachSessionResponse;
import com.zzk.interfaces.dto.response.Result;
import com.zzk.interfaces.dto.response.SkillInfoResponse;
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

import java.util.List;

/**
 * Prompt Coach 控制器
 * 
 * <p>
 * 多轮对话式 Prompt 引导优化 API
 * 支持普通模式和 Agent 模式（带工具调用能力）
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

        private final AgentCoachService agentCoachService;
        private final SkillRegistry skillRegistry;

        /**
         * 获取所有可用的 Skills（用于前端展示）
         */
        @GetMapping("/skills")
        @Operation(summary = "获取可用 Skills", description = "返回所有启用的 Skills 列表，供用户选择")
        public Result<List<SkillInfoResponse>> getAvailableSkills() {
                List<SkillInfoResponse> skills = skillRegistry.getAllEnabledSkills()
                                .stream()
                                .map(SkillInfoResponse::from)
                                .toList();
                log.info("返回 {} 个可用 Skills", skills.size());
                return Result.success(skills);
        }

        /**
         * 开始新的 Agent Coach 会话（带工具调用能力）
         * 替代原有的 /start 接口
         */
        @PostMapping("/start")
        @Operation(summary = "开始 Coach 会话", description = "创建新的多轮对话会话（已升级为 Agent 模式）")
        public Result<CoachSessionResponse> startSession(
                        @Valid @RequestBody StartCoachRequest request,
                        HttpServletRequest httpRequest) {

                Long userId = (Long) httpRequest.getAttribute("userId");
                log.info("开始 Agent Coach 会话: userId={}, initialInput={}",
                                userId, request.getInitialInput());

                // 统一路由到 Agent Service（Skills 由 AI 自动推断）
                PromptCoachSession session = agentCoachService.startAgentSession(
                                userId,
                                request.getInitialInput(),
                                request.getProvider());

                return Result.success(CoachSessionResponse.from(session));
        }

        /**
         * 开始新的 Agent Coach 会话（带工具调用能力）
         */
        @PostMapping("/agent/start")
        @Operation(summary = "开始 Agent Coach 会话", description = "创建带工具调用能力的智能会话")
        public Result<CoachSessionResponse> startAgentSession(
                        @Valid @RequestBody StartCoachRequest request,
                        HttpServletRequest httpRequest) {

                Long userId = (Long) httpRequest.getAttribute("userId");
                log.info("开始 Agent Coach 会话: userId={}, initialInput={}",
                                userId, request.getInitialInput());

                PromptCoachSession session = agentCoachService.startAgentSession(
                                userId,
                                request.getInitialInput(),
                                request.getProvider());

                return Result.success(CoachSessionResponse.from(session));
        }

        /**
         * 发送消息（SSE 流式返回）
         * 替代原有的 /chat 接口，统一使用 Agent 模式
         */
        @PostMapping(value = "/chat", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
        @Operation(summary = "发送消息", description = "发送用户消息，SSE 流式返回 AI 回复（Agent 模式）")
        public Flux<ServerSentEvent<String>> chat(@Valid @RequestBody CoachChatRequest request) {
                log.info("Coach 对话: sessionId={}, message={}",
                                request.getSessionId(), request.getMessage());

                return agentCoachService.agentChat(
                                request.getSessionId(),
                                request.getMessage())
                                .map(chunk -> ServerSentEvent.<String>builder()
                                                .data(chunk.replace("\n", "\\n"))
                                                .build())
                                .concatWith(Flux.just(ServerSentEvent.<String>builder()
                                                .data("[DONE]")
                                                .build()));
        }

        /**
         * Agent 对话（带工具调用能力）
         */
        @PostMapping(value = "/agent/chat", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
        @Operation(summary = "Agent 对话", description = "发送消息到 Agent Coach，支持自动调用工具获取信息")
        public Flux<ServerSentEvent<String>> agentChat(@Valid @RequestBody CoachChatRequest request) {
                log.info("Agent Coach 对话: sessionId={}, message={}",
                                request.getSessionId(), request.getMessage());

                return agentCoachService.agentChat(
                                request.getSessionId(),
                                request.getMessage())
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
                PromptCoachSession session = agentCoachService.getSession(sessionId);
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

                String prompt = agentCoachService.confirmAndSave(request.getSessionId(), request.getPromptTemplateId());
                return Result.success(prompt);
        }
}
