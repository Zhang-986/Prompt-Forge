import request from './request'

// API 响应类型
interface Result<T> {
    code: number
    data: T
    message: string
}

// 竞技历史列表项
export interface ArenaHistoryItem {
    id: number
    promptVersionId: number
    status: string
    models: string  // JSON string
    createdAt: string
    completedAt: string | null
}

// 竞技结果
export interface ArenaResultItem {
    modelId: string
    content: string
    tokensUsed: number
    latencyMs: number
    status: string
    errorMessage: string | null
}

// 竞技历史详情
export interface ArenaHistoryDetail {
    id: number
    promptVersionId: number
    finalPrompt: string
    variables: string
    models: string
    status: string
    createdAt: string
    completedAt: string | null
    results: ArenaResultItem[]
}

// 获取竞技历史列表
export function getArenaHistory(limit: number = 20) {
    return request.get<any, Result<ArenaHistoryItem[]>>(`/arena/history?limit=${limit}`)
}

// 获取竞技详情
export function getArenaHistoryDetail(sessionId: number) {
    return request.get<any, Result<ArenaHistoryDetail>>(`/arena/history/${sessionId}`)
}
