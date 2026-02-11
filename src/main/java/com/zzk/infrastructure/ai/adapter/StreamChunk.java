package com.zzk.infrastructure.ai.adapter;

/**
 * 统一流式响应块
 * 
 * <p>
 * 将各厂商的不同响应格式适配为统一模型，
 * 使前端能够区分深度思考和正式回答。
 * 
 * <p>
 * 设计模式：适配器模式（Adapter Pattern）
 * - 将不兼容的接口转换为统一接口
 * - 解耦厂商格式与前端渲染逻辑
 * 
 * @author zzk
 * @since 1.0.0
 */
public record StreamChunk(
        ChunkType type,
        String content) {
    /**
     * 响应块类型
     */
    public enum ChunkType {
        /**
         * 深度思考/推理过程
         * <p>
         * 对应：
         * - OpenAI o1: reasoning_content
         * - DeepSeek R1: reasoning_content
         * - Claude: thinking_delta
         * - Gemini: thought
         */
        REASONING,

        /**
         * 正式回答内容
         */
        CONTENT,

        /**
         * 工具调用
         */
        TOOL_CALL,

        /**
         * 流结束标识
         */
        DONE
    }

    /**
     * 创建推理类型的块
     */
    public static StreamChunk reasoning(String content) {
        return new StreamChunk(ChunkType.REASONING, content);
    }

    /**
     * 创建内容类型的块
     */
    public static StreamChunk content(String content) {
        return new StreamChunk(ChunkType.CONTENT, content);
    }

    /**
     * 创建工具调用类型的块
     */
    public static StreamChunk toolCall(String content) {
        return new StreamChunk(ChunkType.TOOL_CALL, content);
    }

    /**
     * 创建结束标识块
     */
    public static StreamChunk done() {
        return new StreamChunk(ChunkType.DONE, null);
    }

    /**
     * 判断是否为有效内容块
     */
    public boolean hasContent() {
        return content != null && !content.isEmpty();
    }
}
