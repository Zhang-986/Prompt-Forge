package com.zzk.interfaces.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.List;

/**
 * 开始 Coach 会话请求
 */
@Data
@Schema(description = "开始 Coach 会话请求")
public class StartCoachRequest {

    @NotBlank(message = "初始输入不能为空")
    @Schema(description = "用户的初始想法", example = "我想做个管理系统")
    private String initialInput;

    @Schema(description = "AI 模型提供商（可选）", example = "zhipu")
    private String provider;

    @Schema(description = "用户选择的 Skills（为空则不使用工具）")
    private List<String> selectedSkillNames;
}
