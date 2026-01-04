package com.zzk.domain.model.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 竞技场投票实体
 * 
 * <p>记录用户对模型输出的投票
 * 
 * @author zzk
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ArenaVote {

    /**
     * 投票 ID
     */
    private Long id;

    /**
     * 竞技会话 ID (可选)
     */
    private Long sessionId;

    /**
     * 胜者模型 ID
     */
    private String winnerModel;

    /**
     * 败者模型 ID
     */
    private String loserModel;

    /**
     * 投票用户 ID
     */
    private Long voterId;

    /**
     * 投票时间
     */
    private LocalDateTime createdAt;
}
