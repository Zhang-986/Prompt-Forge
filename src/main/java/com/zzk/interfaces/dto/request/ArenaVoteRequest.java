package com.zzk.interfaces.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 竞技场投票请求
 * 
 * @author zzk
 * @since 1.0.0
 */
@Data
public class ArenaVoteRequest {
    
    /**
     * 竞技会话 ID (可选)
     */
    private Long sessionId;
    
    /**
     * 胜者模型 ID
     */
    @NotBlank(message = "胜者模型不能为空")
    private String winnerModel;
    
    /**
     * 败者模型 ID
     */
    @NotBlank(message = "败者模型不能为空")
    private String loserModel;
}
