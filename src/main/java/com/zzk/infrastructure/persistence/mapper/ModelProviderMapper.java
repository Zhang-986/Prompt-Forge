package com.zzk.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zzk.infrastructure.persistence.po.ModelProviderPO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * 模型厂商 Mapper
 * 
 * @author zzk
 * @since 1.0.0
 */
@Mapper
public interface ModelProviderMapper extends BaseMapper<ModelProviderPO> {

    /**
     * 查询所有启用的厂商，按排序字段排列
     */
    default List<ModelProviderPO> findAllEnabled() {
        return selectList(new LambdaQueryWrapper<ModelProviderPO>()
                .eq(ModelProviderPO::getEnabled, 1)
                .orderByAsc(ModelProviderPO::getSortOrder));
    }

    /**
     * 查询所有厂商，按排序字段排列
     */
    default List<ModelProviderPO> findAllOrdered() {
        return selectList(new LambdaQueryWrapper<ModelProviderPO>()
                .orderByAsc(ModelProviderPO::getSortOrder));
    }
}
