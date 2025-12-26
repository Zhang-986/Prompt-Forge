package com.zzk.domain.model.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Prompt 版本实体
 * 
 * <p>采用链式存储结构，通过 parentId 形成版本树。
 * 设计原则：一旦创建，数据不可变（只能 INSERT，禁止 UPDATE）
 * 
 * @author zzk
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PromptVersion {

    /**
     * 版本 ID
     */
    private Long id;

    /**
     * 所属 Prompt ID
     */
    private Long promptId;

    /**
     * 版本号
     */
    private Integer versionNumber;

    /**
     * Prompt 内容
     */
    private String content;

    /**
     * 变量定义 (JSON)
     * 格式: {"topic": {"type": "string", "description": "主题"}}
     */
    private String variables;

    /**
     * 父版本 ID (形成链式结构)
     */
    private Long parentId;

    /**
     * 提交说明
     */
    private String commitMessage;

    /**
     * 作者 ID
     */
    private Long authorId;

    /**
     * 内容哈希 (用于 Diff 检查)
     */
    private String contentHash;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    // ==================== 领域行为 ====================

    /**
     * 检查内容是否与另一个版本相同
     * 
     * @param other 另一个版本
     * @return 是否相同
     */
    public boolean hasSameContent(PromptVersion other) {
        if (other == null) {
            return false;
        }
        // 使用内容哈希比较
        if (this.contentHash != null && other.getContentHash() != null) {
            return this.contentHash.equals(other.getContentHash());
        }
        // 降级为内容直接比较
        return this.content != null && this.content.equals(other.getContent());
    }

    /**
     * 是否是根版本（没有父版本）
     */
    public boolean isRootVersion() {
        return this.parentId == null;
    }

    /**
     * 创建新版本（基于当前版本）
     * 
     * @param newContent 新内容
     * @param commitMessage 提交说明
     * @param authorId 作者 ID
     * @param contentHash 内容哈希
     * @return 新版本
     */
    public PromptVersion createChild(String newContent, String commitMessage, Long authorId, String contentHash) {
        return PromptVersion.builder()
                .promptId(this.promptId)
                .versionNumber(this.versionNumber + 1)
                .content(newContent)
                .variables(this.variables) // 继承变量定义
                .parentId(this.id)
                .commitMessage(commitMessage)
                .authorId(authorId)
                .contentHash(contentHash)
                .createdAt(LocalDateTime.now())
                .build();
    }
}
