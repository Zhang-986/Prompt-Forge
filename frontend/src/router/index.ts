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
