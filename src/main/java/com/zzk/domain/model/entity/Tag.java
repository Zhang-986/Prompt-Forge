package com.zzk.domain.model.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 标签实体
 * 
 * <p>用于对 Prompt 进行分类标记
 * 
 * @author zzk
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Tag {
    
    /**
     * 标签 ID
     */
    private Long id;
    
    /**
     * 标签名称
     */
    private String name;
    
    /**
     * 标签颜色 (十六进制)
     */
    private String color;
    
    /**
     * 创建者 ID
     */
    private Long creatorId;
    
    /**
     * 工作空间 ID
     */
    private Long workspaceId;
    
    /**
     * 创建时间
     */
    private LocalDateTime createdAt;
    
    // ==================== 预定义颜色 ====================
    
    public static final String COLOR_PURPLE = "#5e6ad2";
    public static final String COLOR_GREEN = "#10b981";
    public static final String COLOR_BLUE = "#3b82f6";
    public static final String COLOR_ORANGE = "#f59e0b";
    public static final String COLOR_RED = "#ef4444";
    public static final String COLOR_PINK = "#ec4899";
    public static final String COLOR_CYAN = "#06b6d4";
    public static final String COLOR_GRAY = "#6b7280";
}
