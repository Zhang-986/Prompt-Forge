package com.zzk.interfaces.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class OptimizePromptRequest {
    
    @NotBlank(message = "原始内容不能为空")
    private String originalContent;
    
    private String modelId; // 可选，指定用哪个模型优化
}
