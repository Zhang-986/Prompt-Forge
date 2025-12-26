import request from './request'

export interface Prompt {
    id: number
    name: string
    description: string
    workspaceId: number
    latestVersionId: number
    creatorId: number
    isPublic: boolean
    status: number
    createdAt: string
    updatedAt: string
}

export interface PromptVersion {
    id: number
    promptId: number
    versionNumber: number
    content: string
    variables: Record<string, any>
    parentId: number | null
    commitMessage: string
    authorId: number
    contentHash: string
    createdAt: string
}

export interface CreatePromptData {
    name: string
    description: string
    content: string
    workspaceId: number
}

export interface CommitVersionData {
    content: string
    parentVersionId: number
    commitMessage: string
}

// 获取 Prompt 列表
export const getPrompts = (workspaceId: number) => {
    return request.get<any, { code: number; data: Prompt[]; message: string }>('/prompts', {
        params: { workspaceId }
    })
}

// 获取 Prompt 详情
export const getPrompt = (id: number) => {
    return request.get<any, { code: number; data: Prompt; message: string }>(`/prompts/${id}`)
}

// 创建 Prompt
export const createPrompt = (data: CreatePromptData) => {
    return request.post<any, { code: number; data: Prompt; message: string }>('/prompts', data)
}

// 删除 Prompt
export const deletePrompt = (id: number) => {
    return request.delete<any, { code: number; message: string }>(`/prompts/${id}`)
}

// 获取版本历史
export const getVersionHistory = (promptId: number) => {
    return request.get<any, { code: number; data: PromptVersion[]; message: string }>(`/prompts/${promptId}/versions`)
}

// 获取最新版本
export const getLatestVersion = (promptId: number) => {
    return request.get<any, { code: number; data: PromptVersion; message: string }>(`/prompts/${promptId}/latest`)
}

// 提交新版本
export const commitVersion = (promptId: number, data: CommitVersionData) => {
    return request.post<any, { code: number; data: PromptVersion; message: string }>(`/prompts/${promptId}/commit`, data)
}

// 回滚版本
export const rollbackVersion = (promptId: number, versionId: number) => {
    return request.post<any, { code: number; data: PromptVersion; message: string }>(`/prompts/${promptId}/rollback/${versionId}`)
}
