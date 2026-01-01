package com.zzk.infrastructure.persistence.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 竞技场会话持久化对象
 * 
 * @author zzk
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("arena_sessions")
public class ArenaSessionPO {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long promptVersionId;
    private String finalPrompt;
    private String variables;  // JSON string
    private String models;     // JSON string
    private String status;
    private Long creatorId;
    private LocalDateTime createdAt;
    private LocalDateTime completedAt;
}

