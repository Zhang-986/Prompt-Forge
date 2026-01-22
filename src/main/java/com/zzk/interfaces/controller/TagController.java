package com.zzk.interfaces.controller;

import com.zzk.application.service.TagAppService;
import com.zzk.interfaces.dto.request.CreateTagRequest;
import com.zzk.interfaces.dto.response.Result;
import com.zzk.interfaces.dto.response.TagDTO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 标签控制器
 * 
 * @author zzk
 * @since 1.0.0
 */
@RestController
@RequestMapping("/api/tags")
@RequiredArgsConstructor
public class TagController {

    private final TagAppService tagAppService;

    /**
     * 获取工作空间所有标签
     */
    @GetMapping
    public Result<List<TagDTO>> getTags(@RequestAttribute("userId") Long userId,
            @RequestParam Long workspaceId) {
        List<TagDTO> tags = tagAppService.getTagsByWorkspace(workspaceId);
        return Result.success(tags);
    }

    /**
     * 创建标签
     */
    @PostMapping
    public Result<TagDTO> createTag(@RequestAttribute("userId") Long userId,
            @RequestParam Long workspaceId,
            @Valid @RequestBody CreateTagRequest request) {
        TagDTO tag = tagAppService.createTag(request, userId, workspaceId);
        return Result.success(tag);
    }

    /**
     * 删除标签
     */
    @DeleteMapping("/{tagId}")
    public Result<Void> deleteTag(@RequestAttribute("userId") Long userId,
            @PathVariable Long tagId) {
        tagAppService.deleteTag(tagId);
        return Result.success(null);
    }

    /**
     * 获取 Prompt 的标签
     */
    @GetMapping("/prompt/{promptId}")
    public Result<List<TagDTO>> getPromptTags(@PathVariable Long promptId) {
        List<TagDTO> tags = tagAppService.getTagsByPromptId(promptId);
        return Result.success(tags);
    }

    /**
     * 为 Prompt 添加标签
     */
    @PostMapping("/prompt/{promptId}/tag/{tagId}")
    public Result<Void> addTagToPrompt(@PathVariable Long promptId,
            @PathVariable Long tagId) {
        tagAppService.addTagToPrompt(promptId, tagId);
        return Result.success(null);
    }

    /**
     * 移除 Prompt 的标签
     */
    @DeleteMapping("/prompt/{promptId}/tag/{tagId}")
    public Result<Void> removeTagFromPrompt(@PathVariable Long promptId,
            @PathVariable Long tagId) {
        tagAppService.removeTagFromPrompt(promptId, tagId);
        return Result.success(null);
    }

    /**
     * 批量设置 Prompt 的标签
     */
    @PutMapping("/prompt/{promptId}")
    public Result<Void> setPromptTags(@PathVariable Long promptId,
            @RequestBody List<Long> tagIds) {
        tagAppService.setPromptTags(promptId, tagIds);
        return Result.success(null);
    }

    /**
     * 批量获取工作空间内所有 Prompt 的标签映射
     * 
     * @return Map of promptId -> List of tagIds
     */
    @GetMapping("/mappings")
    public Result<java.util.Map<Long, java.util.List<Long>>> getAllPromptTagMappings(
            @RequestAttribute("userId") Long userId,
            @RequestParam Long workspaceId) {
        java.util.Map<Long, java.util.List<Long>> mappings = tagAppService.getAllPromptTagMappings(workspaceId);
        return Result.success(mappings);
    }
}
