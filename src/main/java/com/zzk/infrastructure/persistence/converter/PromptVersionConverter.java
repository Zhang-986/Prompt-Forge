package com.zzk.infrastructure.persistence.converter;

import com.zzk.domain.model.entity.PromptVersion;
import com.zzk.infrastructure.persistence.po.PromptVersionPO;

/**
 * PromptVersion 转换器
 * 
 * @author zzk
 * @since 1.0.0
 */
public class PromptVersionConverter {

    /**
     * PO -> Domain
     */
    public static PromptVersion toDomain(PromptVersionPO po) {
        if (po == null) {
            return null;
        }
        return PromptVersion.builder()
                .id(po.getId())
                .promptId(po.getPromptId())
                .versionNumber(po.getVersionNumber())
                .content(po.getContent())
                .variables(po.getVariables())
                .parentId(po.getParentId())
                .commitMessage(po.getCommitMessage())
                .authorId(po.getAuthorId())
                .contentHash(po.getContentHash())
                .createdAt(po.getCreatedAt())
                .build();
    }

    /**
     * Domain -> PO
     */
    public static PromptVersionPO toPO(PromptVersion domain) {
        if (domain == null) {
            return null;
        }
        PromptVersionPO po = new PromptVersionPO();
        po.setId(domain.getId());
        po.setPromptId(domain.getPromptId());
        po.setVersionNumber(domain.getVersionNumber());
        po.setContent(domain.getContent());
        po.setVariables(domain.getVariables());
        po.setParentId(domain.getParentId());
        po.setCommitMessage(domain.getCommitMessage());
        po.setAuthorId(domain.getAuthorId());
        po.setContentHash(domain.getContentHash());
        po.setCreatedAt(domain.getCreatedAt());
        return po;
    }
}
