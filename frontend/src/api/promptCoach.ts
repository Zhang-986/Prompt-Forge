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
    message: string | null  // null 表示首次对话，使用 session 中的 initialInput
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
    const baseUrl = import.meta.env.VITE_API_BASE_URL || '/api'

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
        let currentEvent: string | null = null

        while (true) {
            const { done, value } = await reader.read()
            if (done) break

            buffer += decoder.decode(value, { stream: true })
            const lines = buffer.split('\n')

            buffer = lines.pop() || ''

            for (const line of lines) {
                if (line.trim() === '') {
                    currentEvent = null
                    continue
                }

                // 解析 event: 行
                if (line.startsWith('event:')) {
                    currentEvent = line.slice(6).trim()
                    continue
                }

                // 解析 data: 行
                if (line.startsWith('data:')) {
                    let data = line.slice(5)
                    if (data.startsWith(' ')) {
                        data = data.slice(1)
                    }

                    if (data === '[DONE]') {
                        onComplete()
                        return
                    }

                    // 传递事件类型和数据
                    // 支持的事件类型: THOUGHT, TOOL_START, TOOL_END, REASONING, CONTENT
                    if (currentEvent) {
                        onChunk(`__EVENT__:${currentEvent}:${data}`)
                    } else {
                        onChunk(data.replace(/\\n/g, '\n'))
                    }
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
