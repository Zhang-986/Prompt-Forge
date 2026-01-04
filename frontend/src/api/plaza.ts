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

export interface PlazaCategory {
    id: number
    value: string
    label: string
    icon: string
    sortOrder: number
    isActive: boolean
}

// 默认分类列表（作为后备）
export const DEFAULT_CATEGORIES = [
    { value: 'ALL', label: '全部', icon: '🌐' },
    { value: 'WRITING', label: '文案写作', icon: '✍️' },
    { value: 'CODING', label: '代码助手', icon: '💻' },
    { value: 'ANALYSIS', label: '数据分析', icon: '📊' },
    { value: 'ROLEPLAY', label: '角色扮演', icon: '🎭' },
    { value: 'EDUCATION', label: '教育辅导', icon: '📚' },
    { value: 'TRANSLATION', label: '翻译润色', icon: '🌍' },
    { value: 'OTHER', label: '其他', icon: '📦' },
]

// 获取分类列表（从后端动态获取）
export const getCategories = () => {
    return request.get<any, { code: number; data: PlazaCategory[]; message: string }>('/plaza/categories')
}

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

// ==================== 管理员 API ====================

// 管理员 - 更新广场模板
export const updatePlazaTemplate = (id: number, data: { name: string; description: string; content: string; category: string }) => {
    return request.put<any, { code: number; data: PromptTemplate; message: string }>(`/admin/templates/${id}`, data)
}

// 管理员 - 删除广场模板
export const deletePlazaTemplate = (id: number) => {
    return request.delete<any, { code: number; message: string }>(`/admin/templates/${id}`)
}

// 管理员 - 获取所有分类
export const getAdminCategories = () => {
    return request.get<any, { code: number; data: PlazaCategory[]; message: string }>('/admin/categories')
}

// 管理员 - 创建分类
export const createCategory = (data: { value: string; label: string; icon?: string; sortOrder?: number }) => {
    return request.post<any, { code: number; data: PlazaCategory; message: string }>('/admin/categories', data)
}

// 管理员 - 更新分类
export const updateCategory = (id: number, data: { value: string; label: string; icon?: string; sortOrder?: number }) => {
    return request.put<any, { code: number; data: PlazaCategory; message: string }>(`/admin/categories/${id}`, data)
}

// 管理员 - 删除分类
export const deleteCategory = (id: number) => {
    return request.delete<any, { code: number; message: string }>(`/admin/categories/${id}`)
}
