package com.zzk.interfaces.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Diff 结果 DTO
 * 
 * <p>包含两个版本之间的差异信息
 * 
 * @author zzk
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DiffResult {

    /**
     * 源版本 ID
     */
    private Long sourceVersionId;

    /**
     * 源版本号
     */
    private Integer sourceVersionNumber;

    /**
     * 目标版本 ID
     */
    private Long targetVersionId;

    /**
     * 目标版本号
     */
    private Integer targetVersionNumber;

    /**
     * 差异行列表
     */
    private List<DiffLine> lines;

    /**
     * 新增行数
     */
    private Integer addedLines;

    /**
     * 删除行数
     */
    private Integer deletedLines;

    /**
     * 差异行
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DiffLine {
        /**
         * 行类型：EQUAL, INSERT, DELETE
         */
        private String type;

        /**
         * 源版本行号（删除行和相同行有值）
         */
        private Integer sourceLineNumber;

        /**
         * 目标版本行号（新增行和相同行有值）
         */
        private Integer targetLineNumber;

        /**
         * 行内容
         */
        private String content;
    }
}
