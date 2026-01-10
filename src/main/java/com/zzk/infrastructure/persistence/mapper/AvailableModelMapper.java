package com.zzk.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zzk.infrastructure.persistence.po.AvailableModelPO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * 可用模型 Mapper
 * 
 * @author zzk
 * @since 1.0.0
 */
@Mapper
public interface AvailableModelMapper extends BaseMapper<AvailableModelPO> {

    /**
     * 根据厂商ID查询所有启用的模型
     */
    default List<AvailableModelPO> findEnabledByProviderId(String providerId) {
        return selectList(new LambdaQueryWrapper<AvailableModelPO>()
                .eq(AvailableModelPO::getProviderId, providerId)
                .eq(AvailableModelPO::getEnabled, 1)
                .orderByAsc(AvailableModelPO::getSortOrder));
    }

    /**
     * 根据厂商ID查询所有模型
     */
    default List<AvailableModelPO> findByProviderId(String providerId) {
        return selectList(new LambdaQueryWrapper<AvailableModelPO>()
                .eq(AvailableModelPO::getProviderId, providerId)
                .orderByAsc(AvailableModelPO::getSortOrder));
    }

    /**
     * 查询所有启用的模型
     */
    default List<AvailableModelPO> findAllEnabled() {
        return selectList(new LambdaQueryWrapper<AvailableModelPO>()
                .eq(AvailableModelPO::getEnabled, 1)
                .orderByAsc(AvailableModelPO::getSortOrder));
    }

    /**
     * 根据厂商ID和模型ID查询
     */
    default AvailableModelPO findByProviderIdAndModelId(String providerId, String modelId) {
        return selectOne(new LambdaQueryWrapper<AvailableModelPO>()
                .eq(AvailableModelPO::getProviderId, providerId)
                .eq(AvailableModelPO::getModelId, modelId));
    }

    /**
     * 根据厂商ID删除所有模型
     */
    default int deleteByProviderId(String providerId) {
        return delete(new LambdaQueryWrapper<AvailableModelPO>()
                .eq(AvailableModelPO::getProviderId, providerId));
    }
}
