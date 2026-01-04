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
 * 竞技场投票持久化对象
 * 
 * @author zzk
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("arena_votes")
public class ArenaVotePO {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long sessionId;
    private String winnerModel;
    private String loserModel;
    private Long voterId;
    private LocalDateTime createdAt;
}
