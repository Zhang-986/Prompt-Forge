package com.zzk.interfaces.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 竞技场投票记录 DTO
 * 
 * @author zzk
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ArenaVoteDTO {

    private Long id;
    private Long sessionId;
    private String prompt;
    private String winnerModel;
    private String loserModel;
    private LocalDateTime createdAt;
}
