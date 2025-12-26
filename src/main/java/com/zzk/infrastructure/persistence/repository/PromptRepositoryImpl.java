package com.zzk.infrastructure.persistence.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zzk.domain.model.aggregate.Prompt;
import com.zzk.domain.repository.PromptRepository;
import com.zzk.infrastructure.persistence.converter.PromptConverter;
import com.zzk.infrastructure.persistence.mapper.PromptMapper;
import com.zzk.infrastructure.persistence.po.PromptPO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Prompt 仓储实现
 * 
 * @author zzk
 * @since 1.0.0
 */
@Repository
@RequiredArgsConstructor
public class PromptRepositoryImpl implements PromptRepository {

    private final PromptMapper promptMapper;

    @Override
    public Optional<Prompt> findById(Long id) {
        PromptPO po = promptMapper.selectById(id);
        return Optional.ofNullable(po).map(PromptConverter::toDomain);
    }

    @Override
    public List<Prompt> findByWorkspaceId(Long workspaceId) {
        LambdaQueryWrapper<PromptPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PromptPO::getWorkspaceId, workspaceId)
               .eq(PromptPO::getStatus, 1)
               .orderByDesc(PromptPO::getUpdatedAt);
        
        return promptMapper.selectList(wrapper).stream()
                .map(PromptConverter::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public void save(Prompt prompt) {
        PromptPO po = PromptConverter.toPO(prompt);
        if (prompt.getId() == null) {
            promptMapper.insert(po);
            prompt.setId(po.getId()); // 回填 ID
        } else {
            promptMapper.updateById(po);
        }
    }

    @Override
    public void updateLatestVersion(Long promptId, Long latestVersionId) {
        promptMapper.updateLatestVersionId(promptId, latestVersionId);
    }

    @Override
    public void deleteById(Long id) {
        // 软删除
        PromptPO po = new PromptPO();
        po.setId(id);
        po.setStatus(0);
        po.setUpdatedAt(java.time.LocalDateTime.now());
        promptMapper.updateById(po);
    }

    @Override
    public List<Prompt> findByCreatorId(Long creatorId) {
        LambdaQueryWrapper<PromptPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PromptPO::getCreatorId, creatorId)
               .eq(PromptPO::getStatus, 1)
               .orderByDesc(PromptPO::getUpdatedAt);
        
        return promptMapper.selectList(wrapper).stream()
                .map(PromptConverter::toDomain)
                .collect(Collectors.toList());
    }
}
