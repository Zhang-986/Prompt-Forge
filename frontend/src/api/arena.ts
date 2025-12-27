import request from './request'

// 获取可用的模型列表
export const getAvailableModels = () => {
    return request.get<any, { code: number; data: string[]; message: string }>('/arena/models')
}

// 构建竞技场 SSE 连接 URL
export const buildCompeteUrl = (params: {
    promptVersionId: number
    variables: Record<string, any>
    modelIds: string[]
}): string => {
    const baseUrl = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api'
    const queryParams = new URLSearchParams()
    queryParams.append('promptVersionId', params.promptVersionId.toString())
    queryParams.append('variables', JSON.stringify(params.variables))
    queryParams.append('modelIds', params.modelIds.join(','))
    return `${baseUrl}/arena/compete?${queryParams.toString()}`
}

export interface ArenaEvent {
    modelId: string
    type: 'start' | 'content' | 'finish' | 'error'
    content: string
    sequence: number
    finished: boolean
}
