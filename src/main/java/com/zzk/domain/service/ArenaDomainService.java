package com.zzk.domain.service;

import cn.hutool.core.util.StrUtil;
import com.zzk.infrastructure.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 竞技场领域服务
 * 
 * <p>处理 Prompt 模板渲染和变量解析
 * 
 * @author zzk
 * @since 1.0.0
 */
@Slf4j
@Service
public class ArenaDomainService {

    /**
     * 变量占位符正则：匹配 {{variableName}}
     */
    private static final Pattern VARIABLE_PATTERN = Pattern.compile("\\{\\{(\\w+)}}");

    /**
     * 渲染 Prompt 模板
     * 
     * <p>将模板中的 {{variable}} 占位符替换为实际值
     * 
     * @param template Prompt 模板
     * @param variables 变量值映射
     * @return 渲染后的 Prompt
     */
    public String renderPrompt(String template, Map<String, Object> variables) {
        if (StrUtil.isBlank(template)) {
            throw new BusinessException("Prompt 模板不能为空");
        }

        if (variables == null || variables.isEmpty()) {
            log.debug("无变量，返回原始模板");
            return template;
        }

        // 解析模板中的变量
        Set<String> requiredVariables = parseVariables(template);
        log.debug("模板变量: {}", requiredVariables);

        // 检查必填变量
        for (String varName : requiredVariables) {
            if (!variables.containsKey(varName)) {
                throw new BusinessException("缺少必填变量: " + varName);
            }
        }

        // 渲染模板
        String result = template;
        for (Map.Entry<String, Object> entry : variables.entrySet()) {
            String placeholder = "{{" + entry.getKey() + "}}";
            String value = String.valueOf(entry.getValue());
            result = result.replace(placeholder, value);
        }

        log.debug("渲染结果: {}", result);
        return result;
    }

    /**
     * 解析模板中的变量名
     * 
     * @param template Prompt 模板
     * @return 变量名集合
     */
    public Set<String> parseVariables(String template) {
        Set<String> variables = new HashSet<>();
        
        if (StrUtil.isBlank(template)) {
            return variables;
        }

        Matcher matcher = VARIABLE_PATTERN.matcher(template);
        while (matcher.find()) {
            variables.add(matcher.group(1));
        }

        return variables;
    }

    /**
     * 验证变量值是否符合定义
     * 
     * @param variableDefinitions 变量定义 (JSON)
     * @param variables 变量值映射
     * @return 验证结果
     */
    public boolean validateVariables(String variableDefinitions, Map<String, Object> variables) {
        // TODO: 根据变量定义校验变量值类型和约束
        return true;
    }
}
