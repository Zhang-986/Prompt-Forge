package com.zzk.infrastructure.ai.skill;

import java.util.Map;

/**
 * Skill 执行结果
 * 
 * @author zzk
 * @since 1.0.0
 */
public class SkillResult {

    private final boolean success;
    private final String content;
    private final String error;

    private SkillResult(boolean success, String content, String error) {
        this.success = success;
        this.content = content;
        this.error = error;
    }

    /**
     * 创建成功结果
     */
    public static SkillResult success(String content) {
        return new SkillResult(true, content, null);
    }

    /**
     * 创建失败结果
     */
    public static SkillResult error(String error) {
        return new SkillResult(false, null, error);
    }

    public boolean isSuccess() {
        return success;
    }

    /**
     * 获取内容（成功时返回 content，失败时返回 error）
     */
    public String getContent() {
        return success ? content : error;
    }

    public String getError() {
        return error;
    }
}
