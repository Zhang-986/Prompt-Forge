package com.zzk.domain.model.valueobject;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Prompt 变量值对象
 * 
 * <p>定义 Prompt 模板中的变量类型和约束
 * 
 * @author zzk
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PromptVariable {

    /**
     * 变量名
     */
    private String name;

    /**
     * 变量类型: string, enum, number, boolean
     */
    private String type;

    /**
     * 描述
     */
    private String description;

    /**
     * 默认值
     */
    private String defaultValue;

    /**
     * 是否必填
     */
    private Boolean required;

    /**
     * 枚举选项 (当 type 为 enum 时)
     */
    private String[] options;
}
