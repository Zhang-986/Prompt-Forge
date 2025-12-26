package com.zzk.interfaces.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 创建 Prompt 请求 DTO
 * 
 * @author zzk
 * @since 1.0.0
 */
@Data
public class CreatePromptRequest {

    /**
     * Prompt 名称
     */
    @NotBlank(message = "名称不能为空")
    private String name;

    /**
     * 描述
     */
    private String description;

    /**
     * 初始内容
     */
    @NotBlank(message = "内容不能为空")
    private String content;

    /**
     * 工作空间 ID
     */
    @NotNull(message = "工作空间 ID 不能为空")
    private Long workspaceId;
}
