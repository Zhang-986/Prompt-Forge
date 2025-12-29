package com.zzk.interfaces.dto.response;

import com.zzk.domain.model.entity.PromptCoachSession;
import com.zzk.domain.model.valueobject.DialogTurn;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * Coach 会话响应
 */
@Data
@Builder
@Schema(description = "Coach 会话响应")
public class CoachSessionResponse {

    @Schema(description = "会话ID")
    private String sessionId;

    @Schema(description = "当前阶段")
    private String currentPhase;

    @Schema(description = "当前阶段描述")
    private String phaseDescription;

    @Schema(description = "对话轮数")
    private int turnCount;

    @Schema(description = "对话历史")
    private List<DialogTurn> history;

    @Schema(description = "已提取的信息")
    private Map<String, String> extractedInfo;

    @Schema(description = "生成的 Prompt（如果有）")
    private String generatedPrompt;

    @Schema(description = "是否已生成最终 Prompt")
    private boolean promptGenerated;

    /**
     * 从实体转换
     */
    public static CoachSessionResponse from(PromptCoachSession session) {
        return CoachSessionResponse.builder()
                .sessionId(session.getSessionId())
                .currentPhase(session.getCurrentPhase().name())
                .phaseDescription(session.getCurrentPhase().getDescription())
                .turnCount(session.getTurnCount())
                .history(session.getHistory())
                .extractedInfo(session.getExtractedInfo())
                .generatedPrompt(session.getGeneratedPrompt())
                .promptGenerated(session.getGeneratedPrompt() != null && !session.getGeneratedPrompt().isBlank())
                .build();
    }
}
