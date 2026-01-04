package com.zzk.infrastructure.persistence.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zzk.domain.model.entity.PlazaCategory;
import com.zzk.domain.repository.PlazaCategoryRepository;
import com.zzk.infrastructure.persistence.mapper.PlazaCategoryMapper;
import com.zzk.infrastructure.persistence.po.PlazaCategoryPO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 广场分类仓储实现
 */
@Repository
@RequiredArgsConstructor
public class PlazaCategoryRepositoryImpl implements PlazaCategoryRepository {
    
    private final PlazaCategoryMapper mapper;
    
    @Override
    public List<PlazaCategory> findAll() {
        LambdaQueryWrapper<PlazaCategoryPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByAsc(PlazaCategoryPO::getSortOrder);
        return mapper.selectList(wrapper).stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }
    
    @Override
    public List<PlazaCategory> findAllActive() {
        LambdaQueryWrapper<PlazaCategoryPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PlazaCategoryPO::getIsActive, true)
               .orderByAsc(PlazaCategoryPO::getSortOrder);
        return mapper.selectList(wrapper).stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }
    
    @Override
    public Optional<PlazaCategory> findById(Long id) {
        PlazaCategoryPO po = mapper.selectById(id);
        return Optional.ofNullable(po).map(this::toDomain);
    }
    
    @Override
    public Optional<PlazaCategory> findByValue(String value) {
        LambdaQueryWrapper<PlazaCategoryPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PlazaCategoryPO::getValue, value);
        PlazaCategoryPO po = mapper.selectOne(wrapper);
        return Optional.ofNullable(po).map(this::toDomain);
    }
    
    @Override
    public PlazaCategory save(PlazaCategory category) {
        PlazaCategoryPO po = toPO(category);
        if (po.getId() == null) {
            po.setCreatedAt(LocalDateTime.now());
            po.setIsActive(true);
            mapper.insert(po);
            category.setId(po.getId());
        } else {
            po.setUpdatedAt(LocalDateTime.now());
            mapper.updateById(po);
        }
        return category;
    }
    
    @Override
    public void deleteById(Long id) {
        mapper.deleteById(id);
    }
    
    // ==================== 转换方法 ====================
    
    private PlazaCategory toDomain(PlazaCategoryPO po) {
        if (po == null) return null;
        return PlazaCategory.builder()
                .id(po.getId())
                .value(po.getValue())
                .label(po.getLabel())
                .icon(po.getIcon())
                .sortOrder(po.getSortOrder())
                .isActive(po.getIsActive())
                .createdAt(po.getCreatedAt())
                .updatedAt(po.getUpdatedAt())
                .build();
    }
    
    private PlazaCategoryPO toPO(PlazaCategory domain) {
        if (domain == null) return null;
        PlazaCategoryPO po = new PlazaCategoryPO();
        po.setId(domain.getId());
        po.setValue(domain.getValue());
        po.setLabel(domain.getLabel());
        po.setIcon(domain.getIcon());
        po.setSortOrder(domain.getSortOrder());
        po.setIsActive(domain.getIsActive());
        po.setCreatedAt(domain.getCreatedAt());
        po.setUpdatedAt(domain.getUpdatedAt());
        return po;
    }
}
