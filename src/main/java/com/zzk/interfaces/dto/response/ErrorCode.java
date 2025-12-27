package com.zzk.interfaces.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 统一错误码枚举
 * 
 * @author zzk
 * @since 1.0.0
 */
@Getter
@AllArgsConstructor
public enum ErrorCode {

    // 成功
    SUCCESS(200, "操作成功"),

    // 客户端错误 4xx
    BAD_REQUEST(400, "请求参数错误"),
    UNAUTHORIZED(401, "未登录或登录已过期"),
    FORBIDDEN(403, "无权限访问"),
    NOT_FOUND(404, "资源不存在"),
    METHOD_NOT_ALLOWED(405, "请求方法不支持"),
    CONFLICT(409, "资源冲突"),
    RATE_LIMITED(429, "请求过于频繁，请稍后重试"),

    // 服务端错误 5xx
    SERVER_ERROR(500, "服务器内部错误"),
    SERVICE_UNAVAILABLE(503, "服务暂时不可用"),

    // 业务错误 1xxx
    USER_NOT_FOUND(1001, "用户不存在"),
    USER_ALREADY_EXISTS(1002, "用户名已存在"),
    PASSWORD_INCORRECT(1003, "密码错误"),
    TOKEN_INVALID(1004, "Token 无效"),
    TOKEN_EXPIRED(1005, "Token 已过期"),

    // Prompt 相关 2xxx
    PROMPT_NOT_FOUND(2001, "Prompt 不存在"),
    PROMPT_VERSION_NOT_FOUND(2002, "Prompt 版本不存在"),

    // 工作空间相关 3xxx
    WORKSPACE_NOT_FOUND(3001, "工作空间不存在"),
    WORKSPACE_ACCESS_DENIED(3002, "无权访问该工作空间"),

    // AI 相关 4xxx
    AI_CONFIG_NOT_FOUND(4001, "未配置 AI 模型"),
    AI_CALL_FAILED(4002, "AI 调用失败"),
    AI_RATE_LIMITED(4003, "AI 服务请求过于频繁");

    /**
     * 错误码
     */
    private final int code;

    /**
     * 错误消息
     */
    private final String message;
}
