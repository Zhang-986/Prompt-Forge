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
    const baseUrl = import.meta.env.VITE_API_BASE_URL || '/api'
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
