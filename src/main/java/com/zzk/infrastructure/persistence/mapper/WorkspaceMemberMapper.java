package com.zzk.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zzk.infrastructure.persistence.po.WorkspaceMemberPO;
import org.apache.ibatis.annotations.Mapper;

/**
 * 工作空间成员 Mapper
 * 
 * @author zzk
 * @since 1.0.0
 */
@Mapper
public interface WorkspaceMemberMapper extends BaseMapper<WorkspaceMemberPO> {
}
