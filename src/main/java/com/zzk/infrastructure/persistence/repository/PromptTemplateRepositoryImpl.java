package com.zzk.infrastructure.persistence.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zzk.domain.model.entity.PromptTemplate;
import com.zzk.domain.repository.PromptTemplateRepository;
import com.zzk.infrastructure.persistence.mapper.PromptTemplateMapper;
import com.zzk.infrastructure.persistence.po.PromptTemplatePO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Prompt 模板仓储实现
 */
@Repository
@RequiredArgsConstructor
public class PromptTemplateRepositoryImpl implements PromptTemplateRepository {
    
    private final PromptTemplateMapper mapper;
    
    @Override
    public Optional<PromptTemplate> findById(Long id) {
        PromptTemplatePO po = mapper.selectById(id);
        return Optional.ofNullable(po).map(this::toDomain);
    }
    
    @Override
    public List<PromptTemplate> findAllActive() {
        LambdaQueryWrapper<PromptTemplatePO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PromptTemplatePO::getIsActive, true)
               .orderByDesc(PromptTemplatePO::getCloneCount);
        return mapper.selectList(wrapper).stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }
    
    @Override
    public List<PromptTemplate> findByCategory(String category) {
        LambdaQueryWrapper<PromptTemplatePO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PromptTemplatePO::getIsActive, true)
               .eq(PromptTemplatePO::getCategory, category)
               .orderByDesc(PromptTemplatePO::getCloneCount);
        return mapper.selectList(wrapper).stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }
    
    @Override
    public PromptTemplate save(PromptTemplate template) {
        PromptTemplatePO po = toPO(template);
        if (po.getId() == null) {
            po.setCreatedAt(LocalDateTime.now());
            po.setCloneCount(0);
            po.setIsActive(true);
            mapper.insert(po);
            template.setId(po.getId());
        } else {
            po.setUpdatedAt(LocalDateTime.now());
            mapper.updateById(po);
        }
        return template;
    }
    
    @Override
    public void incrementCloneCount(Long id) {
        mapper.incrementCloneCount(id);
    }
    
    // ==================== 转换方法 ====================
    
    private PromptTemplate toDomain(PromptTemplatePO po) {
        if (po == null) return null;
        return PromptTemplate.builder()
                .id(po.getId())
                .name(po.getName())
                .description(po.getDescription())
                .content(po.getContent())
                .category(po.getCategory())
                .authorId(po.getAuthorId())
                .authorName(po.getAuthorName())
                .cloneCount(po.getCloneCount())
                .isOfficial(po.getIsOfficial())
                .isActive(po.getIsActive())
                .createdAt(po.getCreatedAt())
                .updatedAt(po.getUpdatedAt())
                .build();
    }
    
    private PromptTemplatePO toPO(PromptTemplate domain) {
        if (domain == null) return null;
        PromptTemplatePO po = new PromptTemplatePO();
        po.setId(domain.getId());
        po.setName(domain.getName());
        po.setDescription(domain.getDescription());
        po.setContent(domain.getContent());
        po.setCategory(domain.getCategory());
        po.setAuthorId(domain.getAuthorId());
        po.setAuthorName(domain.getAuthorName());
        po.setCloneCount(domain.getCloneCount());
        po.setIsOfficial(domain.getIsOfficial());
        po.setIsActive(domain.getIsActive());
        po.setCreatedAt(domain.getCreatedAt());
        po.setUpdatedAt(domain.getUpdatedAt());
        return po;
    }
}
