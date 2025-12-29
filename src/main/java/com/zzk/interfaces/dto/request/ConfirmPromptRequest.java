package com.zzk.interfaces.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 确认保存 Prompt 请求
 */
@Data
@Schema(description = "确认保存 Prompt 请求")
public class ConfirmPromptRequest {

    @NotBlank(message = "会话ID不能为空")
    @Schema(description = "会话ID")
    private String sessionId;

    @NotNull(message = "模板ID不能为空")
    @Schema(description = "保存到的 Prompt 模板ID")
    private Long promptTemplateId;
}
