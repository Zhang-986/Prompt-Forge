package com.zzk.interfaces.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 提交版本请求 DTO
 * 
 * @author zzk
 * @since 1.0.0
 */
@Data
public class CommitVersionRequest {

    /**
     * 新内容
     */
    @NotBlank(message = "内容不能为空")
    private String content;

    /**
     * 父版本 ID（首次提交可为空）
     */
    private Long parentVersionId;

    /**
     * 提交说明
     */
    @NotBlank(message = "提交说明不能为空")
    private String commitMessage;
}
