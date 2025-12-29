package com.zzk.domain.model.entity;

import com.zzk.domain.model.valueobject.CoachPhase;
import com.zzk.domain.model.valueobject.DialogTurn;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Prompt Coach 会话实体
 * 
 * <p>存储多轮对话的完整状态，包括对话历史、当前阶段、提取的信息等。
 * 会话临时存储在 Redis 中，TTL 2 小时。
 * 
 * @author zzk
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PromptCoachSession {

    /**
     * 会话ID（UUID）
     */
    private String sessionId;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 关联的 Prompt 模板ID（可选，用于保存时关联）
     */
    private Long promptTemplateId;

    /**
     * 使用的 AI 模型提供商
     */
    private String provider;

    /**
     * 对话历史
     */
    @Builder.Default
    private List<DialogTurn> history = new ArrayList<>();

    /**
     * 当前对话阶段
     */
    @Builder.Default
    private CoachPhase currentPhase = CoachPhase.GOAL_CLARIFICATION;

    /**
     * 已提取的信息（从对话中提取的关键信息）
     */
    @Builder.Default
    private Map<String, String> extractedInfo = new HashMap<>();

    /**
     * 最终生成的 Prompt
     */
    private String generatedPrompt;

    /**
     * 当前对话轮数
     */
    @Builder.Default
    private int turnCount = 0;

    /**
     * 最大对话轮数
     */
    @Builder.Default
    private int maxTurns = 10;

    /**
     * 会话创建时间
     */
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    /**
     * 最后更新时间
     */
    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();

    // ==================== 业务方法 ====================

    /**
     * 添加用户消息
     */
    public void addUserMessage(String content) {
        history.add(DialogTurn.user(content));
        turnCount++;
        updatedAt = LocalDateTime.now();
    }

    /**
     * 添加 AI 回复
     */
    public void addAssistantMessage(String content) {
        history.add(DialogTurn.assistant(content));
        updatedAt = LocalDateTime.now();
    }

    /**
     * 进入下一阶段
     */
    public void nextPhase() {
        this.currentPhase = this.currentPhase.next();
        updatedAt = LocalDateTime.now();
    }

    /**
     * 添加提取的信息
     */
    public void addExtractedInfo(String key, String value) {
        extractedInfo.put(key, value);
        updatedAt = LocalDateTime.now();
    }

    /**
     * 批量添加提取的信息
     */
    public void addExtractedInfo(Map<String, String> info) {
        if (info != null) {
            extractedInfo.putAll(info);
            updatedAt = LocalDateTime.now();
        }
    }

    /**
     * 是否达到最大轮数
     */
    public boolean isMaxTurnsReached() {
        return turnCount >= maxTurns;
    }

    /**
     * 是否处于最终阶段
     */
    public boolean isFinalPhase() {
        return currentPhase.isFinal();
    }

    /**
     * 获取对话历史的格式化文本
     */
    public String getFormattedHistory() {
        StringBuilder sb = new StringBuilder();
        for (DialogTurn turn : history) {
            String role = "user".equals(turn.role()) ? "用户" : "AI";
            sb.append(role).append(": ").append(turn.content()).append("\n\n");
        }
        return sb.toString();
    }

    /**
     * 获取提取信息的格式化文本
     */
    public String getFormattedExtractedInfo() {
        if (extractedInfo.isEmpty()) {
            return "暂无";
        }
        StringBuilder sb = new StringBuilder();
        extractedInfo.forEach((k, v) -> sb.append("- ").append(k).append(": ").append(v).append("\n"));
        return sb.toString();
    }
}
