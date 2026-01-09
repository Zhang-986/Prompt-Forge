import request from './request'

// 可用模型信息
export interface AvailableModelInfo {
    provider: string      // 提供商 ID，如 "cloudflare"
    modelId: string       // 完整标识，如 "cloudflare:@cf/meta/llama-3.3-70b-instruct-fp8-fast"
    modelName: string     // 原始模型名，如 "@cf/meta/llama-3.3-70b-instruct-fp8-fast"
    displayName: string   // 显示名，如 "Cloudflare - Llama 3.3 70B"
}

// 获取可用的模型列表（返回详细信息）
export const getAvailableModels = () => {
    return request.get<any, { code: number; data: AvailableModelInfo[]; message: string }>('/arena/models')
}

// 构建竞技场 SSE 连接 URL
export const buildCompeteUrl = (params: {
    promptVersionId: number
    variables: Record<string, any>
    modelIds: string[]
}): string => {
    const baseUrl = import.meta.env.VITE_API_BASE_URL || '/api'
    const queryParams = new URLSearchParams()
    queryParams.append('promptVersionId', params.promptVersionId.toString())
    queryParams.append('variables', JSON.stringify(params.variables))
    queryParams.append('modelIds', params.modelIds.join(','))
    return `${baseUrl}/arena/compete?${queryParams.toString()}`
}

export interface ArenaEvent {
    modelId?: string
    type: 'start' | 'content' | 'finish' | 'error' | 'session'
    content?: string
    sequence?: number
    finished?: boolean
    sessionId?: number
}

// 提交投票
export const submitVote = (data: {
    sessionId?: number
    winnerModel: string
    loserModel: string
}) => {
    return request.post<any, { code: number; data: null; message: string }>('/arena/vote', data)
}

// 获取排行榜
export interface LeaderboardItem {
    modelId: string
    wins: number
    losses: number
    total: number
    winRate: number
}

export const getLeaderboard = () => {
    return request.get<any, { code: number; data: LeaderboardItem[]; message: string }>('/arena/leaderboard')
}

// 投票历史项
export interface ArenaVoteHistoryItem {
    id: number
    sessionId: number
    prompt: string
    winnerModel: string
    loserModel: string
    createdAt: string
}

// 获取用户投票历史
export const getUserHistory = (params: { page: number; size: number }) => {
    return request.get<any, {
        code: number
        data: {
            records: ArenaVoteHistoryItem[]
            total: number
            current: number
            size: number
        }
        message: string
    }>('/arena/history', { params })
}

// 竞技详情 DTO
export interface ArenaSessionDetail {
    id: number
    promptVersionId: number
    finalPrompt: string
    variables: Record<string, any>
    models: string[]
    status: string
    createdAt: string
    completedAt: string
    results: {
        modelId: string
        content: string
        error?: string
        latencyMs: number
        tokensUsed: number
    }[]
}

// 获取详情
export const getSessionDetail = (sessionId: number) => {
    return request.get<any, { code: number; data: ArenaSessionDetail; message: string }>(`/arena/session/${sessionId}`)
}
