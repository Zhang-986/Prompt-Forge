package com.zzk.infrastructure.persistence.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 用户偏好持久化对象
 * 
 * @author zzk
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("user_preference")
public class UserPreferencePO {

    @TableId(type = IdType.AUTO)
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
     * 常用领域（JSON 数组）
     */
    private String commonDomains;

    /**
     * 话题频率统计（JSON 对象）
     */
    private String topicFrequency;

    /**
     * 总对话次数
     */
    private Integer totalSessions;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;
}
