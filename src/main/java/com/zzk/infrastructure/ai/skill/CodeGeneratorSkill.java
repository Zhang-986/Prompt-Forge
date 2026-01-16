package com.zzk.infrastructure.ai.skill;

import com.zzk.domain.model.entity.UserModelConfig;
import com.zzk.infrastructure.ai.factory.DynamicLlmClientFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.stream.Collectors;

/**
 * Python 代码生成 Skill
 * 
 * <p>
 * 根据用户需求，调用 AI 模型生成高质量的 Python 代码
 * 
 * @author zzk
 * @since 1.0.0
 */
@Slf4j
@Component("codeGeneratorExecutor")
@RequiredArgsConstructor
public class CodeGeneratorSkill implements SkillExecutor {

    private final DynamicLlmClientFactory llmClientFactory;

    @Value("${ai.default.provider:openai}")
    private String defaultProvider;

    @Value("${ai.default.base-url:https://api.openai.com/v1}")
    private String defaultBaseUrl;

    @Value("${ai.default.api-key:}")
    private String defaultApiKey;

    @Value("${ai.default.model:gpt-3.5-turbo}")
    private String defaultModel;

    @Override
    public String getName() {
        return "code-generator";
    }

    @Override
    public SkillResult execute(Map<String, Object> params, Map<String, Object> context) {
        String requirement = (String) params.get("requirement");
        if (requirement == null || requirement.isBlank()) {
            return SkillResult.error("代码需求描述不能为空");
        }
        String result = generatePythonCode(requirement);
        return SkillResult.success(result);
    }

    /**
     * 根据用户需求生成 Python 代码
     */
    public String generatePythonCode(String requirement) {
        log.info("[CodeGeneratorSkill] 生成代码需求: {}", requirement);

        if (defaultApiKey == null || defaultApiKey.isEmpty()) {
            return "⚠️ 系统未配置默认 AI API Key，无法生成代码。请联系管理员配置 `ai.default.api-key`。";
        }

        try {
            UserModelConfig config = UserModelConfig.builder()
                    .provider(defaultProvider)
                    .baseUrl(defaultBaseUrl)
                    .apiKey(defaultApiKey)
                    .modelName(defaultModel)
                    .userId(0L)
                    .build();

            String systemPrompt = """
                    你是一个 Python 代码生成专家。请根据用户的需求生成高质量、可运行的 Python 代码。
                    要求：
                    1. 代码风格规范 (PEP 8)
                    2. 包含必要的注释
                    3. 如果需要第三方库，请在开头注明 pip install 命令
                    4. 直接返回代码，不要包含多余的解释
                    5. 使用 Markdown 代码块格式包裹
                    """;

            String fullPrompt = systemPrompt + "\n\n用户需求: " + requirement;

            return llmClientFactory.generateStream(config, fullPrompt)
                    .collect(Collectors.joining())
                    .block();

        } catch (Exception e) {
            log.error("[CodeGeneratorSkill] 生成失败: {}", e.getMessage(), e);
            return "生成代码时发生错误: " + e.getMessage();
        }
    }
}
