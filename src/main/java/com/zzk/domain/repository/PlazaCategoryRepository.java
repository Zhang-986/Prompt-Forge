package com.zzk.domain.repository;

import com.zzk.domain.model.entity.PlazaCategory;

import java.util.List;
import java.util.Optional;

/**
 * 广场分类仓储接口
 */
public interface PlazaCategoryRepository {
    
    /**
     * 查询所有分类
     */
    List<PlazaCategory> findAll();
    
    /**
     * 查询所有启用的分类
     */
    List<PlazaCategory> findAllActive();
    
    /**
     * 根据 ID 查询
     */
    Optional<PlazaCategory> findById(Long id);
    
    /**
     * 根据 value 查询
     */
    Optional<PlazaCategory> findByValue(String value);
    
    /**
     * 保存分类
     */
    PlazaCategory save(PlazaCategory category);
    
    /**
     * 删除分类
     */
    void deleteById(Long id);
}
