<template>
  <div class="main-layout">
    <!-- 侧边栏 -->
    <aside class="sidebar">
      <div class="sidebar-header">
        <div class="logo">
          <img src="/logo.svg?v=4" alt="Logo" class="logo-icon" />
          <span class="logo-text">Prompt-Forge</span>
        </div>
      </div>

      <nav class="sidebar-nav">
        <router-link to="/prompts" class="nav-item" active-class="active">
          <AppstoreOutlined />
          <span>Prompt 库</span>
        </router-link>
        <router-link to="/coach" class="nav-item" active-class="active">
          <RobotOutlined />
          <span>Prompt 教练</span>
        </router-link>
        <router-link to="/arena" class="nav-item" active-class="active">
          <ThunderboltOutlined />
          <span>竞技场</span>
        </router-link>
        <router-link to="/plaza" class="nav-item" active-class="active">
          <ShopOutlined /> <!-- 使用 ShopOutlined 或类似的 -->
          <span>广场</span>
        </router-link>

        <router-link to="/settings/models" class="nav-item" active-class="active">
          <SettingOutlined />
          <span>模型配置</span>
        </router-link>

        <div class="nav-divider"></div>

        <router-link v-if="isAdmin" to="/admin" class="nav-item" active-class="active">
          <SettingOutlined />
          <span>管理后台</span>
        </router-link>
      </nav>

      <div class="sidebar-footer">
        <div class="workspace-section">
          <WorkspaceSelector v-model="currentWorkspaceId" @change="handleWorkspaceChange" />
        </div>

        <div class="user-profile">
          <a-dropdown placement="topLeft">
            <div class="user-info">
              <a-avatar :size="32" class="user-avatar">{{ userInitials }}</a-avatar>
              <div class="user-details">
                <span class="username">{{ user?.username || '用户' }}</span>
                <span class="user-role">{{ userRoleText }}</span>
              </div>
              <MoreOutlined class="more-icon" />
            </div>
            <template #overlay>
              <a-menu>
                <a-menu-item key="logout" danger @click="handleLogout">
                  <LogoutOutlined /> 退出登录
                </a-menu-item>
              </a-menu>
            </template>
          </a-dropdown>
        </div>
      </div>
    </aside>

    <!-- 主内容区 -->
    <main class="content-area">
      <router-view v-slot="{ Component }">
        <transition name="fade" mode="out-in">
          <component :is="Component" />
        </transition>
      </router-view>
    </main>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import {
  AppstoreOutlined,
  RobotOutlined,
  ThunderboltOutlined,
  ShopOutlined,
  SettingOutlined,
  LogoutOutlined,
  MoreOutlined
} from '@ant-design/icons-vue'
import WorkspaceSelector from '../components/WorkspaceSelector.vue'
import { message } from 'ant-design-vue'

const router = useRouter()
const user = ref<any>(null)
const currentWorkspaceId = ref<number>()

onMounted(() => {
  const userStr = localStorage.getItem('user')
  if (userStr) {
    user.value = JSON.parse(userStr)
  }

  const wsId = localStorage.getItem('currentWorkspaceId')
  if (wsId) {
    currentWorkspaceId.value = parseInt(wsId)
  }
})

const isAdmin = computed(() => user.value?.role === 'ADMIN')
const userInitials = computed(() => user.value?.username?.substring(0, 1).toUpperCase() || 'U')
const userRoleText = computed(() => isAdmin.value ? '管理员' : '普通用户')

const handleWorkspaceChange = (id: number) => {
  currentWorkspaceId.value = id
  localStorage.setItem('currentWorkspaceId', id.toString())
  // 可以在这里触发全局事件或刷新，简单起见刷新页面或依靠组件内部的 watcher
  window.location.reload()
}

const handleLogout = () => {
  localStorage.removeItem('token')
  localStorage.removeItem('user')
  localStorage.removeItem('currentWorkspaceId')
  message.success('已退出登录')
  router.push('/login')
}
</script>

<style scoped>
.main-layout {
  display: flex;
  height: 100vh;
  width: 100vw;
  background: var(--color-bg-primary);
  overflow: hidden;
}

/* Sidebar Styling */
.sidebar {
  width: 260px;
  background: #f9f9f9;
  /* 浅灰背景，即 Sidebar 背景 */
  border-right: 1px solid var(--color-border);
  display: flex;
  flex-direction: column;
  flex-shrink: 0;
  transition: all var(--transition-base);
}

/* 适配暗色模式或者 Sidebar 特有样式，这里保持 Light Theme */
.sidebar-header {
  padding: var(--space-5) var(--space-4);
}

.logo {
  display: flex;
  align-items: center;
  gap: var(--space-3);
}

.logo-text {
  font-size: var(--text-lg);
  font-weight: 600;
  color: var(--color-text-primary);
}

.sidebar-nav {
  flex: 1;
  padding: 0 var(--space-3);
  display: flex;
  flex-direction: column;
  gap: 4px;
  overflow-y: auto;
}

.nav-item {
  display: flex;
  align-items: center;
  gap: var(--space-3);
  padding: var(--space-3) var(--space-3);
  border-radius: var(--radius-md);
  color: var(--color-text-secondary);
  font-size: var(--text-base);
  text-decoration: none;
  transition: all var(--transition-fast);
}

.nav-item:hover {
  background: rgba(0, 0, 0, 0.05);
  color: var(--color-text-primary);
}

.nav-item.active {
  background: #e6e6e6;
  /* 激活项背景 */
  color: var(--color-text-primary);
  font-weight: 500;
}

.nav-divider {
  height: 1px;
  background: var(--color-border);
  margin: var(--space-2) 0;
}

/* Footer Section */
.sidebar-footer {
  padding: var(--space-4);
  border-top: 1px solid var(--color-border);
  display: flex;
  flex-direction: column;
  gap: var(--space-4);
  background: #f9f9f9;
}

.user-profile {
  cursor: pointer;
}

.user-info {
  display: flex;
  align-items: center;
  gap: var(--space-3);
  padding: var(--space-2);
  border-radius: var(--radius-md);
  transition: background var(--transition-fast);
}

.user-info:hover {
  background: rgba(0, 0, 0, 0.05);
}

.user-avatar {
  background: var(--color-primary);
  color: white;
}

.user-details {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.username {
  font-size: var(--text-sm);
  font-weight: 500;
  color: var(--color-text-primary);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.user-role {
  font-size: var(--text-xs);
  color: var(--color-text-tertiary);
}

.more-icon {
  color: var(--color-text-tertiary);
}

/* Content Area */
.content-area {
  flex: 1;
  height: 100%;
  overflow-y: auto;
  background: var(--color-bg-primary);
  /* 内容区纯白 */
  position: relative;
}

/* 覆盖 Workspace Selector 样式以适应 Sidebar */
:deep(.workspace-selector) {
  width: 100%;
}
</style>
