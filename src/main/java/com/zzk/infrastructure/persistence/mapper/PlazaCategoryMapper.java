package com.zzk.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zzk.infrastructure.persistence.po.PlazaCategoryPO;
import org.apache.ibatis.annotations.Mapper;

/**
 * 广场分类 Mapper
 */
@Mapper
public interface PlazaCategoryMapper extends BaseMapper<PlazaCategoryPO> {
}
