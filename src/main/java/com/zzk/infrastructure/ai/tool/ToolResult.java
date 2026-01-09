package com.zzk.infrastructure.ai.tool;

/**
 * 工具执行结果
 * 
 * @author zzk
 * @since 1.0.0
 */
public record ToolResult(
        boolean success,
        String content,
        String errorMessage) {
    public static ToolResult success(String content) {
        return new ToolResult(true, content, null);
    }

    public static ToolResult error(String errorMessage) {
        return new ToolResult(false, null, errorMessage);
    }
}
