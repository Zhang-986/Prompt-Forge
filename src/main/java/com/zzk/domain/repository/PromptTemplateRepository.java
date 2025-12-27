package com.zzk.domain.repository;

import com.zzk.domain.model.entity.PromptTemplate;

import java.util.List;
import java.util.Optional;

/**
 * Prompt 模板仓储接口
 */
public interface PromptTemplateRepository {
    
    /**
     * 根据 ID 查询
     */
    Optional<PromptTemplate> findById(Long id);
    
    /**
     * 查询所有激活的模板
     */
    List<PromptTemplate> findAllActive();
    
    /**
     * 按分类查询
     */
    List<PromptTemplate> findByCategory(String category);
    
    /**
     * 保存模板
     */
    PromptTemplate save(PromptTemplate template);
    
    /**
     * 增加克隆次数
     */
    void incrementCloneCount(Long id);
}
