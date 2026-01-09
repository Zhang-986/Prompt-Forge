package com.zzk.infrastructure.ai.tool;

import com.zzk.domain.repository.PromptRepository;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Prompt 模板库工具
 * 
 * <p>
 * 检索内部 Prompt 模板库，找到相似或可参考的模板
 * 
 * @author zzk
 * @since 1.0.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PromptLibraryTool {

    private final PromptRepository promptRepository;

    /**
     * 搜索内部 Prompt 模板库
     * 
     * @param keyword 搜索关键词
     * @return 匹配的模板列表
     */
    @Tool(description = "搜索内部 Prompt 模板库，查找相似或可参考的优质模板。当需要参考已有模板或用户想复用现有 Prompt 时使用。")
    public String searchTemplates(String keyword) {
        log.info("[PromptLibraryTool] 搜索模板: {}", keyword);

        // TODO: 实现基于向量或关键词的模板搜索
        // 目前返回模拟结果
        return """
                找到以下相关模板 (keyword: %s):

                1. 📝 代码生成助手 (ID: 101)
                   描述: 根据需求生成高质量代码
                   评分: ⭐⭐⭐⭐⭐

                2. 📝 SQL 查询优化器 (ID: 102)
                   描述: 优化 SQL 查询性能
                   评分: ⭐⭐⭐⭐

                3. 📝 API 文档生成器 (ID: 103)
                   描述: 根据代码生成 API 文档
                   评分: ⭐⭐⭐⭐

                提示: 可以使用模板 ID 获取详细内容。
                """.formatted(keyword);
    }
}
