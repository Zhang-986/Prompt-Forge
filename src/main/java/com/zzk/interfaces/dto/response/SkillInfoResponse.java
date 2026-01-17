package com.zzk.interfaces.dto.response;

import com.zzk.infrastructure.ai.skill.core.SkillMetadata;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Skill 信息响应（用于前端展示）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Skill 信息")
public class SkillInfoResponse {

    @Schema(description = "技能唯一标识")
    private String name;

    @Schema(description = "显示名称")
    private String displayName;

    @Schema(description = "技能描述")
    private String description;

    @Schema(description = "技能分类")
    private String category;

    public static SkillInfoResponse from(SkillMetadata metadata) {
        return SkillInfoResponse.builder()
                .name(metadata.getName())
                .displayName(metadata.getDisplayName())
                .description(metadata.getDescription())
                .category(metadata.getCategory())
                .build();
    }
}
