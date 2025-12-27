package com.zzk.infrastructure.persistence.repository;

import com.zzk.domain.model.entity.ArenaResult;
import com.zzk.domain.repository.ArenaResultRepository;
import com.zzk.infrastructure.persistence.mapper.ArenaResultMapper;
import com.zzk.infrastructure.persistence.po.ArenaResultPO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 竞技场结果仓储实现
 * 
 * @author zzk
 * @since 1.0.0
 */
@Repository
@RequiredArgsConstructor
public class ArenaResultRepositoryImpl implements ArenaResultRepository {
    
    private final ArenaResultMapper arenaResultMapper;
    
    @Override
    public ArenaResult save(ArenaResult result) {
        ArenaResultPO po = toPO(result);
        if (result.getId() == null) {
            arenaResultMapper.insert(po);
            result.setId(po.getId());
        } else {
            arenaResultMapper.update(po);
        }
        return result;
    }
    
    @Override
    public List<ArenaResult> findBySessionId(Long sessionId) {
        return arenaResultMapper.selectBySessionId(sessionId).stream()
                .map(this::toEntity)
                .collect(Collectors.toList());
    }
    
    @Override
    public void saveAll(List<ArenaResult> results) {
        for (ArenaResult result : results) {
            save(result);
        }
    }
    
    private ArenaResult toEntity(ArenaResultPO po) {
        return ArenaResult.builder()
                .id(po.getId())
                .sessionId(po.getSessionId())
                .modelId(po.getModelId())
                .content(po.getContent())
                .tokensUsed(po.getTokensUsed())
                .latencyMs(po.getLatencyMs())
                .status(po.getStatus())
                .errorMessage(po.getErrorMessage())
                .createdAt(po.getCreatedAt())
                .build();
    }
    
    private ArenaResultPO toPO(ArenaResult entity) {
        return ArenaResultPO.builder()
                .id(entity.getId())
                .sessionId(entity.getSessionId())
                .modelId(entity.getModelId())
                .content(entity.getContent())
                .tokensUsed(entity.getTokensUsed())
                .latencyMs(entity.getLatencyMs())
                .status(entity.getStatus())
                .errorMessage(entity.getErrorMessage())
                .createdAt(entity.getCreatedAt())
                .build();
    }
}
