package com.zzk.domain.model.valueobject;

import java.time.LocalDateTime;

/**
 * 对话轮次值对象
 *
 * <p>记录单轮对话的角色和内容。
 *
 * @author zzk
 * @since 1.0.0
 */
public record DialogTurn(
        /**
         * 角色：user / assistant
         */
        String role,

        /**
         * 对话内容
         */
        String content,

        /**
         * 时间戳
         */
        LocalDateTime timestamp
) {

    /**
     * 创建用户消息
     */
    public static DialogTurn user(String content) {
        return new DialogTurn("user", content, LocalDateTime.now());
    }

    /**
     * 创建 AI 回复
     */
    public static DialogTurn assistant(String content) {
        return new DialogTurn("assistant", content, LocalDateTime.now());
    }
}
