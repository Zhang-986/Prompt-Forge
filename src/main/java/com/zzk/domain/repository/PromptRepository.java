package com.zzk.domain.repository;

import com.zzk.domain.model.aggregate.Prompt;

import java.util.List;
import java.util.Optional;

/**
 * Prompt 仓储接口
 * 
 * <p>定义 Prompt 聚合根的持久化操作，遵循 DDD 仓储模式
 * 
 * @author zzk
 * @since 1.0.0
 */
public interface PromptRepository {

    /**
     * 根据 ID 查询
     */
    Optional<Prompt> findById(Long id);

    /**
     * 根据工作空间 ID 查询列表
     */
    List<Prompt> findByWorkspaceId(Long workspaceId);

    /**
     * 保存
     */
    void save(Prompt prompt);

    /**
     * 更新最新版本 ID
     */
    void updateLatestVersion(Long promptId, Long latestVersionId);

    /**
     * 删除（软删除）
     */
    void deleteById(Long id);

    /**
     * 根据创建者 ID 查询
     */
    List<Prompt> findByCreatorId(Long creatorId);
}
