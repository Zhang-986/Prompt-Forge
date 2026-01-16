package com.zzk.infrastructure.ai.skill;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zzk.infrastructure.persistence.mapper.PromptMapper;
import com.zzk.infrastructure.persistence.po.PromptPO;
import org.springframework.stereotype.Component;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Map;

/**
 * Prompt 库搜索 Skill
 * 
 * <p>
 * 检索内部 Prompt 模板库，找到相似或可参考的模板
 * 
 * @author zzk
 * @since 1.0.0
 */
@Slf4j
@Component("promptLibraryExecutor")
@RequiredArgsConstructor
public class PromptLibrarySkill implements SkillExecutor {

    private final PromptMapper promptMapper;
    private static final int MAX_RESULTS = 5;

    @Override
    public String getName() {
        return "prompt-library";
    }

    @Override
    public SkillResult execute(Map<String, Object> params, Map<String, Object> context) {
        String query = (String) params.get("query");
        if (query == null || query.isBlank()) {
            return SkillResult.error("搜索关键词不能为空");
        }
        String result = searchTemplates(query);
        return SkillResult.success(result);
    }

    /**
     * 搜索内部 Prompt 模板库
     */
    public String searchTemplates(String keyword) {
        log.info("[PromptLibrarySkill] 搜索模板: {}", keyword);

        try {
            LambdaQueryWrapper<PromptPO> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper
                    .eq(PromptPO::getStatus, 1)
                    .eq(PromptPO::getIsPublic, true)
                    .and(w -> w
                            .like(PromptPO::getName, keyword)
                            .or()
                            .like(PromptPO::getDescription, keyword))
                    .orderByDesc(PromptPO::getCreatedAt)
                    .last("LIMIT " + MAX_RESULTS);

            List<PromptPO> results = promptMapper.selectList(queryWrapper);

            if (results.isEmpty()) {
                return "未找到与 \"" + keyword + "\" 相关的模板。\n\n" +
                        "建议：\n- 尝试使用更通用的关键词\n- 检查关键词拼写是否正确";
            }

            StringBuilder sb = new StringBuilder();
            sb.append("找到 ").append(results.size()).append(" 个相关模板 (关键词: \"").append(keyword).append("\"):\n\n");

            for (int i = 0; i < results.size(); i++) {
                PromptPO prompt = results.get(i);
                sb.append(i + 1).append(". 📝 **").append(prompt.getName()).append("** (ID: ").append(prompt.getId())
                        .append(")\n");
                sb.append("   描述: ").append(prompt.getDescription() != null ? prompt.getDescription() : "无描述")
                        .append("\n\n");
            }

            sb.append("💡 提示: 可以参考这些模板的结构和内容来优化你的 Prompt。");
            return sb.toString();

        } catch (Exception e) {
            log.error("[PromptLibrarySkill] 搜索失败: {}", e.getMessage(), e);
            return "搜索模板时发生错误: " + e.getMessage();
        }
    }
}
