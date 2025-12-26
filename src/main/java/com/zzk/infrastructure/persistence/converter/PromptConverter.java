package com.zzk.infrastructure.persistence.converter;

import com.zzk.domain.model.aggregate.Prompt;
import com.zzk.infrastructure.persistence.po.PromptPO;

/**
 * Prompt 转换器
 * 
 * @author zzk
 * @since 1.0.0
 */
public class PromptConverter {

    /**
     * PO -> Domain
     */
    public static Prompt toDomain(PromptPO po) {
        if (po == null) {
            return null;
        }
        return Prompt.builder()
                .id(po.getId())
                .name(po.getName())
                .description(po.getDescription())
                .workspaceId(po.getWorkspaceId())
                .latestVersionId(po.getLatestVersionId())
                .creatorId(po.getCreatorId())
                .isPublic(po.getIsPublic())
                .status(po.getStatus())
                .createdAt(po.getCreatedAt())
                .updatedAt(po.getUpdatedAt())
                .build();
    }

    /**
     * Domain -> PO
     */
    public static PromptPO toPO(Prompt domain) {
        if (domain == null) {
            return null;
        }
        PromptPO po = new PromptPO();
        po.setId(domain.getId());
        po.setName(domain.getName());
        po.setDescription(domain.getDescription());
        po.setWorkspaceId(domain.getWorkspaceId());
        po.setLatestVersionId(domain.getLatestVersionId());
        po.setCreatorId(domain.getCreatorId());
        po.setIsPublic(domain.getIsPublic());
        po.setStatus(domain.getStatus());
        po.setCreatedAt(domain.getCreatedAt());
        po.setUpdatedAt(domain.getUpdatedAt());
        return po;
    }
}
