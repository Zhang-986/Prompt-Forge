package com.zzk.infrastructure.persistence.repository;

import com.zzk.domain.model.entity.UserModelConfig;
import com.zzk.domain.repository.UserModelConfigRepository;
import com.zzk.infrastructure.persistence.mapper.UserModelConfigMapper;
import com.zzk.infrastructure.persistence.po.UserModelConfigPO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 用户模型配置仓储实现
 * 
 * @author zzk
 * @since 1.0.0
 */
@Repository
@RequiredArgsConstructor
public class UserModelConfigRepositoryImpl implements UserModelConfigRepository {

    private final UserModelConfigMapper mapper;

    @Override
    public Optional<UserModelConfig> findById(Long id) {
        UserModelConfigPO po = mapper.selectById(id);
        return Optional.ofNullable(po).map(this::toEntity);
    }

    @Override
    public List<UserModelConfig> findByUserId(Long userId) {
        return mapper.findByUserId(userId).stream()
                .map(this::toEntity)
                .collect(Collectors.toList());
    }

    @Override
    public Optional<UserModelConfig> findByUserIdAndProvider(Long userId, String provider) {
        UserModelConfigPO po = mapper.findByUserIdAndProvider(userId, provider);
        return Optional.ofNullable(po).map(this::toEntity);
    }

    @Override
    public List<UserModelConfig> findEnabledByUserId(Long userId) {
        return mapper.findEnabledByUserId(userId).stream()
                .map(this::toEntity)
                .collect(Collectors.toList());
    }

    @Override
    public UserModelConfig save(UserModelConfig config) {
        UserModelConfigPO po = toPO(config);
        po.setCreatedAt(java.time.LocalDateTime.now());
        po.setUpdatedAt(java.time.LocalDateTime.now());
        mapper.insert(po);
        config.setId(po.getId());
        return config;
    }

    @Override
    public UserModelConfig update(UserModelConfig config) {
        UserModelConfigPO po = toPO(config);
        po.setUpdatedAt(java.time.LocalDateTime.now());
        mapper.updateById(po);
        return config;
    }

    @Override
    public void deleteById(Long id) {
        mapper.deleteById(id);
    }

    private UserModelConfig toEntity(UserModelConfigPO po) {
        return UserModelConfig.builder()
                .id(po.getId())
                .userId(po.getUserId())
                .provider(po.getProvider())
                .apiKey(po.getApiKey())
                .baseUrl(po.getBaseUrl())
                .modelName(po.getModelName())
                .enabled(po.getEnabled() == 1)
                .createdAt(po.getCreatedAt())
                .updatedAt(po.getUpdatedAt())
                .build();
    }

    private UserModelConfigPO toPO(UserModelConfig entity) {
        UserModelConfigPO po = new UserModelConfigPO();
        po.setId(entity.getId());
        po.setUserId(entity.getUserId());
        po.setProvider(entity.getProvider());
        po.setApiKey(entity.getApiKey());
        po.setBaseUrl(entity.getBaseUrl());
        po.setModelName(entity.getModelName());
        po.setEnabled(entity.getEnabled() ? 1 : 0);
        return po;
    }
}
