package com.zzk.infrastructure.persistence.po;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 模型厂商持久化对象
 * 
 * @author zzk
 * @since 1.0.0
 */
@Data
@TableName("model_providers")
public class ModelProviderPO {

    /**
     * Provider ID (如 openai, deepseek)
     */
    @TableId(type = IdType.INPUT)
    private String id;

    /**
     * 显示名称
     */
    private String name;

    /**
     * 默认 API 地址
     */
    private String defaultBaseUrl;

    /**
     * 描述
     */
    private String description;

    /**
     * 官方文档链接
     */
    private String modelsUrl;

    /**
     * SDK类型: openai/anthropic/google
     */
    private String sdkType;

    /**
     * 是否启用: 0-禁用, 1-启用
     */
    private Integer enabled;

    /**
     * 排序 (越小越靠前)
     */
    private Integer sortOrder;

    /**
     * 最后同步时间
     */
    private LocalDateTime syncedAt;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
