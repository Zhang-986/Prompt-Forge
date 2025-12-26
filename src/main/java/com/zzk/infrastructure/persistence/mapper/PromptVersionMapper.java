package com.zzk.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zzk.infrastructure.persistence.po.PromptVersionPO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * Prompt 版本 Mapper
 * 
 * @author zzk
 * @since 1.0.0
 */
@Mapper
public interface PromptVersionMapper extends BaseMapper<PromptVersionPO> {

    /**
     * 根据 Prompt ID 查询所有版本（按版本号倒序）
     */
    @Select("SELECT * FROM prompt_versions WHERE prompt_id = #{promptId} ORDER BY version_number DESC")
    List<PromptVersionPO> findByPromptIdOrderByVersionNumberDesc(@Param("promptId") Long promptId);

    /**
     * 获取指定 Prompt 的最新版本号
     */
    @Select("SELECT COALESCE(MAX(version_number), 0) FROM prompt_versions WHERE prompt_id = #{promptId}")
    Integer getMaxVersionNumber(@Param("promptId") Long promptId);
}
