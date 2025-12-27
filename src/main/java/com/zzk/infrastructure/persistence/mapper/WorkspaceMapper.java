package com.zzk.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zzk.infrastructure.persistence.po.WorkspacePO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 工作空间 Mapper
 * 
 * @author zzk
 * @since 1.0.0
 */
@Mapper
public interface WorkspaceMapper extends BaseMapper<WorkspacePO> {

    /**
     * 查询用户参与的所有工作空间（包括自己创建的和被邀请的）
     */
    @Select("SELECT DISTINCT w.* FROM workspaces w " +
            "LEFT JOIN workspace_members wm ON w.id = wm.workspace_id " +
            "WHERE w.owner_id = #{userId} OR wm.user_id = #{userId}")
    List<WorkspacePO> findByMemberUserId(@Param("userId") Long userId);
}
