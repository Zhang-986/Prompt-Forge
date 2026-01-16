package com.zzk.infrastructure.ai.skill;

import com.zzk.infrastructure.persistence.mapper.PromptVersionMapper;
import com.zzk.infrastructure.persistence.po.PromptVersionPO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * Prompt 版本分析 Skill
 * 
 * <p>
 * 分析 Prompt 的版本变更历史，对比不同版本的差异
 * 
 * @author zzk
 * @since 1.0.0
 */
@Slf4j
@Component("versionAnalyzerExecutor")
@RequiredArgsConstructor
public class VersionAnalyzerSkill implements SkillExecutor {

    private final PromptVersionMapper versionMapper;

    @Override
    public String getName() {
        return "version-analyzer";
    }

    @Override
    public SkillResult execute(Map<String, Object> params, Map<String, Object> context) {
        Object promptIdObj = params.get("promptId");
        if (promptIdObj == null) {
            return SkillResult.error("Prompt ID 不能为空");
        }

        Long promptId;
        try {
            promptId = Long.parseLong(promptIdObj.toString());
        } catch (NumberFormatException e) {
            return SkillResult.error("Prompt ID 格式错误");
        }

        String result = analyzeVersions(promptId);
        return SkillResult.success(result);
    }

    /**
     * 分析 Prompt 版本历史
     */
    public String analyzeVersions(Long promptId) {
        log.info("[VersionAnalyzerSkill] 分析 Prompt 版本: {}", promptId);

        try {
            List<PromptVersionPO> versions = versionMapper.findByPromptIdOrderByVersionNumberDesc(promptId);

            if (versions == null || versions.isEmpty()) {
                return "未找到该 Prompt 的版本历史（ID: " + promptId + "）";
            }

            StringBuilder sb = new StringBuilder();
            sb.append("# Prompt 版本分析报告\n\n");
            sb.append("**Prompt ID**: ").append(promptId).append("\n");
            sb.append("**版本数量**: ").append(versions.size()).append("\n\n");
            sb.append("## 版本列表\n\n");

            for (int i = 0; i < versions.size(); i++) {
                PromptVersionPO v = versions.get(i);
                sb.append("### 版本 ").append(v.getVersionNumber()).append("\n");
                sb.append("- **创建时间**: ").append(v.getCreatedAt()).append("\n");
                sb.append("- **变更说明**: ").append(v.getCommitMessage() != null ? v.getCommitMessage() : "无")
                        .append("\n");

                String content = v.getContent();
                if (content != null && content.length() > 200) {
                    content = content.substring(0, 200) + "...";
                }
                sb.append("- **内容预览**: \n```\n").append(content).append("\n```\n\n");

                if (i > 0) {
                    PromptVersionPO prev = versions.get(i - 1);
                    int lenDiff = (v.getContent() != null ? v.getContent().length() : 0)
                            - (prev.getContent() != null ? prev.getContent().length() : 0);
                    String diffDesc = lenDiff > 0 ? "增加了 " + lenDiff + " 字符"
                            : lenDiff < 0 ? "减少了 " + (-lenDiff) + " 字符"
                                    : "长度未变";
                    sb.append("📊 **与上一版对比**: ").append(diffDesc).append("\n\n");
                }
            }

            sb.append("---\n💡 提示: 如需回滚到某个版本，请告诉我版本号。");
            return sb.toString();

        } catch (Exception e) {
            log.error("[VersionAnalyzerSkill] 分析失败: {}", e.getMessage(), e);
            return "分析版本时发生错误: " + e.getMessage();
        }
    }
}
