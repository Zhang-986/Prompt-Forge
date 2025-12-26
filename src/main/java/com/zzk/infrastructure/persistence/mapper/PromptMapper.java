package com.zzk.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zzk.infrastructure.persistence.po.PromptPO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

/**
 * Prompt Mapper
 * 
 * @author zzk
 * @since 1.0.0
 */
@Mapper
public interface PromptMapper extends BaseMapper<PromptPO> {

    /**
     * 更新最新版本 ID
     */
    @Update("UPDATE prompts SET latest_version_id = #{latestVersionId}, updated_at = NOW() WHERE id = #{promptId}")
    int updateLatestVersionId(@Param("promptId") Long promptId, @Param("latestVersionId") Long latestVersionId);
}
