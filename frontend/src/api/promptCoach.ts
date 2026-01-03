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

    try {
        const response = await fetch('https://api.nmcp.tech/api/prompt-coach/chat', {
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

        while (true) {
            const { done, value } = await reader.read()
            if (done) break

            const text = decoder.decode(value)
            const lines = text.split('\n')

            for (const line of lines) {
                if (line.startsWith('data: ')) {
                    const data = line.slice(6)
                    if (data === '[DONE]') {
                        onComplete()
                        return
                    }
                    // 还原换行符
                    onChunk(data.replace(/\\n/g, '\n'))
                }
            }
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
