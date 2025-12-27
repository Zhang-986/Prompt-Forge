import axios, { AxiosError } from 'axios'
import { ElMessage, ElNotification } from 'element-plus'

// 错误码映射
const ERROR_MESSAGES: Record<number, string> = {
    400: '请求参数错误',
    401: '未登录或登录已过期',
    403: '无权限访问',
    404: '请求的资源不存在',
    405: '请求方法不支持',
    429: '请求过于频繁，请稍后重试',
    500: '服务器内部错误',
    503: '服务暂时不可用'
}

// 业务错误码
const BUSINESS_ERROR_CODES: Record<number, string> = {
    1001: '用户不存在',
    1002: '用户名已存在',
    1003: '密码错误',
    1004: 'Token 无效',
    1005: 'Token 已过期',
    2001: 'Prompt 不存在',
    2002: 'Prompt 版本不存在',
    3001: '工作空间不存在',
    3002: '无权访问该工作空间',
    4001: '未配置 AI 模型',
    4002: 'AI 调用失败',
    4003: 'AI 服务请求过于频繁'
}

// 创建 axios 实例
const request = axios.create({
    baseURL: '/api',
    timeout: 30000
})

// 请求拦截器：自动添加 Token
request.interceptors.request.use(
    (config) => {
        const token = localStorage.getItem('token')
        if (token) {
            config.headers.Authorization = `Bearer ${token}`
        }
        return config
    },
    (error) => Promise.reject(error)
)

// 响应拦截器：统一错误处理
request.interceptors.response.use(
    (response) => {
        const data = response.data
        // 业务层面的错误 (code !== 200)
        if (data && data.code && data.code !== 200) {
            const errorMessage = data.message || BUSINESS_ERROR_CODES[data.code] || '操作失败'
            ElMessage.error(errorMessage)
            // 记录 traceId 用于问题排查
            if (data.traceId) {
                console.error(`[TraceId: ${data.traceId}] ${errorMessage}`)
            }
            return Promise.reject(new Error(errorMessage))
        }
        return data
    },
    (error: AxiosError<{ code?: number; message?: string; traceId?: string }>) => {
        // 网络错误
        if (!error.response) {
            if (error.code === 'ECONNABORTED') {
                ElNotification({
                    title: '请求超时',
                    message: '服务器响应超时，请检查网络连接后重试',
                    type: 'error',
                    duration: 5000
                })
            } else {
                ElNotification({
                    title: '网络错误',
                    message: '无法连接到服务器，请检查网络连接',
                    type: 'error',
                    duration: 5000
                })
            }
            return Promise.reject(error)
        }

        const status = error.response.status
        const data = error.response.data

        // 401 未授权：清除 Token 并跳转登录
        if (status === 401) {
            localStorage.removeItem('token')
            localStorage.removeItem('user')
            ElMessage.warning('登录已过期，请重新登录')
            setTimeout(() => {
                window.location.href = '/login'
            }, 1500)
            return Promise.reject(error)
        }

        // 429 请求过于频繁
        if (status === 429) {
            ElNotification({
                title: '请求频繁',
                message: '您的操作过于频繁，请稍后再试',
                type: 'warning',
                duration: 5000
            })
            return Promise.reject(error)
        }

        // 其他 HTTP 错误
        const errorMessage = data?.message || ERROR_MESSAGES[status] || `请求失败 (${status})`
        ElMessage.error(errorMessage)

        // 记录 traceId 用于问题排查
        if (data?.traceId) {
            console.error(`[TraceId: ${data.traceId}] HTTP ${status}: ${errorMessage}`)
        }

        return Promise.reject(error)
    }
)

export default request
