import request from './request'

export interface Workspace {
    id: number
    name: string
    description: string
    ownerId: number
    createdAt: string
    updatedAt: string
}

export interface WorkspaceMember {
    id: number
    workspaceId: number
    userId: number
    username: string
    role: 'ADMIN' | 'MEMBER' | 'VIEWER'
    createdAt: string
}

export interface CreateWorkspaceData {
    name: string
    description?: string
}

export interface UpdateWorkspaceData {
    name: string
    description?: string
}

export interface AddMemberData {
    userId: number
    role?: 'ADMIN' | 'MEMBER' | 'VIEWER'
}

// 获取用户的工作空间列表
export const getWorkspaces = () => {
    return request.get<any, { code: number; data: Workspace[]; message: string }>('/workspaces')
}

// 获取工作空间详情
export const getWorkspace = (id: number) => {
    return request.get<any, { code: number; data: Workspace; message: string }>(`/workspaces/${id}`)
}

// 创建工作空间
export const createWorkspace = (data: CreateWorkspaceData) => {
    return request.post<any, { code: number; data: Workspace; message: string }>('/workspaces', data)
}

// 更新工作空间
export const updateWorkspace = (id: number, data: UpdateWorkspaceData) => {
    return request.put<any, { code: number; data: Workspace; message: string }>(`/workspaces/${id}`, data)
}

// 删除工作空间
export const deleteWorkspace = (id: number) => {
    return request.delete<any, { code: number; message: string }>(`/workspaces/${id}`)
}

// 获取工作空间成员
export const getWorkspaceMembers = (workspaceId: number) => {
    return request.get<any, { code: number; data: WorkspaceMember[]; message: string }>(`/workspaces/${workspaceId}/members`)
}

// 添加成员
export const addWorkspaceMember = (workspaceId: number, data: AddMemberData) => {
    return request.post<any, { code: number; message: string }>(`/workspaces/${workspaceId}/members`, data)
}

// 移除成员
export const removeWorkspaceMember = (workspaceId: number, userId: number) => {
    return request.delete<any, { code: number; message: string }>(`/workspaces/${workspaceId}/members/${userId}`)
}
