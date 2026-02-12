<template>
  <div class="main-layout">
    <!-- 侧边栏 -->
    <aside class="sidebar">
      <div class="sidebar-header">
        <div class="logo">
          <img :src="logo" alt="Logo" class="logo-icon" />
          <span class="logo-text">Prompt-Forge</span>
        </div>
      </div>

      <nav class="sidebar-nav">
        <router-link to="/app/prompts" class="nav-item" active-class="active">
          <AppstoreOutlined />
          <span>Prompt 库</span>
        </router-link>
        <router-link to="/app/coach" class="nav-item" active-class="active">
          <RobotOutlined />
          <span>Prompt 教练</span>
        </router-link>
        <router-link to="/app/arena" class="nav-item" active-class="active">
          <ThunderboltOutlined />
          <span>竞技场</span>
        </router-link>
        <router-link to="/app/plaza" class="nav-item" active-class="active">
          <ShopOutlined /> <!-- 使用 ShopOutlined 或类似的 -->
          <span>广场</span>
        </router-link>

        <router-link to="/app/settings/models" class="nav-item" active-class="active">
          <SettingOutlined />
          <span>模型配置</span>
        </router-link>

        <div class="nav-divider"></div>

        <router-link v-if="isAdmin" to="/app/admin" class="nav-item" active-class="active">
          <SettingOutlined />
          <span>管理后台</span>
        </router-link>
      </nav>

      <!-- BigModel.cn Promotion -->
      <div class="sidebar-promo-wrapper">
        <div class="promo-hint">
          <span class="hint-question">没有 API Key？</span>
          <span class="hint-arrow">↓</span>
          <span class="hint-answer">点这里免费领！</span>
        </div>
        <a href="https://www.bigmodel.cn/invite?icode=9v1maCD3s4Titga15Jy0C3HEaazDlIZGj9HxftzTbt4%3D" target="_blank"
          class="sidebar-promo-card">
          <div class="promo-content">
            <div class="promo-badge">NEW</div>
            <div class="promo-title">GLM-5.0 已上线</div>
            <div class="promo-desc">免费领 2000万 Tokens &rarr;</div>
          </div>
          <div class="promo-shine"></div>
        </a>
      </div>

      <div class="sidebar-footer">
        <div class="workspace-section">
          <WorkspaceSelector v-model="currentWorkspaceId" @change="handleWorkspaceChange" />
        </div>

        <div class="user-profile">
          <a-dropdown placement="topLeft">
            <div class="user-info">
              <a-avatar :size="32" :src="user?.avatar" class="user-avatar">
                <template #icon v-if="!user?.avatar">
                  <UserOutlined />
                </template>
              </a-avatar>
              <div class="user-details">
                <span class="username">{{ user?.nickname || user?.username || '用户' }}</span>
                <span class="user-role">{{ userRoleText }}</span>
              </div>
              <MoreOutlined class="more-icon" />
            </div>
            <template #overlay>
              <a-menu>
                <a-menu-item key="profile" @click="router.push('/app/settings/profile')">
                  <UserOutlined /> 个人资料
                </a-menu-item>
                <a-menu-divider />
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
  UserOutlined,
  MoreOutlined
} from '@ant-design/icons-vue'
import WorkspaceSelector from '../components/WorkspaceSelector.vue'
import { message } from 'ant-design-vue'
import logo from '@/assets/logo.svg'

const router = useRouter()
const user = ref<any>(null)
const currentWorkspaceId = ref<number>()

const updateUser = () => {
  const userStr = localStorage.getItem('user')
  if (userStr) {
    user.value = JSON.parse(userStr)
  }
}

onMounted(() => {
  updateUser() // Initial load
  window.addEventListener('user-updated', updateUser) // Listen for updates

  const wsId = localStorage.getItem('currentWorkspaceId')
  if (wsId) {
    currentWorkspaceId.value = parseInt(wsId)
  }
})

// Clean up listener (optional but good practice)
import { onUnmounted } from 'vue'
onUnmounted(() => {
  window.removeEventListener('user-updated', updateUser)
})

const isAdmin = computed(() => user.value?.role === 'ADMIN')
const userRoleText = computed(() => isAdmin.value ? '管理员' : '普通用户')

const handleWorkspaceChange = (workspace: any) => {
  const id = typeof workspace === 'number' ? workspace : workspace.id
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
  background: #f2f2f2;
  color: #a1a1a1;
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

/* 广告样式 (Renamed to promo to avoid adblockers) */
.sidebar-promo-wrapper {
  padding: 0 var(--space-3) var(--space-3);
  margin-top: auto;
  /* 确保贴在底部 (如果 nav 没有撑满) */
  flex-shrink: 0;
  z-index: 10;
  position: relative;
}

/* API Key 引导提示 */
.promo-hint {
  display: flex;
  flex-direction: column;
  align-items: center;
  margin-bottom: var(--space-2);
  padding: var(--space-2) var(--space-3);
  background: linear-gradient(135deg, rgba(126, 34, 206, 0.08) 0%, rgba(30, 64, 175, 0.08) 100%);
  border-radius: var(--radius-md);
  border: 1px dashed rgba(126, 34, 206, 0.25);
}

.hint-question {
  font-size: 12px;
  color: #7e22ce;
  font-weight: 500;
}

.hint-arrow {
  font-size: 16px;
  color: #7e22ce;
  animation: bounce 1s infinite;
  line-height: 1;
}

@keyframes bounce {

  0%,
  100% {
    transform: translateY(0);
  }

  50% {
    transform: translateY(3px);
  }
}

.hint-answer {
  font-size: 11px;
  color: #1e40af;
  font-weight: 600;
}

.sidebar-promo-card {
  display: block;
  position: relative;
  background: linear-gradient(135deg, #1e40af 0%, #7e22ce 100%);
  /* 深蓝到深紫 */
  border-radius: var(--radius-lg);
  padding: var(--space-4);
  text-decoration: none;
  overflow: hidden;
  transition: all var(--transition-base);
  box-shadow: 0 4px 12px rgba(37, 99, 235, 0.15);
  border: 1px solid rgba(255, 255, 255, 0.1);
}

.sidebar-promo-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 20px rgba(126, 34, 206, 0.25);
}

.promo-content {
  position: relative;
  z-index: 2;
}

.promo-badge {
  display: inline-block;
  background: rgba(255, 255, 255, 0.2);
  color: #fff;
  font-size: 10px;
  font-weight: 700;
  padding: 2px 6px;
  border-radius: 4px;
  margin-bottom: 6px;
  letter-spacing: 0.5px;
}

.promo-title {
  color: #ffffff;
  font-size: 15px;
  font-weight: 700;
  margin-bottom: 2px;
  letter-spacing: -0.2px;
}

.promo-desc {
  color: rgba(255, 255, 255, 0.9);
  font-size: 11px;
  font-weight: 500;
}

/* 光泽效果 */
.promo-shine {
  position: absolute;
  top: 0;
  left: -100%;
  width: 50%;
  height: 100%;
  background: linear-gradient(90deg,
      transparent,
      rgba(255, 255, 255, 0.2),
      transparent);
  transform: skewX(-20deg);
  transition: 0.5s;
  z-index: 1;
}

.sidebar-promo-card:hover .promo-shine {
  left: 200%;
  transition: 1s ease-in-out;
}
</style>
