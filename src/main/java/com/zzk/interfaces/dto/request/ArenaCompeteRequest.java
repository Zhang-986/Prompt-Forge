package com.zzk.interfaces.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * 竞技场对比请求 DTO
 * 
 * @author zzk
 * @since 1.0.0
 */
@Data
public class ArenaCompeteRequest {

    /**
     * Prompt 版本 ID
     */
    @NotNull(message = "Prompt 版本 ID 不能为空")
    private Long promptVersionId;

    /**
     * 变量值映射
     */
    private Map<String, Object> variables;

    /**
     * 参与的模型 ID 列表
     */
    @NotEmpty(message = "至少选择一个模型")
    private List<String> modelIds;
}
