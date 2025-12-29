package com.zzk.infrastructure.persistence.repository;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.TypeReference;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zzk.domain.model.entity.UserPreference;
import com.zzk.domain.repository.UserPreferenceRepository;
import com.zzk.infrastructure.persistence.mapper.UserPreferenceMapper;
import com.zzk.infrastructure.persistence.po.UserPreferencePO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 用户偏好仓储实现
 * 
 * @author zzk
 * @since 1.0.0
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class UserPreferenceRepositoryImpl implements UserPreferenceRepository {

    private final UserPreferenceMapper mapper;

    @Override
    public void save(UserPreference preference) {
        UserPreferencePO po = toPO(preference);
        
        if (preference.getId() == null) {
            // 新增
            mapper.insert(po);
            preference.setId(po.getId());
            log.debug("创建用户偏好: userId={}, id={}", preference.getUserId(), po.getId());
        } else {
            // 更新
            mapper.updateById(po);
            log.debug("更新用户偏好: userId={}, id={}", preference.getUserId(), po.getId());
        }
    }

    @Override
    public Optional<UserPreference> findByUserId(Long userId) {
        LambdaQueryWrapper<UserPreferencePO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserPreferencePO::getUserId, userId);
        
        UserPreferencePO po = mapper.selectOne(wrapper);
        return Optional.ofNullable(po).map(this::toEntity);
    }

    // ==================== 转换方法 ====================

    private UserPreferencePO toPO(UserPreference entity) {
        return UserPreferencePO.builder()
                .id(entity.getId())
                .userId(entity.getUserId())
                .preferredTechStack(entity.getPreferredTechStack())
                .preferredOutputFormat(entity.getPreferredOutputFormat())
                .preferredProvider(entity.getPreferredProvider())
                .commonDomains(JSON.toJSONString(entity.getCommonDomains()))
                .topicFrequency(JSON.toJSONString(entity.getTopicFrequency()))
                .totalSessions(entity.getTotalSessions())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    private UserPreference toEntity(UserPreferencePO po) {
        List<String> domains = new ArrayList<>();
        Map<String, Integer> frequency = new HashMap<>();
        
        try {
            if (po.getCommonDomains() != null) {
                domains = JSON.parseObject(po.getCommonDomains(), new TypeReference<List<String>>() {});
            }
            if (po.getTopicFrequency() != null) {
                frequency = JSON.parseObject(po.getTopicFrequency(), new TypeReference<Map<String, Integer>>() {});
            }
        } catch (Exception e) {
            log.warn("解析用户偏好 JSON 失败: userId={}", po.getUserId(), e);
        }

        return UserPreference.builder()
                .id(po.getId())
                .userId(po.getUserId())
                .preferredTechStack(po.getPreferredTechStack())
                .preferredOutputFormat(po.getPreferredOutputFormat())
                .preferredProvider(po.getPreferredProvider())
                .commonDomains(domains)
                .topicFrequency(frequency)
                .totalSessions(po.getTotalSessions() != null ? po.getTotalSessions() : 0)
                .createdAt(po.getCreatedAt())
                .updatedAt(po.getUpdatedAt())
                .build();
    }
}
