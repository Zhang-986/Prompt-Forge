package com.zzk.application.service;

import com.zzk.domain.model.entity.ArenaResult;
import com.zzk.domain.model.entity.ArenaSession;
import com.zzk.domain.repository.ArenaResultRepository;
import com.zzk.domain.repository.ArenaSessionRepository;
import com.zzk.interfaces.dto.response.ArenaHistoryDTO;
import com.zzk.interfaces.dto.response.ArenaHistoryDetailDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 竞技场历史服务
 * 
 * @author zzk
 * @since 1.0.0
 */
@Service
@RequiredArgsConstructor
public class ArenaHistoryAppService {
    
    private final ArenaSessionRepository arenaSessionRepository;
    private final ArenaResultRepository arenaResultRepository;
    
    /**
     * 获取用户的竞技历史列表
     */
    public List<ArenaHistoryDTO> getHistory(Long userId, int limit) {
        List<ArenaSession> sessions = arenaSessionRepository.findByCreatorIdOrderByCreatedAtDesc(userId, limit);
        
        return sessions.stream()
                .map(session -> ArenaHistoryDTO.builder()
                        .id(session.getId())
                        .promptVersionId(session.getPromptVersionId())
                        .status(session.getStatus())
                        .models(session.getModels())
                        .createdAt(session.getCreatedAt())
                        .completedAt(session.getCompletedAt())
                        .build())
                .collect(Collectors.toList());
    }
    
    /**
     * 获取竞技详情
     */
    public ArenaHistoryDetailDTO getDetail(Long sessionId) {
        ArenaSession session = arenaSessionRepository.findById(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("会话不存在"));
        
        List<ArenaResult> results = arenaResultRepository.findBySessionId(sessionId);
        
        return ArenaHistoryDetailDTO.builder()
                .id(session.getId())
                .promptVersionId(session.getPromptVersionId())
                .finalPrompt(session.getFinalPrompt())
                .variables(session.getVariables())
                .models(session.getModels())
                .status(session.getStatus())
                .createdAt(session.getCreatedAt())
                .completedAt(session.getCompletedAt())
                .results(results.stream()
                        .map(r -> ArenaHistoryDetailDTO.ResultDTO.builder()
                                .modelId(r.getModelId())
                                .content(r.getContent())
                                .tokensUsed(r.getTokensUsed())
                                .latencyMs(r.getLatencyMs())
                                .status(r.getStatus())
                                .errorMessage(r.getErrorMessage())
                                .build())
                        .collect(Collectors.toList()))
                .build();
    }
}
