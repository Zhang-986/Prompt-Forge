package com.zzk.infrastructure.persistence.repository;

import com.zzk.domain.model.entity.ArenaSession;
import com.zzk.domain.repository.ArenaSessionRepository;
import com.zzk.infrastructure.persistence.mapper.ArenaSessionMapper;
import com.zzk.infrastructure.persistence.po.ArenaSessionPO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 竞技场会话仓储实现
 * 
 * @author zzk
 * @since 1.0.0
 */
@Repository
@RequiredArgsConstructor
public class ArenaSessionRepositoryImpl implements ArenaSessionRepository {
    
    private final ArenaSessionMapper arenaSessionMapper;
    
    @Override
    public ArenaSession save(ArenaSession session) {
        ArenaSessionPO po = toPO(session);
        if (session.getId() == null) {
            arenaSessionMapper.insert(po);
            session.setId(po.getId());
        }
        return session;
    }
    
    @Override
    public Optional<ArenaSession> findById(Long id) {
        ArenaSessionPO po = arenaSessionMapper.selectById(id);
        return Optional.ofNullable(po).map(this::toEntity);
    }
    
    @Override
    public List<ArenaSession> findByCreatorIdOrderByCreatedAtDesc(Long creatorId, int limit) {
        return arenaSessionMapper.selectByCreatorId(creatorId, limit).stream()
                .map(this::toEntity)
                .collect(Collectors.toList());
    }
    
    @Override
    public void updateStatus(Long sessionId, String status) {
        arenaSessionMapper.updateStatus(sessionId, status);
    }
    
    private ArenaSession toEntity(ArenaSessionPO po) {
        return ArenaSession.builder()
                .id(po.getId())
                .promptVersionId(po.getPromptVersionId())
                .finalPrompt(po.getFinalPrompt())
                .variables(po.getVariables())
                .models(po.getModels())
                .status(po.getStatus())
                .creatorId(po.getCreatorId())
                .createdAt(po.getCreatedAt())
                .completedAt(po.getCompletedAt())
                .build();
    }
    
    private ArenaSessionPO toPO(ArenaSession entity) {
        return ArenaSessionPO.builder()
                .id(entity.getId())
                .promptVersionId(entity.getPromptVersionId())
                .finalPrompt(entity.getFinalPrompt())
                .variables(entity.getVariables())
                .models(entity.getModels())
                .status(entity.getStatus())
                .creatorId(entity.getCreatorId())
                .createdAt(entity.getCreatedAt())
                .completedAt(entity.getCompletedAt())
                .build();
    }
}
