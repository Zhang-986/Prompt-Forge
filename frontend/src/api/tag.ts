import request from './request'

export interface Tag {
    id: number
    name: string
    color: string
    createdAt: string
}

export interface CreateTagRequest {
    name: string
    color?: string
}

// API 响应类型
interface Result<T> {
    code: number
    data: T
    message: string
}

// 获取工作空间所有标签
export function getTags(workspaceId: number = 1) {
    return request.get<any, Result<Tag[]>>(`/tags?workspaceId=${workspaceId}`)
}

// 创建标签
export function createTag(data: CreateTagRequest, workspaceId: number = 1) {
    return request.post<any, Result<Tag>>(`/tags?workspaceId=${workspaceId}`, data)
}

// 删除标签
export function deleteTag(tagId: number) {
    return request.delete<any, Result<void>>(`/tags/${tagId}`)
}

// 获取 Prompt 的标签
export function getPromptTags(promptId: number) {
    return request.get<any, Result<Tag[]>>(`/tags/prompt/${promptId}`)
}

// 为 Prompt 添加标签
export function addTagToPrompt(promptId: number, tagId: number) {
    return request.post<any, Result<void>>(`/tags/prompt/${promptId}/tag/${tagId}`)
}

// 移除 Prompt 的标签
export function removeTagFromPrompt(promptId: number, tagId: number) {
    return request.delete<any, Result<void>>(`/tags/prompt/${promptId}/tag/${tagId}`)
}

// 批量设置 Prompt 的标签
export function setPromptTags(promptId: number, tagIds: number[]) {
    return request.put<any, Result<void>>(`/tags/prompt/${promptId}`, tagIds)
}

// 预定义颜色
export const TAG_COLORS = [
    '#5e6ad2',  // Purple (default)
    '#10b981',  // Green
    '#3b82f6',  // Blue
    '#f59e0b',  // Orange
    '#ef4444',  // Red
    '#ec4899',  // Pink
    '#06b6d4',  // Cyan
    '#6b7280',  // Gray
]
