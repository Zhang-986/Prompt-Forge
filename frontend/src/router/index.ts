import { createRouter, createWebHistory } from 'vue-router'

const router = createRouter({
    history: createWebHistory(),
    routes: [
        {
            path: '/login',
            name: 'Login',
            component: () => import('../views/Login.vue'),
            meta: { public: true }
        },
        {
            path: '/',
            component: () => import('../layouts/MainLayout.vue'),
            redirect: '/prompts',
            children: [
                {
                    path: 'prompts',
                    name: 'Prompts',
                    component: () => import('../views/PromptList.vue')
                },
                {
                    path: 'prompts/:id/versions',
                    name: 'VersionHistory',
                    component: () => import('../views/VersionHistory.vue')
                },
                {
                    path: 'arena',
                    name: 'Arena',
                    component: () => import('../views/Arena.vue')
                },
                {
                    path: 'arena/history',
                    name: 'ArenaHistory',
                    component: () => import('../views/ArenaHistory.vue')
                },
                {
                    path: 'settings/models',
                    name: 'ModelSettings',
                    component: () => import('../views/ModelSettings.vue')
                },
                {
                    path: 'settings/profile',
                    name: 'UserProfile',
                    component: () => import('../views/UserProfile.vue')
                },
                {
                    path: 'plaza',
                    name: 'Plaza',
                    component: () => import('../views/PromptPlaza.vue')
                },
                {
                    path: 'coach',
                    name: 'PromptCoach',
                    component: () => import('../views/PromptCoach.vue')
                },
                {
                    path: 'admin',
                    name: 'Admin',
                    component: () => import('../views/AdminDashboard.vue'),
                    meta: { requiresAdmin: true }
                },
                {
                    path: 'admin/monitor',
                    name: 'AgentMonitor',
                    component: () => import('../views/AgentMonitor.vue'),
                    meta: { requiresAdmin: true }
                }
            ]
        }
    ]
})

// 路由守卫
router.beforeEach((to, _from, next) => {
    const token = localStorage.getItem('token')

    // 公开页面直接放行
    if (to.meta.public) {
        next()
        return
    }

    // 需要登录的页面
    if (!token) {
        next({ name: 'Login', query: { redirect: to.fullPath } })
        return
    }

    // 需要管理员权限的页面
    if (to.meta.requiresAdmin) {
        try {
            const userStr = localStorage.getItem('user')
            if (userStr) {
                const user = JSON.parse(userStr)
                if (user.role !== 'ADMIN') {
                    next({ name: 'Prompts' })
                    return
                }
            } else {
                next({ name: 'Login' })
                return
            }
        } catch {
            next({ name: 'Login' })
            return
        }
    }

    next()
})

export default router

