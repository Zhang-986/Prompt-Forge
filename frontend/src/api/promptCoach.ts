import request from './request'

export interface DialogTurn {
    role: 'user' | 'assistant'
    content: string
    timestamp: string
}

export interface CoachSession {
    sessionId: string
    currentPhase: string
    phaseDescription: string
    turnCount: number
    history: DialogTurn[]
    extractedInfo: Record<string, string>
    generatedPrompt: string | null
    promptGenerated: boolean
}

export interface SkillInfo {
    name: string
    displayName: string
    description: string
    category: string
}

export interface StartCoachRequest {
    initialInput: string
    provider?: string
}

export interface CoachChatRequest {
    sessionId: string
    message: string
}

export interface ConfirmPromptRequest {
    sessionId: string
    promptTemplateId: number
}

/**
 * 获取可用的 Skills 列表
 */
export const getAvailableSkills = () => {
    return request.get<any, { code: number; data: SkillInfo[]; message: string }>(
        '/prompt-coach/skills'
    )
}

/**
 * 开始 Coach 会话
 */
export const startCoachSession = (data: StartCoachRequest) => {
    return request.post<any, { code: number; data: CoachSession; message: string }>(
        '/prompt-coach/start',
        data
    )
}

/**
 * 发送消息（SSE 流式）
 */
export const sendCoachMessage = async (
    data: CoachChatRequest,
    onChunk: (chunk: string) => void,
    onComplete: () => void,
    onError: (error: Error) => void
) => {
    const token = localStorage.getItem('token')
    const baseUrl = import.meta.env.VITE_API_URL || '/api'

    try {
        const response = await fetch(`${baseUrl}/prompt-coach/chat`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'Authorization': token ? `Bearer ${token}` : ''
            },
            body: JSON.stringify(data)
        })

        if (!response.ok) {
            throw new Error(`HTTP ${response.status}`)
        }

        const reader = response.body?.getReader()
        if (!reader) {
            throw new Error('无法读取响应流')
        }

        const decoder = new TextDecoder()
        let buffer = ''

        while (true) {
            const { done, value } = await reader.read()
            if (done) break

            buffer += decoder.decode(value, { stream: true })
            const lines = buffer.split('\n')

            // 保留最后一个可能不完整的行
            buffer = lines.pop() || ''

            for (const line of lines) {
                if (line.trim() === '') continue

                // 处理 SSE 数据行 (支持 "data:" 和 "data: " 两种格式)
                if (line.startsWith('data:')) {
                    // 去掉 "data:" 前缀，然后 trim 掉可能的前导空格
                    let data = line.slice(5)
                    if (data.startsWith(' ')) {
                        data = data.slice(1)
                    }

                    if (data === '[DONE]') {
                        onComplete()
                        return
                    }
                    // 还原换行符
                    onChunk(data.replace(/\\n/g, '\n'))
                }
            }
        }

        // 处理最后剩余的 buffer
        if (buffer.startsWith('data:') && !buffer.includes('[DONE]')) {
            let data = buffer.slice(5)
            if (data.startsWith(' ')) {
                data = data.slice(1)
            }
            onChunk(data.replace(/\\n/g, '\n'))
        }

        onComplete()
    } catch (error) {
        onError(error as Error)
    }
}

/**
 * 开始 Agent Coach 会话
 */
export const startAgentSession = (data: StartCoachRequest) => {
    return request.post<any, { code: number; data: CoachSession; message: string }>(
        '/prompt-coach/agent/start',
        data
    )
}

/**
 * 发送 Agent 消息（SSE 流式）
 */
export const sendAgentMessage = async (
    data: CoachChatRequest,
    onChunk: (chunk: string) => void,
    onComplete: () => void,
    onError: (error: Error) => void
) => {
    const token = localStorage.getItem('token')
    const baseUrl = import.meta.env.VITE_API_URL || '/api'

    try {
        const response = await fetch(`${baseUrl}/prompt-coach/agent/chat`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'Authorization': token ? `Bearer ${token}` : ''
            },
            body: JSON.stringify(data)
        })

        if (!response.ok) {
            throw new Error(`HTTP ${response.status}`)
        }

        const reader = response.body?.getReader()
        if (!reader) {
            throw new Error('无法读取响应流')
        }

        const decoder = new TextDecoder()
        let buffer = ''

        while (true) {
            const { done, value } = await reader.read()
            if (done) break

            buffer += decoder.decode(value, { stream: true })
            const lines = buffer.split('\n')

            buffer = lines.pop() || ''

            for (const line of lines) {
                if (line.trim() === '') continue

                if (line.startsWith('data:')) {
                    let data = line.slice(5)
                    if (data.startsWith(' ')) {
                        data = data.slice(1)
                    }

                    if (data === '[DONE]') {
                        onComplete()
                        return
                    }
                    onChunk(data.replace(/\\n/g, '\n'))
                }
            }
        }

        if (buffer.startsWith('data:') && !buffer.includes('[DONE]')) {
            let data = buffer.slice(5)
            if (data.startsWith(' ')) {
                data = data.slice(1)
            }
            onChunk(data.replace(/\\n/g, '\n'))
        }

        onComplete()
    } catch (error) {
        onError(error as Error)
    }
}

/**
 * 获取会话状态
 */
export const getCoachSession = (sessionId: string) => {
    return request.get<any, { code: number; data: CoachSession; message: string }>(
        `/prompt-coach/session/${sessionId}`
    )
}

/**
 * 确认并保存 Prompt
 */
export const confirmPrompt = (data: ConfirmPromptRequest) => {
    return request.post<any, { code: number; data: string; message: string }>(
        '/prompt-coach/confirm',
        data
    )
}
