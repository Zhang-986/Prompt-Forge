package com.zzk.infrastructure.persistence.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zzk.domain.model.entity.PromptVersion;
import com.zzk.domain.repository.PromptVersionRepository;
import com.zzk.infrastructure.persistence.converter.PromptVersionConverter;
import com.zzk.infrastructure.persistence.mapper.PromptVersionMapper;
import com.zzk.infrastructure.persistence.po.PromptVersionPO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * PromptVersion 仓储实现
 * 
 * @author zzk
 * @since 1.0.0
 */
@Repository
@RequiredArgsConstructor
public class PromptVersionRepositoryImpl implements PromptVersionRepository {

    private final PromptVersionMapper versionMapper;

    @Override
    public Optional<PromptVersion> findById(Long id) {
        PromptVersionPO po = versionMapper.selectById(id);
        return Optional.ofNullable(po).map(PromptVersionConverter::toDomain);
    }

    @Override
    public List<PromptVersion> findByPromptIdOrderByVersionNumberDesc(Long promptId) {
        return versionMapper.findByPromptIdOrderByVersionNumberDesc(promptId).stream()
                .map(PromptVersionConverter::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public void save(PromptVersion version) {
        PromptVersionPO po = PromptVersionConverter.toPO(version);
        versionMapper.insert(po);
        version.setId(po.getId()); // 回填 ID
    }

    @Override
    public Optional<PromptVersion> findByPromptIdAndVersionNumber(Long promptId, Integer versionNumber) {
        LambdaQueryWrapper<PromptVersionPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PromptVersionPO::getPromptId, promptId)
               .eq(PromptVersionPO::getVersionNumber, versionNumber);
        
        PromptVersionPO po = versionMapper.selectOne(wrapper);
        return Optional.ofNullable(po).map(PromptVersionConverter::toDomain);
    }

    @Override
    public Integer getMaxVersionNumber(Long promptId) {
        return versionMapper.getMaxVersionNumber(promptId);
    }
}
