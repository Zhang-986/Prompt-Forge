package com.zzk.infrastructure.persistence.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 广场分类 PO
 */
@Data
@TableName("plaza_categories")
public class PlazaCategoryPO {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private String value;
    private String label;
    private String icon;
    private Integer sortOrder;
    private Boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
