package com.zzk.infrastructure.persistence.converter;

import com.zzk.domain.model.entity.PromptTemplate;
import com.zzk.infrastructure.persistence.po.PromptTemplatePO;

/**
 * Prompt模板转换器
 * 
 * @author zzk
 * @since 1.0.0
 */
public class PromptTemplateConverter {

    /**
     * PO -> Domain
     */
    public static PromptTemplate toDomain(PromptTemplatePO po) {
        if (po == null) {
            return null;
        }
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

    /**
     * Domain -> PO
     */
    public static PromptTemplatePO toPO(PromptTemplate domain) {
        if (domain == null) {
            return null;
        }
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
