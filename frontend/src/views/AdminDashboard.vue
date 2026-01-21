<script setup lang="ts">
import { ref, onMounted } from 'vue'
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
    CloudSyncOutlined
} from '@ant-design/icons-vue'
import AdminModels from './admin/AdminModels.vue'
import AgentMonitor from './AgentMonitor.vue'
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
} catch { }

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
        <div class="admin-layout">
            <!-- Top Navigation Tabs (Replaces Sidebar) -->
            <div class="admin-nav">
                <div class="nav-item" :class="{ active: activeTab === 'dashboard' }"
                    @click="handleTabChange('dashboard')">
                    <DashboardOutlined /> 仪表盘
                </div>
                <div class="nav-item" :class="{ active: activeTab === 'users' }" @click="handleTabChange('users')">
                    <UserOutlined /> 用户管理
                </div>
                <div class="nav-item" :class="{ active: activeTab === 'workspaces' }"
                    @click="handleTabChange('workspaces')">
                    <FolderOutlined /> 工作空间
                </div>
                <div class="nav-item" :class="{ active: activeTab === 'prompts' }" @click="handleTabChange('prompts')">
                    <FileTextOutlined /> Prompt管理
                </div>
                <div class="nav-item" :class="{ active: activeTab === 'templates' }"
                    @click="handleTabChange('templates')">
                    <AppstoreOutlined /> 广场模板
                </div>
                <div class="nav-item" :class="{ active: activeTab === 'arena' }" @click="handleTabChange('arena')">
                    <ThunderboltOutlined /> 竞技场会话
                </div>
                <div class="nav-item" :class="{ active: activeTab === 'models' }" @click="handleTabChange('models')">
                    <CloudSyncOutlined /> 模型管理
                </div>
                <div class="nav-item" :class="{ active: activeTab === 'monitor' }" @click="handleTabChange('monitor')">
                    <DashboardOutlined /> Agent 监控
                </div>
                <div class="nav-item" :class="{ active: activeTab === 'logs' }" @click="handleTabChange('logs')">
                    <SettingOutlined /> 登录日志
                </div>
            </div>

            <!-- Content -->
            <main class="content">
                <!-- Agent 监控 -->
                <div v-if="activeTab === 'monitor'" class="tab-content">
                    <AgentMonitor />
                </div>

                <!-- 模型管理 -->
                <div v-if="activeTab === 'models'" class="tab-content">
                    <h2 class="tab-title">模型管理</h2>
                    <AdminModels />
                </div>

                <!-- 仪表盘 -->
                <div v-if="activeTab === 'dashboard'" class="tab-content">
                    <h2 class="tab-title">系统概览</h2>
                    <p class="tab-desc">点击卡片可查看详细数据</p>
                    <div v-if="loadingStats" class="loading">加载中...</div>
                    <div v-else-if="stats" class="stats-container">
                        <!-- 主要数据卡片 - 大尺寸 -->
                        <div class="stats-row main-stats">
                            <div class="stat-card clickable" @click="handleTabChange('users')">
                                <div class="stat-icon users">
                                    <UserOutlined />
                                </div>
                                <div class="stat-info">
                                    <div class="stat-value">{{ stats.totalUsers }}</div>
                                    <div class="stat-label">总用户数</div>
                                </div>
                                <div class="stat-arrow">→</div>
                            </div>
                            <div class="stat-card clickable" @click="handleTabChange('workspaces')">
                                <div class="stat-icon workspaces">
                                    <FolderOutlined />
                                </div>
                                <div class="stat-info">
                                    <div class="stat-value">{{ stats.totalWorkspaces }}</div>
                                    <div class="stat-label">工作空间</div>
                                </div>
                                <div class="stat-arrow">→</div>
                            </div>
                            <div class="stat-card clickable" @click="handleTabChange('templates')">
                                <div class="stat-icon templates">
                                    <AppstoreOutlined />
                                </div>
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
                                <div class="stat-icon-sm prompts">
                                    <FileTextOutlined />
                                </div>
                                <div class="stat-info-sm">
                                    <div class="stat-value-sm">{{ stats.totalPrompts }}</div>
                                    <div class="stat-label-sm">总 Prompt</div>
                                </div>
                            </div>
                            <div class="stat-card-sm">
                                <div class="stat-icon-sm public">
                                    <AppstoreOutlined />
                                </div>
                                <div class="stat-info-sm">
                                    <div class="stat-value-sm">{{ stats.publicPrompts }}</div>
                                    <div class="stat-label-sm">公开 Prompt</div>
                                </div>
                            </div>
                            <div class="stat-card-sm">
                                <div class="stat-icon-sm arena">
                                    <ThunderboltOutlined />
                                </div>
                                <div class="stat-info-sm">
                                    <div class="stat-value-sm">{{ stats.totalArenaSessions }}</div>
                                    <div class="stat-label-sm">竞技场次数</div>
                                </div>
                            </div>
                            <div class="stat-card-sm">
                                <div class="stat-icon-sm active">
                                    <UserOutlined />
                                </div>
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
                                        <a-select :value="user.role" style="width: 100px"
                                            @change="(val: string) => handleChangeRole(user, val)"
                                            :disabled="user.role === 'ADMIN'">
                                            <a-select-option v-for="opt in roleOptions" :key="opt.value"
                                                :value="opt.value">
                                                {{ opt.label }}
                                            </a-select-option>
                                        </a-select>
                                    </td>
                                    <td>
                                        <div v-if="user.workspaces && user.workspaces.length > 0"
                                            class="workspace-list">
                                            <a-tooltip v-for="ws in user.workspaces" :key="ws.id"
                                                :title="ws.isOwner ? '所有者 - ' + ws.role : ws.role">
                                                <a-tag class="ws-tag">
                                                    {{ ws.name }}
                                                </a-tag>
                                            </a-tooltip>
                                        </div>
                                        <span v-else class="no-data">-</span>
                                    </td>
                                    <td>{{ user.promptCount || 0 }}</td>
                                    <td>{{ user.arenaSessionCount || 0 }}</td>
                                    <td>
                                        <a-tag>
                                            {{ user.status === 1 ? '正常' : '禁用' }}
                                        </a-tag>
                                    </td>
                                    <td>{{ formatDate(user.createdAt) }}</td>
                                    <td>
                                        <a-button v-if="user.role !== 'ADMIN'" size="small" :type="'default'"
                                            @click="handleToggleUserStatus(user)">
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
                            <a-pagination :current="usersPage + 1" :total="usersTotal" :pageSize="usersSize"
                                @change="handleUsersPageChange" show-quick-jumper />
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
                                        <a-button size="small" class="delete-btn" @click="handleDeleteWorkspace(ws)">
                                            <template #icon>
                                                <DeleteOutlined />
                                            </template>
                                            删除
                                        </a-button>
                                    </td>
                                </tr>
                            </tbody>
                        </table>
                        <div class="pagination">
                            <a-pagination :current="workspacesPage + 1" :total="workspacesTotal"
                                :pageSize="workspacesSize" @change="handleWorkspacesPageChange" show-quick-jumper />
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
                                            <a-button size="small" danger @click="handleDeleteTemplate(tpl)"
                                                v-if="tpl.isActive">
                                                <template #icon>
                                                    <DeleteOutlined />
                                                </template>
                                                删除
                                            </a-button>
                                        </a-space>
                                    </td>
                                </tr>
                            </tbody>
                        </table>
                        <div class="pagination">
                            <a-pagination :current="templatesPage + 1" :total="templatesTotal" :pageSize="templatesSize"
                                @change="handleTemplatesPageChange" show-quick-jumper />
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
                                    <th>工作空间</th>
                                    <th>创建者</th>
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
                                    <td>
                                        <a-tooltip :title="'ID: ' + prompt.workspaceId">
                                            {{ prompt.workspaceName }}
                                        </a-tooltip>
                                    </td>
                                    <td>
                                        <a-tooltip :title="'ID: ' + prompt.creatorId">
                                            {{ prompt.creatorName }}
                                        </a-tooltip>
                                    </td>
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
                                        <a-button size="small" danger @click="handleDeletePrompt(prompt)"
                                            v-if="prompt.status === 1">
                                            <template #icon>
                                                <DeleteOutlined />
                                            </template>
                                            删除
                                        </a-button>
                                    </td>
                                </tr>
                            </tbody>
                        </table>
                        <div class="pagination">
                            <a-pagination :current="promptsPage + 1" :total="promptsTotal" :pageSize="promptsSize"
                                @change="handlePromptsPageChange" show-quick-jumper />
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
                                    <td class="text-ellipsis" :title="session.finalPrompt">{{
                                        session.finalPrompt?.substring(0, 50) ||
                                        '-' }}...</td>
                                    <td>
                                        <div class="model-tags">
                                            <a-tag color="blue">{{ (session as any).modelAProvider }}</a-tag>
                                            <a-tag color="purple">{{ (session as any).modelBProvider }}</a-tag>
                                        </div>
                                    </td>
                                    <td>
                                        <a-tag
                                            :color="(session as any).winner ? 'green' : (session.status === 'COMPLETED' ? 'blue' : 'orange')">
                                            {{ (session as any).winner ? '已决出胜负' : session.status }}
                                        </a-tag>
                                    </td>
                                    <td>{{ session.creatorId }}</td>
                                    <td>{{ formatDate(session.createdAt) }}</td>
                                    <td>{{ formatDate(session.completedAt) || '-' }}</td>
                                </tr>
                            </tbody>
                        </table>
                        <div class="pagination">
                            <a-pagination :current="arenaSessionsPage + 1" :total="arenaSessionsTotal"
                                :pageSize="arenaSessionsSize" @change="handleArenaSessionsPageChange"
                                show-quick-jumper />
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
                                    <th>用户ID</th>
                                    <th>用户名</th>
                                    <th>IP地址</th>
                                    <th>User Agent</th>
                                    <th>登录时间</th>
                                </tr>
                            </thead>
                            <tbody>
                                <tr v-for="log in loginLogs" :key="log.id">
                                    <td>{{ log.id }}</td>
                                    <td>{{ (log as any).userId }}</td>
                                    <td>{{ log.username }}</td>
                                    <td>{{ log.ipAddress }}</td>
                                    <td class="text-ellipsis" :title="log.userAgent">{{ log.userAgent }}</td>
                                    <td>{{ formatDate(log.createdAt) }}</td>
                                </tr>
                            </tbody>
                        </table>
                        <div class="pagination">
                            <a-pagination :current="loginLogsPage + 1" :total="loginLogsTotal" :pageSize="loginLogsSize"
                                @change="handleLoginLogsPageChange" show-quick-jumper />
                        </div>
                    </div>
                </div>
            </main>
        </div>
    </div>
</template>

<style scoped>
.admin-container {
    height: 100%;
    display: flex;
    flex-direction: column;
    padding: var(--space-6);
    background: var(--color-bg-primary);
}

.admin-layout {
    display: flex;
    flex-direction: column;
    gap: var(--space-6);
    height: 100%;
}

/* New Top Navigation Styles */
.admin-nav {
    display: flex;
    gap: var(--space-2);
    padding-bottom: var(--space-4);
    border-bottom: 1px solid var(--color-border);
    overflow-x: auto;
}

.nav-item {
    padding: var(--space-2) var(--space-4);
    border-radius: var(--radius-md);
    cursor: pointer;
    color: var(--color-text-secondary);
    font-size: var(--text-sm);
    display: flex;
    align-items: center;
    gap: var(--space-2);
    transition: all var(--transition-fast);
    white-space: nowrap;
}

.nav-item:hover {
    background: var(--color-bg-secondary);
    color: var(--color-text-primary);
}

.nav-item.active {
    background: var(--color-primary);
    color: #fff;
}

.content {
    flex: 1;
    overflow-y: auto;
    background: #fff;
    border-radius: var(--radius-lg);
    border: 1px solid var(--color-border);
    padding: var(--space-6);
}

.tab-title {
    font-size: var(--text-xl);
    margin-bottom: var(--space-2);
    color: var(--color-text-primary);
}

.tab-desc {
    color: var(--color-text-secondary);
    margin-bottom: var(--space-6);
}

.stats-container {
    display: flex;
    flex-direction: column;
    gap: var(--space-6);
}

.stats-row {
    display: grid;
    gap: var(--space-6);
}

.main-stats {
    grid-template-columns: repeat(3, 1fr);
}

.secondary-stats {
    grid-template-columns: repeat(4, 1fr);
}

.stat-card,
.stat-card-sm {
    background: var(--color-bg-secondary);
    border-radius: var(--radius-lg);
    padding: var(--space-6);
    display: flex;
    align-items: center;
    justify-content: space-between;
    transition: transform var(--transition-fast), box-shadow var(--transition-fast);
}

.stat-card.clickable:hover {
    transform: translateY(-2px);
    box-shadow: 0 4px 12px rgba(0, 0, 0, 0.05);
    cursor: pointer;
}

.stat-icon {
    font-size: 32px;
    padding: var(--space-3);
    border-radius: var(--radius-md);
    background: #fff;
}

.stat-icon.users {
    color: #3b82f6;
}

.stat-icon.workspaces {
    color: #10b981;
}

.stat-icon.templates {
    color: #f59e0b;
}

.stat-info {
    flex: 1;
}

.stat-value {
    font-size: var(--text-2xl);
    font-weight: 700;
    color: var(--color-text-primary);
    line-height: 1.2;
}

.stat-label {
    font-size: var(--text-sm);
    color: var(--color-text-secondary);
    margin-top: var(--space-1);
}

/* 小卡片 - 仅展示 */
.stat-card-sm {
    display: flex;
    align-items: center;
    gap: var(--space-3);
    padding: var(--space-4) var(--space-5);
    background: var(--color-bg-secondary);
    border: 1px solid var(--color-border);
    border-radius: var(--radius-lg);
    min-width: 150px;
    flex: 1;
}

.stat-icon-sm {
    width: 40px;
    height: 40px;
    display: flex;
    align-items: center;
    justify-content: center;
    border-radius: var(--radius-md);
    font-size: var(--text-lg);
    color: white;
}

.stat-icon-sm.prompts {
    background: #0d0d0d;
}

.stat-icon-sm.public {
    background: #404040;
}

.stat-icon-sm.arena {
    background: #606060;
}

.stat-icon-sm.active {
    background: #0d0d0d;
}

.stat-info-sm {
    flex: 1;
}

/* Refined UI Elements */
.delete-btn {
    color: var(--color-text-tertiary);
    transition: all var(--transition-fast);
}

.delete-btn:hover {
    color: var(--color-danger);
    background: rgba(239, 68, 68, 0.1);
    border-color: rgba(239, 68, 68, 0.2);
}

.stat-value-sm {
    font-size: var(--text-xl);
    font-weight: 600;
    color: var(--color-text-primary);
}

.stat-label-sm {
    font-size: var(--text-xs);
    color: var(--color-text-tertiary);
}

.data-table {
    width: 100%;
    border-collapse: collapse;
    background: var(--color-bg-primary);
    border: 1px solid var(--color-border);
    border-radius: var(--radius-md);
    overflow: hidden;
}

.data-table th,
.data-table td {
    padding: var(--space-3) var(--space-4);
    text-align: left;
    border-bottom: 1px solid var(--color-border);
}

.data-table th {
    background: var(--color-bg-secondary);
    font-weight: 600;
    color: var(--color-text-secondary);
}

.data-table tr:hover {
    background: var(--color-bg-secondary);
}

.pagination {
    margin-top: var(--space-5);
    display: flex;
    justify-content: flex-end;
}

.admin-label {
    color: var(--color-text-tertiary);
    font-size: var(--text-xs);
}

.workspace-list {
    display: flex;
    flex-wrap: wrap;
    gap: var(--space-1);
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
    gap: var(--space-1);
}

.text-ellipsis {
    max-width: 200px;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
}
</style>
