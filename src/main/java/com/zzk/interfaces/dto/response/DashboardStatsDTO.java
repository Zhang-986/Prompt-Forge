package com.zzk.interfaces.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 仪表盘统计数据 DTO
 * 
 * @author zzk
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardStatsDTO {
    
    /**
     * 总用户数
     */
    private long totalUsers;
    
    /**
     * 总工作空间数
     */
    private long totalWorkspaces;
    
    /**
     * 总 Prompt 数
     */
    private long totalPrompts;
    
    /**
     * 公开 Prompt 数
     */
    private long publicPrompts;
    
    /**
     * 总竞技场会话次数
     */
    private long totalArenaSessions;
    
    /**
     * 最近7天活跃用户数
     */
    private long activeUsersLast7Days;
    
    /**
     * 广场模板数
     */
    private long totalTemplates;
}
