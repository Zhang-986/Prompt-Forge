import request from './request'

export interface PromptTemplate {
    id: number
    name: string
    description: string
    content: string
    category: string
    authorId: number | null
    authorName: string
    cloneCount: number
    isOfficial: boolean
    createdAt: string
}

export interface Prompt {
    id: number
    name: string
    description: string
}

// 分类列表
export const CATEGORIES = [
    { value: 'ALL', label: '全部', icon: '🌐' },
    { value: 'WRITING', label: '文案写作', icon: '✍️' },
    { value: 'CODING', label: '代码助手', icon: '💻' },
    { value: 'ANALYSIS', label: '数据分析', icon: '📊' },
    { value: 'ROLEPLAY', label: '角色扮演', icon: '🎭' },
    { value: 'EDUCATION', label: '教育辅导', icon: '📚' },
    { value: 'TRANSLATION', label: '翻译润色', icon: '🌍' },
    { value: 'OTHER', label: '其他', icon: '📦' },
]

// 获取模板列表
export const getTemplates = (category?: string) => {
    return request.get<any, { code: number; data: PromptTemplate[]; message: string }>('/plaza', {
        params: category ? { category } : {}
    })
}

// 获取模板详情
export const getTemplate = (id: number) => {
    return request.get<any, { code: number; data: PromptTemplate; message: string }>(`/plaza/${id}`)
}

// 克隆模板
export const cloneTemplate = (templateId: number, workspaceId: number) => {
    return request.post<any, { code: number; data: Prompt; message: string }>(`/plaza/${templateId}/clone`, {
        workspaceId
    })
}

// 发布到广场
export const publishToPlaza = (promptId: number, category: string, authorName: string) => {
    return request.post<any, { code: number; data: PromptTemplate; message: string }>('/plaza/publish', {
        promptId,
        category,
        authorName
    })
}
