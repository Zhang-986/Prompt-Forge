package com.zzk.interfaces.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * Coach 对话请求
 */
@Data
@Schema(description = "Coach 对话请求")
public class CoachChatRequest {

    @NotBlank(message = "会话ID不能为空")
    @Schema(description = "会话ID")
    private String sessionId;

    @Schema(description = "用户消息（首次对话传 null，使用 session 中的 initialInput；后续对话传新消息）", example = "员工考勤管理")
    private String message;
}
