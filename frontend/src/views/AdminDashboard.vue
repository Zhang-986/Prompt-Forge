<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { message, Modal } from 'ant-design-vue'
import {
    DashboardOutlined,
    UserOutlined,
    FolderOutlined,
    AppstoreOutlined,
    FileTextOutlined,
    ThunderboltOutlined,
    SettingOutlined,
    DeleteOutlined,
    CheckOutlined,
    CloseOutlined,
    StarOutlined,
    StarFilled,
    HomeOutlined
} from '@ant-design/icons-vue'
import {
    getDashboardStats,
    getUsers,
    updateUserStatus,
    updateUserRole,
    getWorkspaces,
    deleteWorkspace,
    getTemplates,
    deleteTemplate,
    setTemplateOfficial,
    getPrompts,
    deletePrompt,
    getArenaSessions,
    getLoginLogs,
    type DashboardStats,
    type AdminUser,
    type AdminWorkspace,
    type AdminTemplate,
    type AdminPrompt,
    type AdminArenaSession,
    type AdminLoginLog
} from '../api/admin'

const router = useRouter()

// 当前选中的Tab
const activeTab = ref('dashboard')

// 仪表盘数据
const stats = ref<DashboardStats | null>(null)
const loadingStats = ref(false)

// 用户列表
const users = ref<AdminUser[]>([])
const usersTotal = ref(0)
const usersPage = ref(0)
const usersSize = ref(10)
const loadingUsers = ref(false)

// 工作空间列表
const workspaces = ref<AdminWorkspace[]>([])
const workspacesTotal = ref(0)
const workspacesPage = ref(0)
const workspacesSize = ref(10)
const loadingWorkspaces = ref(false)

// 模板列表
const templates = ref<AdminTemplate[]>([])
const templatesTotal = ref(0)
const templatesPage = ref(0)
const templatesSize = ref(10)
const loadingTemplates = ref(false)

// Prompt 列表
const prompts = ref<AdminPrompt[]>([])
const promptsTotal = ref(0)
const promptsPage = ref(0)
const promptsSize = ref(10)
const loadingPrompts = ref(false)

// 竞技场会话列表
const arenaSessions = ref<AdminArenaSession[]>([])
const arenaSessionsTotal = ref(0)
const arenaSessionsPage = ref(0)
const arenaSessionsSize = ref(10)
const loadingArenaSessions = ref(false)

// 登录日志列表
const loginLogs = ref<AdminLoginLog[]>([])
const loginLogsTotal = ref(0)
const loginLogsPage = ref(0)
const loginLogsSize = ref(10)
const loadingLoginLogs = ref(false)

// 获取当前用户
const currentUser = ref<any>(null)
try {
    const userStr = localStorage.getItem('user')
    if (userStr) {
        currentUser.value = JSON.parse(userStr)
    }
} catch {}

// 加载仪表盘数据
const loadDashboardStats = async () => {
    loadingStats.value = true
    try {
        const res = await getDashboardStats()
        if (res.code === 200) {
            stats.value = res.data
        }
    } catch (error) {
        console.error('加载仪表盘数据失败:', error)
    } finally {
        loadingStats.value = false
    }
}

// 加载用户列表
const loadUsers = async () => {
    loadingUsers.value = true
    try {
        const res = await getUsers(usersPage.value, usersSize.value)
        if (res.code === 200) {
            users.value = res.data.list
            usersTotal.value = res.data.total
        }
    } catch (error) {
        console.error('加载用户列表失败:', error)
    } finally {
        loadingUsers.value = false
    }
}

// 加载工作空间列表
const loadWorkspaces = async () => {
    loadingWorkspaces.value = true
    try {
        const res = await getWorkspaces(workspacesPage.value, workspacesSize.value)
        if (res.code === 200) {
            workspaces.value = res.data.list
            workspacesTotal.value = res.data.total
        }
    } catch (error) {
        console.error('加载工作空间列表失败:', error)
    } finally {
        loadingWorkspaces.value = false
    }
}

// 加载模板列表
const loadTemplates = async () => {
    loadingTemplates.value = true
    try {
        const res = await getTemplates(templatesPage.value, templatesSize.value)
        if (res.code === 200) {
            templates.value = res.data.list
            templatesTotal.value = res.data.total
        }
    } catch (error) {
        console.error('加载模板列表失败:', error)
    } finally {
        loadingTemplates.value = false
    }
}

// 加载 Prompt 列表
const loadPrompts = async () => {
    loadingPrompts.value = true
    try {
        const res = await getPrompts(promptsPage.value, promptsSize.value)
        if (res.code === 200) {
            prompts.value = res.data.list
            promptsTotal.value = res.data.total
        }
    } catch (error) {
        console.error('加载Prompt列表失败:', error)
    } finally {
        loadingPrompts.value = false
    }
}

// 加载竞技场会话列表
const loadArenaSessions = async () => {
    loadingArenaSessions.value = true
    try {
        const res = await getArenaSessions(arenaSessionsPage.value, arenaSessionsSize.value)
        if (res.code === 200) {
            arenaSessions.value = res.data.list
            arenaSessionsTotal.value = res.data.total
        }
    } catch (error) {
        console.error('加载竞技场会话列表失败:', error)
    } finally {
        loadingArenaSessions.value = false
    }
}

// 加载登录日志列表
const loadLoginLogs = async () => {
    loadingLoginLogs.value = true
    try {
        const res = await getLoginLogs(loginLogsPage.value, loginLogsSize.value)
        if (res.code === 200) {
            loginLogs.value = res.data.list
            loginLogsTotal.value = res.data.total
        }
    } catch (error) {
        console.error('加载登录日志列表失败:', error)
    } finally {
        loadingLoginLogs.value = false
    }
}

// 切换Tab
const handleTabChange = (tab: string) => {
    activeTab.value = tab
    if (tab === 'dashboard') {
        loadDashboardStats()
    } else if (tab === 'users') {
        loadUsers()
    } else if (tab === 'workspaces') {
        loadWorkspaces()
    } else if (tab === 'templates') {
        loadTemplates()
    } else if (tab === 'prompts') {
        loadPrompts()
    } else if (tab === 'arena') {
        loadArenaSessions()
    } else if (tab === 'logs') {
        loadLoginLogs()
    }
}

// 用户状态切换
const handleToggleUserStatus = async (user: AdminUser) => {
    const newStatus = user.status === 1 ? false : true
    const action = newStatus ? '启用' : '禁用'
    
    Modal.confirm({
        title: `确认${action}用户？`,
        content: `确定要${action}用户 "${user.username}" 吗？`,
        okText: '确认',
        cancelText: '取消',
        onOk: async () => {
            try {
                await updateUserStatus(user.id, newStatus)
                message.success(`${action}成功`)
                loadUsers()
            } catch (error) {
                console.error(`${action}用户失败:`, error)
            }
        }
    })
}

// 修改用户角色
const handleChangeRole = async (user: AdminUser, newRole: string) => {
    try {
        await updateUserRole(user.id, newRole)
        message.success('角色修改成功')
        loadUsers()
    } catch (error) {
        console.error('修改角色失败:', error)
    }
}

// 删除工作空间
const handleDeleteWorkspace = async (workspace: AdminWorkspace) => {
    Modal.confirm({
        title: '确认删除工作空间？',
        content: `确定要删除工作空间 "${workspace.name}" 吗？此操作不可恢复！`,
        okText: '确认删除',
        okType: 'danger',
        cancelText: '取消',
        onOk: async () => {
            try {
                await deleteWorkspace(workspace.id)
                message.success('删除成功')
                loadWorkspaces()
            } catch (error) {
                console.error('删除工作空间失败:', error)
            }
        }
    })
}

// 删除模板
const handleDeleteTemplate = async (template: AdminTemplate) => {
    Modal.confirm({
        title: '确认删除模板？',
        content: `确定要删除模板 "${template.name}" 吗？`,
        okText: '确认删除',
        okType: 'danger',
        cancelText: '取消',
        onOk: async () => {
            try {
                await deleteTemplate(template.id)
                message.success('删除成功')
                loadTemplates()
            } catch (error) {
                console.error('删除模板失败:', error)
            }
        }
    })
}

// 设置官方推荐
const handleToggleOfficial = async (template: AdminTemplate) => {
    const newStatus = !template.isOfficial
    const action = newStatus ? '设为官方推荐' : '取消官方推荐'
    
    try {
        await setTemplateOfficial(template.id, newStatus)
        message.success(`${action}成功`)
        loadTemplates()
    } catch (error) {
        console.error(`${action}失败:`, error)
    }
}

// 分页变化
const handleUsersPageChange = (page: number) => {
    usersPage.value = page - 1
    loadUsers()
}

const handleWorkspacesPageChange = (page: number) => {
    workspacesPage.value = page - 1
    loadWorkspaces()
}

const handleTemplatesPageChange = (page: number) => {
    templatesPage.value = page - 1
    loadTemplates()
}

const handlePromptsPageChange = (page: number) => {
    promptsPage.value = page - 1
    loadPrompts()
}

const handleArenaSessionsPageChange = (page: number) => {
    arenaSessionsPage.value = page - 1
    loadArenaSessions()
}

const handleLoginLogsPageChange = (page: number) => {
    loginLogsPage.value = page - 1
    loadLoginLogs()
}

// 删除 Prompt
const handleDeletePrompt = async (prompt: AdminPrompt) => {
    Modal.confirm({
        title: '确认删除Prompt？',
        content: `确定要删除Prompt "${prompt.name}" 吗？`,
        okText: '确认',
        cancelText: '取消',
        onOk: async () => {
            try {
                await deletePrompt(prompt.id)
                message.success('删除成功')
                loadPrompts()
            } catch (error) {
                console.error('删除Prompt失败:', error)
            }
        }
    })
}

// 退出登录
const handleLogout = () => {
    localStorage.removeItem('token')
    localStorage.removeItem('user')
    router.push('/login')
}

// 格式化日期
const formatDate = (dateStr: string) => {
    if (!dateStr) return '-'
    const date = new Date(dateStr)
    return date.toLocaleDateString('zh-CN', {
        year: 'numeric',
        month: '2-digit',
        day: '2-digit',
        hour: '2-digit',
        minute: '2-digit'
    })
}

// 解析模型 JSON 字符串
const parseModels = (modelsStr: string): string[] => {
    if (!modelsStr) return []
    try {
        const parsed = JSON.parse(modelsStr)
        return Array.isArray(parsed) ? parsed : [modelsStr]
    } catch {
        return modelsStr.split(',').map(s => s.trim()).filter(Boolean)
    }
}

// 角色选项
const roleOptions = [
    { value: 'ADMIN', label: '管理员' },
    { value: 'MEMBER', label: '成员' },
    { value: 'VIEWER', label: '访客' }
]

onMounted(() => {
    loadDashboardStats()
})
</script>

<template>
    <div class="admin-container">
        <!-- Header -->
        <header class="header">
            <div class="header-left">
                <img src="/vite.svg" alt="Logo" class="logo-icon" />
                <span class="logo-text">Prompt-Forge</span>
                <a-tag color="orange"><SettingOutlined /> 管理后台</a-tag>
            </div>
            <div class="header-right">
                <a-button @click="router.push('/prompts')">
                    <template #icon><HomeOutlined /></template>
                    返回首页
                </a-button>
                <span class="username">{{ currentUser?.username }}</span>
                <a-button @click="handleLogout">退出</a-button>
            </div>
        </header>

        <div class="main-layout">
            <!-- Sidebar -->
            <aside class="sidebar">
                <div class="menu-item" :class="{ active: activeTab === 'dashboard' }" @click="handleTabChange('dashboard')">
                    <DashboardOutlined /> 仪表盘
                </div>
                <div class="menu-item" :class="{ active: activeTab === 'users' }" @click="handleTabChange('users')">
                    <UserOutlined /> 用户管理
                </div>
                <div class="menu-item" :class="{ active: activeTab === 'workspaces' }" @click="handleTabChange('workspaces')">
                    <FolderOutlined /> 工作空间
                </div>
                <div class="menu-item" :class="{ active: activeTab === 'prompts' }" @click="handleTabChange('prompts')">
                    <FileTextOutlined /> Prompt管理
                </div>
                <div class="menu-item" :class="{ active: activeTab === 'templates' }" @click="handleTabChange('templates')">
                    <AppstoreOutlined /> 广场模板
                </div>
                <div class="menu-item" :class="{ active: activeTab === 'arena' }" @click="handleTabChange('arena')">
                    <ThunderboltOutlined /> 竞技场会话
                </div>
                <div class="menu-item" :class="{ active: activeTab === 'logs' }" @click="handleTabChange('logs')">
                    <SettingOutlined /> 登录日志
                </div>
            </aside>

            <!-- Content -->
            <main class="content">
                <!-- 仪表盘 -->
                <div v-if="activeTab === 'dashboard'" class="tab-content">
                    <h2 class="tab-title">系统概览</h2>
                    <p class="tab-desc">点击卡片可查看详细数据</p>
                    <div v-if="loadingStats" class="loading">加载中...</div>
                    <div v-else-if="stats" class="stats-container">
                        <!-- 主要数据卡片 - 大尺寸 -->
                        <div class="stats-row main-stats">
                            <div class="stat-card clickable" @click="handleTabChange('users')">
                                <div class="stat-icon users"><UserOutlined /></div>
                                <div class="stat-info">
                                    <div class="stat-value">{{ stats.totalUsers }}</div>
                                    <div class="stat-label">总用户数</div>
                                </div>
                                <div class="stat-arrow">→</div>
                            </div>
                            <div class="stat-card clickable" @click="handleTabChange('workspaces')">
                                <div class="stat-icon workspaces"><FolderOutlined /></div>
                                <div class="stat-info">
                                    <div class="stat-value">{{ stats.totalWorkspaces }}</div>
                                    <div class="stat-label">工作空间</div>
                                </div>
                                <div class="stat-arrow">→</div>
                            </div>
                            <div class="stat-card clickable" @click="handleTabChange('templates')">
                                <div class="stat-icon templates"><AppstoreOutlined /></div>
                                <div class="stat-info">
                                    <div class="stat-value">{{ stats.totalTemplates }}</div>
                                    <div class="stat-label">广场模板</div>
                                </div>
                                <div class="stat-arrow">→</div>
                            </div>
                        </div>
                        
                        <!-- 次要数据卡片 - 中等尺寸 -->
                        <div class="stats-row secondary-stats">
                            <div class="stat-card-sm">
                                <div class="stat-icon-sm prompts"><FileTextOutlined /></div>
                                <div class="stat-info-sm">
                                    <div class="stat-value-sm">{{ stats.totalPrompts }}</div>
                                    <div class="stat-label-sm">总 Prompt</div>
                                </div>
                            </div>
                            <div class="stat-card-sm">
                                <div class="stat-icon-sm public"><AppstoreOutlined /></div>
                                <div class="stat-info-sm">
                                    <div class="stat-value-sm">{{ stats.publicPrompts }}</div>
                                    <div class="stat-label-sm">公开 Prompt</div>
                                </div>
                            </div>
                            <div class="stat-card-sm">
                                <div class="stat-icon-sm arena"><ThunderboltOutlined /></div>
                                <div class="stat-info-sm">
                                    <div class="stat-value-sm">{{ stats.totalArenaSessions }}</div>
                                    <div class="stat-label-sm">竞技场次数</div>
                                </div>
                            </div>
                            <div class="stat-card-sm">
                                <div class="stat-icon-sm active"><UserOutlined /></div>
                                <div class="stat-info-sm">
                                    <div class="stat-value-sm">{{ stats.activeUsersLast7Days }}</div>
                                    <div class="stat-label-sm">7天活跃</div>
                                </div>
                            </div>
                        </div>
                    </div>
                </div>

                <!-- 用户管理 -->
                <div v-if="activeTab === 'users'" class="tab-content">
                    <h2 class="tab-title">用户管理</h2>
                    <div v-if="loadingUsers" class="loading">加载中...</div>
                    <div v-else>
                        <table class="data-table">
                            <thead>
                                <tr>
                                    <th>ID</th>
                                    <th>用户名</th>
                                    <th>邮箱</th>
                                    <th>角色</th>
                                    <th>所属工作空间</th>
                                    <th>Prompt数</th>
                                    <th>竞技场</th>
                                    <th>状态</th>
                                    <th>注册时间</th>
                                    <th>操作</th>
                                </tr>
                            </thead>
                            <tbody>
                                <tr v-for="user in users" :key="user.id">
                                    <td>{{ user.id }}</td>
                                    <td>{{ user.username }}</td>
                                    <td>{{ user.email }}</td>
                                    <td>
                                        <a-select
                                            :value="user.role"
                                            style="width: 100px"
                                            @change="(val: string) => handleChangeRole(user, val)"
                                            :disabled="user.role === 'ADMIN'"
                                        >
                                            <a-select-option v-for="opt in roleOptions" :key="opt.value" :value="opt.value">
                                                {{ opt.label }}
                                            </a-select-option>
                                        </a-select>
                                    </td>
                                    <td>
                                        <div v-if="user.workspaces && user.workspaces.length > 0" class="workspace-list">
                                            <a-tooltip v-for="ws in user.workspaces" :key="ws.id" :title="ws.isOwner ? '所有者 - ' + ws.role : ws.role">
                                                <a-tag :color="ws.isOwner ? 'gold' : 'blue'" class="ws-tag">
                                                    {{ ws.name }}
                                                </a-tag>
                                            </a-tooltip>
                                        </div>
                                        <span v-else class="no-data">-</span>
                                    </td>
                                    <td>{{ user.promptCount || 0 }}</td>
                                    <td>{{ user.arenaSessionCount || 0 }}</td>
                                    <td>
                                        <a-tag :color="user.status === 1 ? 'green' : 'red'">
                                            {{ user.status === 1 ? '正常' : '禁用' }}
                                        </a-tag>
                                    </td>
                                    <td>{{ formatDate(user.createdAt) }}</td>
                                    <td>
                                        <a-button
                                            v-if="user.role !== 'ADMIN'"
                                            size="small"
                                            :type="user.status === 1 ? 'default' : 'primary'"
                                            @click="handleToggleUserStatus(user)"
                                        >
                                            <template #icon>
                                                <CloseOutlined v-if="user.status === 1" />
                                                <CheckOutlined v-else />
                                            </template>
                                            {{ user.status === 1 ? '禁用' : '启用' }}
                                        </a-button>
                                        <span v-else class="admin-label">管理员</span>
                                    </td>
                                </tr>
                            </tbody>
                        </table>
                        <div class="pagination">
                            <a-pagination
                                :current="usersPage + 1"
                                :total="usersTotal"
                                :pageSize="usersSize"
                                @change="handleUsersPageChange"
                                show-quick-jumper
                            />
                        </div>
                    </div>
                </div>

                <!-- 工作空间管理 -->
                <div v-if="activeTab === 'workspaces'" class="tab-content">
                    <h2 class="tab-title">工作空间管理</h2>
                    <div v-if="loadingWorkspaces" class="loading">加载中...</div>
                    <div v-else>
                        <table class="data-table">
                            <thead>
                                <tr>
                                    <th>ID</th>
                                    <th>名称</th>
                                    <th>描述</th>
                                    <th>所有者ID</th>
                                    <th>创建时间</th>
                                    <th>操作</th>
                                </tr>
                            </thead>
                            <tbody>
                                <tr v-for="ws in workspaces" :key="ws.id">
                                    <td>{{ ws.id }}</td>
                                    <td>{{ ws.name }}</td>
                                    <td>{{ ws.description || '-' }}</td>
                                    <td>{{ ws.ownerId }}</td>
                                    <td>{{ formatDate(ws.createdAt) }}</td>
                                    <td>
                                        <a-button size="small" danger @click="handleDeleteWorkspace(ws)">
                                            <template #icon><DeleteOutlined /></template>
                                            删除
                                        </a-button>
                                    </td>
                                </tr>
                            </tbody>
                        </table>
                        <div class="pagination">
                            <a-pagination
                                :current="workspacesPage + 1"
                                :total="workspacesTotal"
                                :pageSize="workspacesSize"
                                @change="handleWorkspacesPageChange"
                                show-quick-jumper
                            />
                        </div>
                    </div>
                </div>

                <!-- 广场模板管理 -->
                <div v-if="activeTab === 'templates'" class="tab-content">
                    <h2 class="tab-title">广场模板管理</h2>
                    <div v-if="loadingTemplates" class="loading">加载中...</div>
                    <div v-else>
                        <table class="data-table">
                            <thead>
                                <tr>
                                    <th>ID</th>
                                    <th>名称</th>
                                    <th>分类</th>
                                    <th>作者</th>
                                    <th>克隆数</th>
                                    <th>官方</th>
                                    <th>状态</th>
                                    <th>创建时间</th>
                                    <th>操作</th>
                                </tr>
                            </thead>
                            <tbody>
                                <tr v-for="tpl in templates" :key="tpl.id">
                                    <td>{{ tpl.id }}</td>
                                    <td>{{ tpl.name }}</td>
                                    <td>{{ tpl.category }}</td>
                                    <td>{{ tpl.authorName || '-' }}</td>
                                    <td>{{ tpl.cloneCount }}</td>
                                    <td>
                                        <a-tag :color="tpl.isOfficial ? 'gold' : 'default'">
                                            {{ tpl.isOfficial ? '官方' : '-' }}
                                        </a-tag>
                                    </td>
                                    <td>
                                        <a-tag :color="tpl.isActive ? 'green' : 'red'">
                                            {{ tpl.isActive ? '正常' : '已删除' }}
                                        </a-tag>
                                    </td>
                                    <td>{{ formatDate(tpl.createdAt) }}</td>
                                    <td>
                                        <a-space>
                                            <a-button size="small" @click="handleToggleOfficial(tpl)">
                                                <template #icon>
                                                    <StarFilled v-if="tpl.isOfficial" />
                                                    <StarOutlined v-else />
                                                </template>
                                                {{ tpl.isOfficial ? '取消官方' : '设为官方' }}
                                            </a-button>
                                            <a-button size="small" danger @click="handleDeleteTemplate(tpl)" v-if="tpl.isActive">
                                                <template #icon><DeleteOutlined /></template>
                                                删除
                                            </a-button>
                                        </a-space>
                                    </td>
                                </tr>
                            </tbody>
                        </table>
                        <div class="pagination">
                            <a-pagination
                                :current="templatesPage + 1"
                                :total="templatesTotal"
                                :pageSize="templatesSize"
                                @change="handleTemplatesPageChange"
                                show-quick-jumper
                            />
                        </div>
                    </div>
                </div>

                <!-- Prompt 管理 -->
                <div v-if="activeTab === 'prompts'" class="tab-content">
                    <h2 class="tab-title">Prompt 管理</h2>
                    <div v-if="loadingPrompts" class="loading">加载中...</div>
                    <div v-else>
                        <table class="data-table">
                            <thead>
                                <tr>
                                    <th>ID</th>
                                    <th>名称</th>
                                    <th>描述</th>
                                    <th>工作空间ID</th>
                                    <th>创建者ID</th>
                                    <th>公开</th>
                                    <th>状态</th>
                                    <th>创建时间</th>
                                    <th>操作</th>
                                </tr>
                            </thead>
                            <tbody>
                                <tr v-for="prompt in prompts" :key="prompt.id">
                                    <td>{{ prompt.id }}</td>
                                    <td>{{ prompt.name }}</td>
                                    <td>{{ prompt.description || '-' }}</td>
                                    <td>{{ prompt.workspaceId }}</td>
                                    <td>{{ prompt.creatorId }}</td>
                                    <td>
                                        <a-tag :color="prompt.isPublic ? 'green' : 'default'">
                                            {{ prompt.isPublic ? '公开' : '私有' }}
                                        </a-tag>
                                    </td>
                                    <td>
                                        <a-tag :color="prompt.status === 1 ? 'green' : 'red'">
                                            {{ prompt.status === 1 ? '正常' : '已删除' }}
                                        </a-tag>
                                    </td>
                                    <td>{{ formatDate(prompt.createdAt) }}</td>
                                    <td>
                                        <a-button size="small" danger @click="handleDeletePrompt(prompt)" v-if="prompt.status === 1">
                                            <template #icon><DeleteOutlined /></template>
                                            删除
                                        </a-button>
                                    </td>
                                </tr>
                            </tbody>
                        </table>
                        <div class="pagination">
                            <a-pagination
                                :current="promptsPage + 1"
                                :total="promptsTotal"
                                :pageSize="promptsSize"
                                @change="handlePromptsPageChange"
                                show-quick-jumper
                            />
                        </div>
                    </div>
                </div>

                <!-- 竞技场会话管理 -->
                <div v-if="activeTab === 'arena'" class="tab-content">
                    <h2 class="tab-title">竞技场会话记录</h2>
                    <div v-if="loadingArenaSessions" class="loading">加载中...</div>
                    <div v-else>
                        <table class="data-table">
                            <thead>
                                <tr>
                                    <th>ID</th>
                                    <th>版本ID</th>
                                    <th>Prompt内容</th>
                                    <th>模型</th>
                                    <th>状态</th>
                                    <th>创建者ID</th>
                                    <th>创建时间</th>
                                    <th>完成时间</th>
                                </tr>
                            </thead>
                            <tbody>
                                <tr v-for="session in arenaSessions" :key="session.id">
                                    <td>{{ session.id }}</td>
                                    <td>{{ session.promptVersionId || '-' }}</td>
                                    <td class="text-ellipsis" :title="session.finalPrompt">{{ session.finalPrompt?.substring(0, 50) || '-' }}...</td>
                                    <td>
                                        <div class="model-tags">
                                            <a-tag v-for="model in parseModels(session.models)" :key="model" color="purple">
                                                {{ model }}
                                            </a-tag>
                                            <span v-if="!parseModels(session.models).length">-</span>
                                        </div>
                                    </td>
                                    <td>
                                        <a-tag :color="session.status === 'COMPLETED' ? 'green' : 'blue'">
                                            {{ session.status }}
                                        </a-tag>
                                    </td>
                                    <td>{{ session.creatorId }}</td>
                                    <td>{{ formatDate(session.createdAt) }}</td>
                                    <td>{{ session.completedAt ? formatDate(session.completedAt) : '-' }}</td>
                                </tr>
                            </tbody>
                        </table>
                        <div class="pagination">
                            <a-pagination
                                :current="arenaSessionsPage + 1"
                                :total="arenaSessionsTotal"
                                :pageSize="arenaSessionsSize"
                                @change="handleArenaSessionsPageChange"
                                show-quick-jumper
                            />
                        </div>
                    </div>
                </div>

                <!-- 登录日志管理 -->
                <div v-if="activeTab === 'logs'" class="tab-content">
                    <h2 class="tab-title">登录日志</h2>
                    <div v-if="loadingLoginLogs" class="loading">加载中...</div>
                    <div v-else>
                        <table class="data-table">
                            <thead>
                                <tr>
                                    <th>ID</th>
                                    <th>用户名</th>
                                    <th>IP地址</th>
                                    <th>地理位置</th>
                                    <th>结果</th>
                                    <th>失败原因</th>
                                    <th>UA</th>
                                    <th>创建时间</th>
                                </tr>
                            </thead>
                            <tbody>
                                <tr v-for="log in loginLogs" :key="log.id">
                                    <td>{{ log.id }}</td>
                                    <td>{{ log.username }}</td>
                                    <td>{{ log.ipAddress || '-' }}</td>
                                    <td>{{ log.geoLocation || '-' }}</td>
                                    <td>
                                        <a-tag :color="log.result === 'SUCCESS' ? 'green' : 'red'">
                                            {{ log.result }}
                                        </a-tag>
                                    </td>
                                    <td>{{ log.failureReason || '-' }}</td>
                                    <td class="text-ellipsis" :title="log.userAgent">{{ log.userAgent?.substring(0, 30) || '-' }}...</td>
                                    <td>{{ formatDate(log.createdAt) }}</td>
                                </tr>
                            </tbody>
                        </table>
                        <div class="pagination">
                            <a-pagination
                                :current="loginLogsPage + 1"
                                :total="loginLogsTotal"
                                :pageSize="loginLogsSize"
                                @change="handleLoginLogsPageChange"
                                show-quick-jumper
                            />
                        </div>
                    </div>
                </div>
            </main>
        </div>
    </div>
</template>

<style scoped>
.admin-container {
    min-height: 100vh;
    background: var(--color-bg-primary);
    color: var(--color-text-primary);
}

.header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 16px 32px;
    border-bottom: 1px solid var(--color-border);
    background: var(--color-bg-secondary);
}

.header-left {
    display: flex;
    align-items: center;
    gap: 8px;
}

.logo-icon {
    width: 32px;
    height: 32px;
}

.logo-text {
    font-size: 18px;
    font-weight: 600;
    color: var(--color-text-primary);
}

.header-right {
    display: flex;
    align-items: center;
    gap: 16px;
}

.username {
    color: var(--color-text-tertiary);
}

.main-layout {
    display: flex;
    min-height: calc(100vh - 65px);
}

.sidebar {
    width: 220px;
    background: var(--color-bg-secondary);
    border-right: 1px solid var(--color-border);
    padding: 16px 0;
}

.menu-item {
    display: flex;
    align-items: center;
    gap: 10px;
    padding: 12px 24px;
    color: var(--color-text-secondary);
    cursor: pointer;
    transition: all 0.2s;
}

.menu-item:hover {
    background: var(--color-bg-tertiary);
    color: var(--color-primary);
}

.menu-item.active {
    background: var(--color-primary);
    color: white;
}

.content {
    flex: 1;
    padding: 24px;
    overflow-y: auto;
}

.tab-content {
    max-width: 1400px;
}

.tab-title {
    font-size: 24px;
    font-weight: 600;
    margin-bottom: 24px;
    color: var(--color-text-primary);
}

.loading {
    text-align: center;
    padding: 40px;
    color: var(--color-text-tertiary);
}

.tab-desc {
    color: var(--color-text-tertiary);
    margin-bottom: 24px;
    font-size: 14px;
}

.stats-container {
    display: flex;
    flex-direction: column;
    gap: 24px;
}

.stats-row {
    display: flex;
    flex-wrap: wrap;
    gap: 20px;
}

.main-stats .stat-card {
    flex: 1;
    min-width: 280px;
}

.secondary-stats {
    margin-top: 12px;
}

/* 大卡片 - 可点击 */
.stat-card {
    display: flex;
    align-items: center;
    gap: 16px;
    padding: 28px 24px;
    background: linear-gradient(145deg, var(--color-bg-elevated), var(--color-bg-secondary));
    border: 1px solid var(--color-border);
    border-radius: 16px;
    transition: all 0.3s ease;
    position: relative;
}

.stat-card.clickable {
    cursor: pointer;
}

.stat-card.clickable:hover {
    transform: translateY(-4px);
    box-shadow: 0 12px 40px rgba(0, 0, 0, 0.25);
    border-color: var(--color-primary);
}

.stat-card.clickable:hover .stat-arrow {
    opacity: 1;
    transform: translateX(4px);
}

.stat-arrow {
    position: absolute;
    right: 20px;
    font-size: 20px;
    color: var(--color-primary);
    opacity: 0;
    transition: all 0.3s ease;
}

.stat-icon {
    width: 60px;
    height: 60px;
    display: flex;
    align-items: center;
    justify-content: center;
    border-radius: 16px;
    font-size: 26px;
    color: white;
    box-shadow: 0 4px 12px rgba(0, 0, 0, 0.2);
}

.stat-icon.users { background: linear-gradient(135deg, #667eea, #764ba2); }
.stat-icon.workspaces { background: linear-gradient(135deg, #f093fb, #f5576c); }
.stat-icon.prompts { background: linear-gradient(135deg, #4facfe, #00f2fe); }
.stat-icon.public { background: linear-gradient(135deg, #43e97b, #38f9d7); }
.stat-icon.arena { background: linear-gradient(135deg, #fa709a, #fee140); }
.stat-icon.active { background: linear-gradient(135deg, #a8edea, #fed6e3); color: #333; }
.stat-icon.templates { background: linear-gradient(135deg, #ffecd2, #fcb69f); color: #333; }

.stat-info {
    flex: 1;
}

.stat-value {
    font-size: 36px;
    font-weight: 700;
    color: var(--color-text-primary);
    line-height: 1.2;
}

.stat-label {
    font-size: 14px;
    color: var(--color-text-tertiary);
    margin-top: 4px;
}

/* 小卡片 - 仅展示 */
.stat-card-sm {
    display: flex;
    align-items: center;
    gap: 12px;
    padding: 16px 20px;
    background: var(--color-bg-elevated);
    border: 1px solid var(--color-border);
    border-radius: 12px;
    min-width: 150px;
    flex: 1;
}

.stat-icon-sm {
    width: 40px;
    height: 40px;
    display: flex;
    align-items: center;
    justify-content: center;
    border-radius: 10px;
    font-size: 18px;
    color: white;
}

.stat-icon-sm.prompts { background: linear-gradient(135deg, #4facfe, #00f2fe); }
.stat-icon-sm.public { background: linear-gradient(135deg, #43e97b, #38f9d7); }
.stat-icon-sm.arena { background: linear-gradient(135deg, #fa709a, #fee140); }
.stat-icon-sm.active { background: linear-gradient(135deg, #a8edea, #fed6e3); color: #333; }

.stat-info-sm {
    flex: 1;
}

.stat-value-sm {
    font-size: 22px;
    font-weight: 600;
    color: var(--color-text-primary);
}

.stat-label-sm {
    font-size: 12px;
    color: var(--color-text-tertiary);
}

.data-table {
    width: 100%;
    border-collapse: collapse;
    background: var(--color-bg-elevated);
    border-radius: 8px;
    overflow: hidden;
}

.data-table th,
.data-table td {
    padding: 12px 16px;
    text-align: left;
    border-bottom: 1px solid var(--color-border);
}

.data-table th {
    background: var(--color-bg-secondary);
    font-weight: 600;
    color: var(--color-text-secondary);
}

.data-table tr:hover {
    background: var(--color-bg-tertiary);
}

.pagination {
    margin-top: 20px;
    display: flex;
    justify-content: flex-end;
}

.admin-label {
    color: var(--color-text-tertiary);
    font-size: 12px;
}

.workspace-list {
    display: flex;
    flex-wrap: wrap;
    gap: 4px;
    max-width: 200px;
}

.ws-tag {
    margin: 0 !important;
    font-size: 11px;
}

.no-data {
    color: var(--color-text-tertiary);
}

.model-tags {
    display: flex;
    flex-wrap: wrap;
    gap: 4px;
}

.text-ellipsis {
    max-width: 200px;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
}
</style>

