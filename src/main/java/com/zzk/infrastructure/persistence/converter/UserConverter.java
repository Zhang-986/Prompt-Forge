package com.zzk.infrastructure.persistence.converter;

import com.zzk.domain.model.aggregate.User;
import com.zzk.infrastructure.persistence.po.UserPO;

/**
 * 用户转换器
 * 
 * @author zzk
 * @since 1.0.0
 */
public class UserConverter {

    /**
     * PO -> Domain
     */
    public static User toDomain(UserPO po) {
        if (po == null) {
            return null;
        }
        return User.builder()
                .id(po.getId())
                .username(po.getUsername())
                .password(po.getPassword())
                .email(po.getEmail())
                .nickname(po.getNickname())
                .avatar(po.getAvatar())
                .role(po.getRole())
                .status(po.getStatus())
                .createdAt(po.getCreatedAt())
                .updatedAt(po.getUpdatedAt())
                .build();
    }

    /**
     * Domain -> PO
     */
    public static UserPO toPO(User domain) {
        if (domain == null) {
            return null;
        }
        UserPO po = new UserPO();
        po.setId(domain.getId());
        po.setUsername(domain.getUsername());
        po.setNickname(domain.getNickname());
        po.setPassword(domain.getPassword());
        po.setEmail(domain.getEmail());
        po.setAvatar(domain.getAvatar());
        po.setRole(domain.getRole());
        po.setStatus(domain.getStatus());
        po.setCreatedAt(domain.getCreatedAt());
        po.setUpdatedAt(domain.getUpdatedAt());
        return po;
    }
}
