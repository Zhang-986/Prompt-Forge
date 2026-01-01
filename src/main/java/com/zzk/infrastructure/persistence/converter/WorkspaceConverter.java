package com.zzk.infrastructure.persistence.converter;

import com.zzk.domain.model.entity.Workspace;
import com.zzk.infrastructure.persistence.po.WorkspacePO;

/**
 * 工作空间转换器
 * 
 * @author zzk
 * @since 1.0.0
 */
public class WorkspaceConverter {

    /**
     * PO -> Domain
     */
    public static Workspace toDomain(WorkspacePO po) {
        if (po == null) {
            return null;
        }
        return Workspace.builder()
                .id(po.getId())
                .name(po.getName())
                .description(po.getDescription())
                .ownerId(po.getOwnerId())
                .createdAt(po.getCreatedAt())
                .updatedAt(po.getUpdatedAt())
                .build();
    }

    /**
     * Domain -> PO
     */
    public static WorkspacePO toPO(Workspace domain) {
        if (domain == null) {
            return null;
        }
        WorkspacePO po = new WorkspacePO();
        po.setId(domain.getId());
        po.setName(domain.getName());
        po.setDescription(domain.getDescription());
        po.setOwnerId(domain.getOwnerId());
        po.setCreatedAt(domain.getCreatedAt());
        po.setUpdatedAt(domain.getUpdatedAt());
        return po;
    }
}
