package com.zzk.domain.service;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.crypto.digest.DigestUtil;
import com.alibaba.fastjson2.JSON;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.difflib.DiffUtils;
import com.github.difflib.patch.AbstractDelta;
import com.github.difflib.patch.Patch;
import com.zzk.domain.model.aggregate.Prompt;
import com.zzk.domain.model.entity.PromptVersion;
import com.zzk.domain.repository.PromptRepository;
import com.zzk.domain.repository.PromptVersionRepository;
import com.zzk.infrastructure.exception.BusinessException;
import com.zzk.infrastructure.lock.DistributedLock;
import com.zzk.interfaces.dto.response.DiffResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
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
    private final ObjectMapper objectMapper;

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

        // 判断是否为第一版本Prompt
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

    public DiffResult getVersionDiff(Long versionId1, Long versionId2) {
        // 1. 数据准备
        PromptVersion v1 = getVersionOrThrow(versionId1);
        PromptVersion v2 = getVersionOrThrow(versionId2);

        List<String> sourceLines = splitLines(v1.getContent());
        List<String> targetLines = splitLines(v2.getContent());

        // 2. 计算差异核心算法
        Patch<String> patch = DiffUtils.diff(sourceLines, targetLines);
        List<AbstractDelta<String>> deltas = patch.getDeltas();

        // 3. 初始化状态
        List<DiffResult.DiffLine> diffLines = new ArrayList<>();
        int sourceLineNum = 1;
        int targetLineNum = 1;
        int addedCount = 0;
        int deletedCount = 0;

        int deltaIndex = 0;

        // 有变化的地方，拿出来变化的地方
        AbstractDelta<String> currentDelta = CollectionUtil.isNotEmpty(deltas) ? deltas.get(0) : null;
        int maxLines = Math.max(sourceLines.size(), targetLines.size());
        log.info("FastJSON2 得到变化补丁{}", JSON.toJSONString(currentDelta));


        // 4. 双指针遍历与缝合
        int i = 0;
        while (sourceLineNum <= sourceLines.size() || targetLineNum <= targetLines.size()) {
            // 安全中断检查
            if (++i > maxLines * 3) {
                log.warn("Diff 计算异常：超过最大迭代次数");
                break;
            }

            // 判断是否命中了变更块（Hit Delta）
            if (isHitDelta(currentDelta, sourceLineNum)) {
                switch (currentDelta.getType()) {
                    case DELETE:
                        deletedCount += processDelete(diffLines, currentDelta, sourceLineNum);
                        sourceLineNum += currentDelta.getSource().getLines().size();
                        break;
                    case INSERT:
                        addedCount += processInsert(diffLines, currentDelta, targetLineNum);
                        targetLineNum += currentDelta.getTarget().getLines().size();
                        break;
                    case CHANGE:
                        // Change = Delete (Old) + Insert (New)
                        deletedCount += processDelete(diffLines, currentDelta, sourceLineNum);
                        sourceLineNum += currentDelta.getSource().getLines().size();

                        addedCount += processInsert(diffLines, currentDelta, targetLineNum);
                        targetLineNum += currentDelta.getTarget().getLines().size();
                        break;
                    default:
                        break;
                }
                // 指向下一个变更块
                deltaIndex++;
                currentDelta = deltaIndex < deltas.size() ? deltas.get(deltaIndex) : null;
            } else if (sourceLineNum <= sourceLines.size()) {
                // 处理相同行 (Equal)
                addDiffLine(diffLines, "EQUAL", sourceLineNum, targetLineNum, sourceLines.get(sourceLineNum - 1));
                sourceLineNum++;
                targetLineNum++;
            } else {
                // 剩下的情况（如 Target 还有剩余但 Source 读完了，且没有 Insert Delta）
                break;
            }
        }

        // 5. 构建结果
        return DiffResult.builder()
                .sourceVersionId(versionId1)
                .sourceVersionNumber(v1.getVersionNumber())
                .targetVersionId(versionId2)
                .targetVersionNumber(v2.getVersionNumber())
                .lines(diffLines)
                .addedLines(addedCount)
                .deletedLines(deletedCount)
                .build();
    }

// ==================== 下面是提取的私有辅助方法 (Helper Methods) ====================

    /**
     * 辅助方法：判断当前是否撞到了变更块
     */
    private boolean isHitDelta(AbstractDelta<String> delta, int currentSourceLine) {
        return delta != null && currentSourceLine == delta.getSource().getPosition() + 1;
    }

    /**
     * 辅助方法：处理删除逻辑
     * @return 删除了几行
     */
    private int processDelete(List<DiffResult.DiffLine> lines, AbstractDelta<String> delta, int startLineNum) {
        int count = 0;
        for (String line : delta.getSource().getLines()) {
            addDiffLine(lines, "DELETE", startLineNum + count, null, line);
            count++;
        }
        return count;
    }

    /**
     * 辅助方法：处理新增逻辑
     * @return 新增了几行
     */
    private int processInsert(List<DiffResult.DiffLine> lines, AbstractDelta<String> delta, int startLineNum) {
        int count = 0;
        for (String line : delta.getTarget().getLines()) {
            addDiffLine(lines, "INSERT", null, startLineNum + count, line);
            count++;
        }
        return count;
    }

    /**
     * 核心辅助方法：统一构建 DiffLine，消除样板代码
     */
    private void addDiffLine(List<DiffResult.DiffLine> lines, String type, Integer srcNum, Integer tgtNum, String content) {
        lines.add(DiffResult.DiffLine.builder()
                .type(type)
                .sourceLineNumber(srcNum)
                .targetLineNumber(tgtNum)
                .content(content)
                .build());
    }

    /**
     * 辅助方法：获取版本或抛异常
     */
    private PromptVersion getVersionOrThrow(Long id) {
        return versionRepository.findById(id)
                .orElseThrow(() -> new BusinessException("版本不存在: " + id));
    }

    /**
     * 辅助方法：安全分割字符串
     */
    private List<String> splitLines(String content) {
        if (content == null) return Collections.emptyList();
        return Arrays.asList(content.split("\n", -1));
    }

}
