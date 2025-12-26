package com.zzk.domain.service;

import cn.hutool.crypto.digest.DigestUtil;
import com.zzk.domain.model.aggregate.Prompt;
import com.zzk.domain.model.entity.PromptVersion;
import com.zzk.domain.repository.PromptRepository;
import com.zzk.domain.repository.PromptVersionRepository;
import com.zzk.infrastructure.exception.BusinessException;
import com.zzk.infrastructure.lock.DistributedLock;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Prompt 领域服务
 * 
 * <p>处理 Prompt 版本管理的核心业务逻辑：
 * <ul>
 *   <li>commit - 提交新版本（含 Diff 检查）</li>
 *   <li>rollback - 回滚到指定版本</li>
 *   <li>getVersionHistory - 获取版本历史</li>
 * </ul>
 * 
 * @author zzk
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PromptDomainService {

    private final PromptRepository promptRepository;
    private final PromptVersionRepository versionRepository;

    /**
     * 提交新版本
     * 
     * <p>核心逻辑：
     * <ol>
     *   <li>获取分布式锁，防止并发提交</li>
     *   <li>Diff 检查：对比新内容与父版本，相同则拒绝</li>
     *   <li>创建新版本记录</li>
     *   <li>更新 Prompt 的 HEAD 指针</li>
     * </ol>
     * 
     * @param promptId 所属 Prompt ID
     * @param newContent 新内容
     * @param parentVersionId 父版本 ID（可为空，表示首次提交）
     * @param commitMessage 提交说明
     * @param authorId 作者 ID
     * @return 新创建的版本
     */
    @Transactional(rollbackFor = Exception.class)
    @DistributedLock(key = "'prompt:commit:' + #promptId", waitTime = 5, leaseTime = 30)
    public PromptVersion commit(Long promptId, String newContent, Long parentVersionId, 
                                 String commitMessage, Long authorId) {
        log.info("开始提交 Prompt 版本: promptId={}, parentVersionId={}", promptId, parentVersionId);

        // 1. 获取 Prompt 聚合根
        Prompt prompt = promptRepository.findById(promptId)
                .orElseThrow(() -> new BusinessException("Prompt 不存在: " + promptId));

        if (prompt.isDeleted()) {
            throw new BusinessException("Prompt 已被删除，无法提交");
        }

        // 2. 计算内容哈希
        String contentHash = DigestUtil.sha256Hex(newContent);

        // 3. Diff 检查
        PromptVersion parentVersion = null;
        int newVersionNumber = 1;

        if (parentVersionId != null) {
            parentVersion = versionRepository.findById(parentVersionId)
                    .orElseThrow(() -> new BusinessException("父版本不存在: " + parentVersionId));

            // 检查父版本是否属于该 Prompt
            if (!parentVersion.getPromptId().equals(promptId)) {
                throw new BusinessException("父版本不属于该 Prompt");
            }

            // Diff 检查：内容相同则拒绝
            if (contentHash.equals(parentVersion.getContentHash())) {
                throw new BusinessException("内容无变化，无需提交");
            }

            newVersionNumber = parentVersion.getVersionNumber() + 1;
        }

        // 4. 创建新版本（数据不可变原则：只 INSERT，不 UPDATE）
        PromptVersion newVersion = PromptVersion.builder()
                .promptId(promptId)
                .versionNumber(newVersionNumber)
                .content(newContent)
                .variables(parentVersion != null ? parentVersion.getVariables() : null)
                .parentId(parentVersionId)
                .commitMessage(commitMessage)
                .authorId(authorId)
                .contentHash(contentHash)
                .createdAt(LocalDateTime.now())
                .build();

        versionRepository.save(newVersion);
        log.info("创建新版本成功: versionId={}, versionNumber={}", newVersion.getId(), newVersionNumber);

        // 5. 更新 HEAD 指针
        prompt.updateHead(newVersion);
        promptRepository.updateLatestVersion(promptId, newVersion.getId());
        log.info("更新 HEAD 指针: promptId={}, latestVersionId={}", promptId, newVersion.getId());

        return newVersion;
    }

    /**
     * 回滚到指定版本
     * 
     * <p>回滚本质是创建一个新版本，内容指向目标版本的内容
     * 
     * @param promptId Prompt ID
     * @param targetVersionId 目标版本 ID
     * @param authorId 操作者 ID
     * @return 新创建的版本（回滚版本）
     */
    @Transactional(rollbackFor = Exception.class)
    public PromptVersion rollback(Long promptId, Long targetVersionId, Long authorId) {
        log.info("开始回滚 Prompt: promptId={}, targetVersionId={}", promptId, targetVersionId);

        // 获取目标版本
        PromptVersion targetVersion = versionRepository.findById(targetVersionId)
                .orElseThrow(() -> new BusinessException("目标版本不存在: " + targetVersionId));

        if (!targetVersion.getPromptId().equals(promptId)) {
            throw new BusinessException("目标版本不属于该 Prompt");
        }

        // 获取当前最新版本
        Prompt prompt = promptRepository.findById(promptId)
                .orElseThrow(() -> new BusinessException("Prompt 不存在: " + promptId));

        // 创建回滚版本（以当前 HEAD 为父版本，内容为目标版本的内容）
        String rollbackMessage = String.format("Rollback to version %d", targetVersion.getVersionNumber());
        
        return commit(promptId, targetVersion.getContent(), 
                prompt.getLatestVersionId(), rollbackMessage, authorId);
    }

    /**
     * 获取版本历史（从最新到最老）
     * 
     * @param promptId Prompt ID
     * @return 版本列表
     */
    public List<PromptVersion> getVersionHistory(Long promptId) {
        return versionRepository.findByPromptIdOrderByVersionNumberDesc(promptId);
    }

    /**
     * 获取两个版本之间的 Diff
     * 
     * @param versionId1 版本 1 ID
     * @param versionId2 版本 2 ID
     * @return Diff 结果
     */
    public String getVersionDiff(Long versionId1, Long versionId2) {
        PromptVersion v1 = versionRepository.findById(versionId1)
                .orElseThrow(() -> new BusinessException("版本不存在: " + versionId1));
        PromptVersion v2 = versionRepository.findById(versionId2)
                .orElseThrow(() -> new BusinessException("版本不存在: " + versionId2));

        // TODO: 实现详细的 Diff 算法（如 Myers Diff）
        // 这里简单返回两个版本的内容
        return String.format("=== Version %d ===\n%s\n\n=== Version %d ===\n%s",
                v1.getVersionNumber(), v1.getContent(),
                v2.getVersionNumber(), v2.getContent());
    }
}
