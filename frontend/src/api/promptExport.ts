import request from './request'

// API 响应类型
interface Result<T> {
    code: number
    data: T
    message: string
}

// 导出 Prompt 为 JSON (下载) - 使用认证
export async function exportPrompt(promptId: number) {
    const token = localStorage.getItem('token')
    const response = await fetch(`https://api.nmcp.tech/api/prompts/${promptId}/export`, {
        method: 'GET',
        headers: {
            'Authorization': `Bearer ${token}`
        }
    })

    if (!response.ok) {
        throw new Error('导出失败')
    }

    // 获取 blob 并创建下载链接
    const blob = await response.blob()
    const url = window.URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = `prompt_${promptId}.json`
    document.body.appendChild(a)
    a.click()
    window.URL.revokeObjectURL(url)
    document.body.removeChild(a)
}

// 导入 Prompt (文件)
export function importPromptFile(file: File, workspaceId: number = 1) {
    const formData = new FormData()
    formData.append('file', file)
    return request.post<any, Result<{ id: number; name: string; message: string }>>(
        `/prompts/import?workspaceId=${workspaceId}`,
        formData,
        {
            headers: {
                'Content-Type': 'multipart/form-data'
            }
        }
    )
}

// 导入 Prompt (JSON 字符串)
export function importPromptJson(json: string, workspaceId: number = 1) {
    return request.post<any, Result<{ id: number; name: string; message: string }>>(
        `/prompts/import/json?workspaceId=${workspaceId}`,
        json,
        {
            headers: {
                'Content-Type': 'application/json'
            }
        }
    )
}
