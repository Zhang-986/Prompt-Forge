package com.zzk.infrastructure.ai.skill;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.TypeReference;
import com.zzk.infrastructure.persistence.mapper.AgentSkillMapper;
import com.zzk.infrastructure.persistence.po.AgentSkillPO;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Skill 注册中心
 * 
 * 负责：
 * 1. 启动时加载所有 Skill 元数据（Level 1）
 * 2. 根据用户意图匹配合适的 Skill
 * 3. 生成 OpenAI 格式的 tools JSON
 * 4. 管理 SkillExecutor Bean 映射
 * 
 * @author zzk
 * @since 1.0.0
 */
@Slf4j
@Component
public class SkillRegistry {

    private final AgentSkillMapper skillMapper;
    private final Map<String, SkillExecutor> executorMap = new ConcurrentHashMap<>();
    private final Map<String, SkillMetadata> metadataCache = new ConcurrentHashMap<>();

    public SkillRegistry(AgentSkillMapper skillMapper, List<SkillExecutor> executors) {
        this.skillMapper = skillMapper;
        // 注册所有执行器
        executors.forEach(e -> {
            executorMap.put(e.getName(), e);
            log.info("[SkillRegistry] 注册执行器: {}", e.getName());
        });
        log.info("[SkillRegistry] 共注册 {} 个执行器", executorMap.size());
    }

    /**
     * 启动时从数据库加载所有 Skill 元数据
     */
    @PostConstruct
    public void loadMetadata() {
        try {
            List<AgentSkillPO> skills = skillMapper.selectMetadataOnly();
            if (skills == null || skills.isEmpty()) {
                log.warn("[SkillRegistry] 数据库中没有 Skill 数据，请检查 agent_skills 表");
                return;
            }

            skills.forEach(po -> {
                SkillMetadata metadata = toMetadata(po);
                metadataCache.put(po.getName(), metadata);
            });

            log.info("[SkillRegistry] 加载了 {} 个 Skill 元数据: {}",
                    metadataCache.size(),
                    metadataCache.keySet());
        } catch (Exception e) {
            log.error("[SkillRegistry] 加载 Skill 元数据失败", e);
        }
    }

    /**
     * 根据用户意图选择 Skill（三层加载的入口）
     * 
     * @param userMessage 用户消息
     * @return 匹配到的 Skill 列表
     */
    public List<SkillMetadata> selectByIntent(String userMessage) {
        List<SkillMetadata> selected = new ArrayList<>();

        if (metadataCache.isEmpty()) {
            log.warn("[SkillRegistry] 元数据缓存为空，无法匹配 Skill");
            return selected;
        }

        String lowerMessage = userMessage.toLowerCase();

        // 1. CORE 技能始终加载
        metadataCache.values().stream()
                .filter(m -> "CORE".equalsIgnoreCase(m.getCategory()))
                .forEach(selected::add);

        // 2. 其他技能按关键词匹配
        metadataCache.values().stream()
                .filter(m -> !"CORE".equalsIgnoreCase(m.getCategory()))
                .filter(m -> m.getTriggerKeywords() != null && !m.getTriggerKeywords().isEmpty())
                .filter(m -> m.getTriggerKeywords().stream()
                        .anyMatch(kw -> lowerMessage.contains(kw.toLowerCase())))
                .forEach(selected::add);

        log.info("[SkillRegistry] 用户消息匹配到 {} 个 Skill: {}",
                selected.size(),
                selected.stream().map(SkillMetadata::getName).toList());

        return selected;
    }

    /**
     * 生成 OpenAI 格式的 tools JSON
     * 
     * @param skills 选中的 Skill 列表
     * @return tools JSON 数组
     */
    public List<Map<String, Object>> toOpenAiTools(List<SkillMetadata> skills) {
        return skills.stream()
                .map(skill -> {
                    Map<String, Object> function = new LinkedHashMap<>();
                    function.put("name", skill.getName());
                    function.put("description", skill.getDescription());
                    function.put("parameters", skill.getParameterSchema() != null
                            ? skill.getParameterSchema()
                            : Map.of("type", "object", "properties", Map.of()));

                    Map<String, Object> tool = new LinkedHashMap<>();
                    tool.put("type", "function");
                    tool.put("function", function);
                    return tool;
                })
                .toList();
    }

    /**
     * 获取执行器
     */
    public SkillExecutor getExecutor(String skillName) {
        return executorMap.get(skillName);
    }

    /**
     * 加载 Level 2 Instructions（按需调用）
     */
    public String loadInstructions(String skillName) {
        AgentSkillPO skill = skillMapper.selectByName(skillName);
        return skill != null ? skill.getInstructions() : "";
    }

    /**
     * 获取所有已加载的 Skill 名称
     */
    public Set<String> getAllSkillNames() {
        return metadataCache.keySet();
    }

    /**
     * 检查是否有可用的 Skill
     */
    public boolean hasSkills() {
        return !metadataCache.isEmpty();
    }

    /**
     * 将 PO 转换为 Metadata
     */
    private SkillMetadata toMetadata(AgentSkillPO po) {
        // 解析 trigger_keywords JSON
        List<String> keywords = new ArrayList<>();
        if (po.getTriggerKeywords() != null && !po.getTriggerKeywords().isBlank()) {
            try {
                keywords = JSON.parseObject(po.getTriggerKeywords(), new TypeReference<List<String>>() {
                });
            } catch (Exception e) {
                log.warn("[SkillRegistry] 解析 trigger_keywords 失败: {}", po.getName());
            }
        }

        // 解析 parameter_schema JSON
        Map<String, Object> schema = new LinkedHashMap<>();
        if (po.getParameterSchema() != null && !po.getParameterSchema().isBlank()) {
            try {
                schema = JSON.parseObject(po.getParameterSchema(), new TypeReference<Map<String, Object>>() {
                });
            } catch (Exception e) {
                log.warn("[SkillRegistry] 解析 parameter_schema 失败: {}", po.getName());
            }
        }

        return SkillMetadata.builder()
                .name(po.getName())
                .displayName(po.getDisplayName())
                .description(po.getDescription())
                .triggerKeywords(keywords)
                .category(po.getCategory())
                .executorBean(po.getExecutorBean())
                .parameterSchema(schema)
                .build();
    }
}
