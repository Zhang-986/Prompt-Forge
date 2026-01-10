import request from './request'

/**
 * 管理员后台 API
 */

// ==================== 仪表盘 ====================

export interface DashboardStats {
    totalUsers: number
    totalWorkspaces: number
    totalPrompts: number
    publicPrompts: number
    totalArenaSessions: number
    activeUsersLast7Days: number
    totalTemplates: number
}

/**
 * 获取仪表盘统计数据
 */
export const getDashboardStats = () => {
    return request.get<any, { code: number; data: DashboardStats }>('/admin/dashboard')
}

// ==================== 用户管理 ====================

export interface WorkspaceInfo {
    id: number
    name: string
    role: string
    isOwner: boolean
}

export interface AdminUser {
    id: number
    username: string
    email: string
    avatar: string
    role: string
    status: number
    createdAt: string
    updatedAt: string
    // 新增字段
    workspaces: WorkspaceInfo[]
    promptCount: number
    arenaSessionCount: number
}

export interface PageResult<T> {
    list: T[]
    total: number
    page: number
    size: number
}

/**
 * 获取用户列表
 */
export const getUsers = (page: number = 0, size: number = 10) => {
    return request.get<any, { code: number; data: PageResult<AdminUser> }>('/admin/users', {
        params: { page, size }
    })
}

/**
 * 更新用户状态
 */
export const updateUserStatus = (userId: number, enabled: boolean) => {
    return request.put(`/admin/users/${userId}/status`, { enabled })
}

/**
 * 更新用户角色
 */
export const updateUserRole = (userId: number, role: string) => {
    return request.put(`/admin/users/${userId}/role`, { role })
}

// ==================== 工作空间管理 ====================

export interface AdminWorkspace {
    id: number
    name: string
    description: string
    ownerId: number
    createdAt: string
    updatedAt: string
}

/**
 * 获取工作空间列表
 */
export const getWorkspaces = (page: number = 0, size: number = 10) => {
    return request.get<any, { code: number; data: PageResult<AdminWorkspace> }>('/admin/workspaces', {
        params: { page, size }
    })
}

/**
 * 删除工作空间
 */
export const deleteWorkspace = (workspaceId: number) => {
    return request.delete(`/admin/workspaces/${workspaceId}`)
}

// ==================== 广场模板管理 ====================

export interface AdminTemplate {
    id: number
    name: string
    description: string
    content: string
    category: string
    authorId: number
    authorName: string
    isOfficial: boolean
    cloneCount: number
    isActive: boolean
    createdAt: string
}

/**
 * 获取广场模板列表
 */
export const getTemplates = (page: number = 0, size: number = 10) => {
    return request.get<any, { code: number; data: PageResult<AdminTemplate> }>('/admin/templates', {
        params: { page, size }
    })
}

/**
 * 删除广场模板
 */
export const deleteTemplate = (templateId: number) => {
    return request.delete(`/admin/templates/${templateId}`)
}

/**
 * 设置模板官方推荐
 */
export const setTemplateOfficial = (templateId: number, isOfficial: boolean) => {
    return request.put(`/admin/templates/${templateId}/official`, { isOfficial })
}

// ==================== Prompt 管理 ====================

export interface AdminPrompt {
    id: number
    name: string
    description: string
    workspaceId: number
    workspaceName: string
    latestVersionId: number
    creatorId: number
    creatorName: string
    isPublic: boolean
    status: number
    createdAt: string
    updatedAt: string
}

/**
 * 获取 Prompt 列表
 */
export const getPrompts = (page: number = 0, size: number = 10) => {
    return request.get<any, { code: number; data: PageResult<AdminPrompt> }>('/admin/prompts', {
        params: { page, size }
    })
}

/**
 * 删除 Prompt
 */
export const deletePrompt = (promptId: number) => {
    return request.delete(`/admin/prompts/${promptId}`)
}

// ==================== 竞技场会话管理 ====================

export interface AdminArenaSession {
    id: number
    promptVersionId: number
    finalPrompt: string
    variables: string
    models: string
    status: string
    creatorId: number
    createdAt: string
    completedAt: string
}

/**
 * 获取竞技场会话列表
 */
export const getArenaSessions = (page: number = 0, size: number = 10) => {
    return request.get<any, { code: number; data: PageResult<AdminArenaSession> }>('/admin/arena-sessions', {
        params: { page, size }
    })
}

// ==================== 登录日志管理 ====================

export interface AdminLoginLog {
    id: number
    username: string
    ipAddress: string
    geoLocation: string
    deviceFingerprint: string
    userAgent: string
    result: string
    failureReason: string
    createdAt: string
}

/**
 * 获取登录日志列表
 */
export const getLoginLogs = (page: number = 0, size: number = 10) => {
    return request.get<any, { code: number; data: PageResult<AdminLoginLog> }>('/admin/login-logs', {
        params: { page, size }
    })
}

// ==================== 模型管理 ====================

export interface AdminModelProvider {
    id: string
    name: string
    defaultBaseUrl: string
    description: string
    modelsUrl: string
    sdkType: string
    enabled: number
    sortOrder: number
    syncedAt: string
    createdAt: string
    updatedAt: string
}

export interface AdminAvailableModel {
    id: number
    providerId: string
    modelId: string
    displayName: string
    description: string
    contextWindow: number
    supportsVision: number
    supportsFunctionCall: number
    enabled: number
    sortOrder: number
    source: string
    createdAt: string
    updatedAt: string
}

export interface SyncStats {
    totalProviders: number
    enabledProviders: number
    totalModels: number
    enabledModels: number
    syncedModels: number
    manualModels: number
}

// Provider APIs

export const getModelProviders = () => {
    return request.get<any, { code: number; data: AdminModelProvider[] }>('/admin/models/providers')
}

export const createModelProvider = (data: Partial<AdminModelProvider>) => {
    return request.post<any, { code: number; data: AdminModelProvider }>('/admin/models/providers', data)
}

export const updateModelProvider = (id: string, data: Partial<AdminModelProvider>) => {
    return request.put<any, { code: number; data: AdminModelProvider }>(`/admin/models/providers/${id}`, data)
}

export const deleteModelProvider = (id: string) => {
    return request.delete(`/admin/models/providers/${id}`)
}

export const toggleModelProvider = (id: string) => {
    return request.patch<any, { code: number; data: AdminModelProvider }>(`/admin/models/providers/${id}/toggle`)
}

// Model APIs

export const getAvailableModels = (providerId?: string) => {
    return request.get<any, { code: number; data: AdminAvailableModel[] }>('/admin/models', {
        params: { providerId }
    })
}

export const createAvailableModel = (data: Partial<AdminAvailableModel>) => {
    return request.post<any, { code: number; data: AdminAvailableModel }>('/admin/models', data)
}

export const updateAvailableModel = (id: number, data: Partial<AdminAvailableModel>) => {
    return request.put<any, { code: number; data: AdminAvailableModel }>(`/admin/models/${id}`, data)
}

export const deleteAvailableModel = (id: number) => {
    return request.delete(`/admin/models/${id}`)
}

export const toggleAvailableModel = (id: number) => {
    return request.patch<any, { code: number; data: AdminAvailableModel }>(`/admin/models/${id}/toggle`)
}

// Sync APIs

export const syncFromLobeChat = () => {
    return request.post<any, { code: number; data: any }>('/admin/models/sync')
}

export const getSyncStats = () => {
    return request.get<any, { code: number; data: SyncStats }>('/admin/models/sync/stats')
}

