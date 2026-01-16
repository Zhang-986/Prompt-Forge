import request from './request'

// Token 统计项
export interface TokenStat {
    executor_name: string
    total_tokens: number
    call_count: number
}

// 失败率统计项
export interface FailureStat {
    executor_name: string
    fail_count: number
    total_count: number
}

// 日志项
export interface ExecutionLog {
    id: number
    userId: number
    sessionId: string
    executorName: string
    actionType: 'LLM_CHAT' | 'SKILL_EXECUTION'
    durationMs: number
    status: 'SUCCESS' | 'FAILURE'
    errorMessage?: string
    totalTokens?: number
    model?: string
    createdAt: string
}

export const getMonitorStats = () => {
    return request.get<any, { code: number; data: TokenStat[]; message: string }>('/monitor/stats/token')
}

export const getFailureStats = () => {
    return request.get<any, { code: number; data: FailureStat[]; message: string }>('/monitor/stats/failure')
}

export const getRecentLogs = () => {
    return request.get<any, { code: number; data: ExecutionLog[]; message: string }>('/monitor/logs')
}
