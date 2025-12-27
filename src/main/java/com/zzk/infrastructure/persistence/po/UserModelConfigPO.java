package com.zzk.infrastructure.persistence.po;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户模型配置持久化对象
 * 
 * @author zzk
 * @since 1.0.0
 */
@Data
@TableName("user_model_configs")
public class UserModelConfigPO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private String provider;

    private String apiKey;

    private String baseUrl;

    private String modelName;

    private Integer enabled;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
