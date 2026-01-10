package com.zzk.infrastructure.persistence.po;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 可用模型持久化对象
 * 
 * @author zzk
 * @since 1.0.0
 */
@Data
@TableName("available_models")
public class AvailableModelPO {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 厂商ID
     */
    private String providerId;

    /**
     * 模型ID (调用API时使用)
     */
    private String modelId;

    /**
     * 显示名称
     */
    private String displayName;

    /**
     * 描述
     */
    private String description;

    /**
     * 上下文长度 (tokens)
     */
    private Integer contextWindow;

    /**
     * 支持视觉: 0-否, 1-是
     */
    private Integer supportsVision;

    /**
     * 支持函数调用: 0-否, 1-是
     */
    private Integer supportsFunctionCall;

    /**
     * 是否启用: 0-禁用, 1-启用
     */
    private Integer enabled;

    /**
     * 排序 (越小越靠前)
     */
    private Integer sortOrder;

    /**
     * 来源: sync-同步, manual-手动添加
     */
    private String source;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
