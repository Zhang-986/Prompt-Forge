import { createRouter, createWebHistory } from 'vue-router'

const router = createRouter({
    history: createWebHistory(),
    routes: [
        {
            path: '/',
            redirect: '/prompts'
        },
        {
            path: '/login',
            name: 'Login',
            component: () => import('../views/Login.vue'),
            meta: { public: true }
        },
        {
            path: '/prompts',
            name: 'Prompts',
            component: () => import('../views/PromptList.vue')
        },
        {
            path: '/prompts/:id/versions',
            name: 'VersionHistory',
            component: () => import('../views/VersionHistory.vue')
        },
        {
            path: '/arena',
            name: 'Arena',
            component: () => import('../views/Arena.vue')
        },
        {
            path: '/arena/history',
            name: 'ArenaHistory',
            component: () => import('../views/ArenaHistory.vue')
        },
        {
            path: '/settings/models',
            name: 'ModelSettings',
            component: () => import('../views/ModelSettings.vue')
        },
        {
            path: '/plaza',
            name: 'Plaza',
            component: () => import('../views/PromptPlaza.vue')
        },
        {
            path: '/coach',
            name: 'PromptCoach',
            component: () => import('../views/PromptCoach.vue')
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

    next()
})

export default router
