import request from './request'

export interface LoginData {
    username: string
    password: string
}

export interface RegisterData {
    username: string
    email: string
    password: string
}

export interface User {
    id: number
    username: string
    email: string
    role: string
}

export interface LoginResult {
    token: string
    user: User
}

// 登录
export const login = (data: LoginData) => {
    return request.post<any, { code: number; data: LoginResult; message: string }>('/users/login', data)
}

// 注册
export const register = (data: RegisterData) => {
    return request.post<any, { code: number; data: LoginResult; message: string }>('/users/register', data)
}

// 获取当前用户
export const getCurrentUser = () => {
    return request.get<any, { code: number; data: User; message: string }>('/users/me')
}

// 退出登录
export const logout = () => {
    return request.post('/users/logout')
}

// 搜索用户（用于邀请成员）
export const searchUser = (username: string) => {
    return request.get<any, { code: number; data: User; message: string }>('/users/search', {
        params: { username }
    })
}
