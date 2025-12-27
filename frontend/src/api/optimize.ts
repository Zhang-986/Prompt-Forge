import request from './request'

interface Result<T> {
    code: number
    data: T
    message: string
}

export function optimizePrompt(content: string, modelId?: string) {
    return request.post<any, Result<string>>('/optimize', {
        originalContent: content,
        modelId: modelId
    })
}
