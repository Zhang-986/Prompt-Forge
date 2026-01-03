import request from './request'

export interface ModelInfo {
    id: string
    name: string
    description: string
}

export interface ProviderInfo {
    id: string
    name: string
    defaultBaseUrl: string
    defaultModel: string
    models: ModelInfo[]
}

export interface ModelConfig {
    id: number
    userId: number
    provider: string
    apiKey: string
    baseUrl: string
    modelName: string
    enabled: boolean
    createdAt: string
    updatedAt: string
}

export interface CreateConfigRequest {
    provider: string
    apiKey: string
    baseUrl?: string
    modelName?: string
}

export interface UpdateConfigRequest {
    apiKey?: string
    baseUrl?: string
    modelName?: string
    enabled?: boolean
}

/**
 * 获取支持的提供商列表
 */
export function getProviders() {
    return request.get<any, { code: number; data: ProviderInfo[]; message: string }>('/user/model-configs/providers')
}

/**
 * 获取当前用户的所有配置
 */
export function getModelConfigs() {
    return request.get<any, { code: number; data: ModelConfig[]; message: string }>('/user/model-configs')
}

/**
 * 获取当前用户启用的配置
 */
export function getEnabledConfigs() {
    return request.get<any, { code: number; data: ModelConfig[]; message: string }>('/user/model-configs/enabled')
}

/**
 * 创建配置
 */
export function createConfig(data: CreateConfigRequest) {
    return request.post<any, { code: number; data: ModelConfig; message: string }>('/user/model-configs', data)
}

/**
 * 更新配置
 */
export function updateConfig(id: number, data: UpdateConfigRequest) {
    return request.put<any, { code: number; data: ModelConfig; message: string }>(`/user/model-configs/${id}`, data)
}

/**
 * 删除配置
 */
export function deleteConfig(id: number) {
    return request.delete<any, { code: number; message: string }>(`/user/model-configs/${id}`)
}

/**
 * 切换启用状态
 */
export function toggleConfig(id: number) {
    return request.post<any, { code: number; data: ModelConfig; message: string }>(`/user/model-configs/${id}/toggle`)
}


