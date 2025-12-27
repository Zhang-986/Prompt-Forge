package com.zzk.interfaces.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 更新 Prompt 请求
 */
@Data
public class UpdatePromptRequest {
    
    @NotBlank(message = "名称不能为空")
    private String name;
    
    private String description;
}
