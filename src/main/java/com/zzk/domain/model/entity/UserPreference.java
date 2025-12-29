package com.zzk.domain.model.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 用户偏好画像实体
 * 
 * <p>通过多轮对话收集用户的偏好信息，用于个性化推荐和更智能的引导。
 * 类似 ChatGPT 的 Memory 功能。
 * 
 * @author zzk
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserPreference {

    /**
     * 主键ID
     */
    private Long id;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 偏好的技术栈
     */
    private String preferredTechStack;

    /**
     * 偏好的输出格式
     */
    private String preferredOutputFormat;

    /**
     * 偏好的 AI 模型
     */
    private String preferredProvider;

    /**
     * 常用领域（JSON 数组格式存储）
     */
    @Builder.Default
    private List<String> commonDomains = new ArrayList<>();

    /**
     * 话题频率统计（JSON 对象格式存储）
     */
    @Builder.Default
    private Map<String, Integer> topicFrequency = new HashMap<>();

    /**
     * 总对话次数
     */
    @Builder.Default
    private int totalSessions = 0;

    /**
     * 创建时间
     */
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    /**
     * 最后更新时间
     */
    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();

    // ==================== 业务方法 ====================

    /**
     * 增加话题频率
     */
    public void incrementTopicFrequency(String topic) {
        topicFrequency.merge(topic, 1, Integer::sum);
        updatedAt = LocalDateTime.now();
    }

    /**
     * 添加常用领域
     */
    public void addDomain(String domain) {
        if (!commonDomains.contains(domain)) {
            commonDomains.add(domain);
            updatedAt = LocalDateTime.now();
        }
    }

    /**
     * 增加会话次数
     */
    public void incrementSessionCount() {
        totalSessions++;
        updatedAt = LocalDateTime.now();
    }

    /**
     * 更新技术栈偏好
     */
    public void updateTechStack(String techStack) {
        if (techStack != null && !techStack.isBlank()) {
            this.preferredTechStack = techStack;
            updatedAt = LocalDateTime.now();
        }
    }

    /**
     * 更新输出格式偏好
     */
    public void updateOutputFormat(String format) {
        if (format != null && !format.isBlank()) {
            this.preferredOutputFormat = format;
            updatedAt = LocalDateTime.now();
        }
    }

    /**
     * 创建新用户画像
     */
    public static UserPreference createNew(Long userId) {
        return UserPreference.builder()
                .userId(userId)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }
}
