package com.zzk.domain.repository;

import com.zzk.domain.model.entity.PromptVersion;

import java.util.List;
import java.util.Optional;

/**
 * Prompt 版本仓储接口
 * 
 * @author zzk
 * @since 1.0.0
 */
public interface PromptVersionRepository {

    /**
     * 根据 ID 查询
     */
    Optional<PromptVersion> findById(Long id);

    /**
     * 根据 Prompt ID 查询所有版本（按版本号倒序）
     */
    List<PromptVersion> findByPromptIdOrderByVersionNumberDesc(Long promptId);

    /**
     * 保存
     */
    void save(PromptVersion version);

    /**
     * 根据 Prompt ID 和版本号查询
     */
    Optional<PromptVersion> findByPromptIdAndVersionNumber(Long promptId, Integer versionNumber);

    /**
     * 获取指定 Prompt 的最新版本号
     */
    Integer getMaxVersionNumber(Long promptId);
}
