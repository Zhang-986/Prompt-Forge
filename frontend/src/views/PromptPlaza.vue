<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { getTemplates, cloneTemplate, CATEGORIES, type PromptTemplate } from '../api/plaza'
import { getWorkspaces, type Workspace } from '../api/workspace'
import { message, Modal } from 'ant-design-vue'
import { AppstoreOutlined, FileTextOutlined, ThunderboltOutlined, DownloadOutlined } from '@ant-design/icons-vue'

const router = useRouter()
const loading = ref(false)
const templates = ref<PromptTemplate[]>([])
const selectedCategory = ref('ALL')
const workspaces = ref<Workspace[]>([])
const selectedWorkspace = ref<number | null>(null)
const showCloneDialog = ref(false)
const cloneTargetTemplate = ref<PromptTemplate | null>(null)
const cloning = ref(false)

// 预览弹窗
const showPreview = ref(false)
const previewTemplate = ref<PromptTemplate | null>(null)

// 加载模板列表
const loadTemplates = async () => {
  loading.value = true
  try {
    const category = selectedCategory.value === 'ALL' ? undefined : selectedCategory.value
    const res = await getTemplates(category)
    if (res.code === 200) {
      templates.value = res.data
    }
  } catch (error) {
    console.error('加载模板失败:', error)
  } finally {
    loading.value = false
  }
}

// 加载工作空间
const loadWorkspaces = async () => {
  try {
    const res = await getWorkspaces()
    if (res.code === 200) {
      workspaces.value = res.data
      if (res.data.length > 0) {
        selectedWorkspace.value = res.data[0].id
      }
    }
  } catch (error) {
    console.error('加载工作空间失败:', error)
  }
}

// 打开克隆对话框
const openCloneDialog = (template: PromptTemplate) => {
  cloneTargetTemplate.value = template
  showCloneDialog.value = true
}

// 执行克隆
const handleClone = async () => {
  if (!cloneTargetTemplate.value || !selectedWorkspace.value) return
  
  cloning.value = true
  try {
    const res = await cloneTemplate(cloneTargetTemplate.value.id, selectedWorkspace.value)
    if (res.code === 200) {
      message.success('克隆成功！已添加到您的工作空间')
      showCloneDialog.value = false
      // 询问是否跳转
      Modal.confirm({
        title: '克隆成功',
        content: '是否立即查看克隆的 Prompt？',
        okText: '去查看',
        cancelText: '继续浏览',
        onOk: () => {
          router.push(`/prompts/${res.data.id}/versions`)
        }
      })
    }
  } catch (error: any) {
    message.error(error.response?.data?.message || '克隆失败')
  } finally {
    cloning.value = false
  }
}

// 预览模板
const openPreview = (template: PromptTemplate) => {
  previewTemplate.value = template
  showPreview.value = true
}

// 获取分类图标
const getCategoryIcon = (category: string) => {
  const cat = CATEGORIES.find(c => c.value === category)
  return cat?.icon || '📦'
}

// 获取分类名称
const getCategoryLabel = (category: string) => {
  const cat = CATEGORIES.find(c => c.value === category)
  return cat?.label || category
}

// 切换分类
const handleCategoryChange = (category: string) => {
  selectedCategory.value = category
  loadTemplates()
}

// 获取当前用户
const currentUser = ref<any>(null)
try {
  const userStr = localStorage.getItem('user')
  if (userStr) {
    currentUser.value = JSON.parse(userStr)
  }
} catch {}

// 退出登录
const handleLogout = () => {
  localStorage.removeItem('token')
  localStorage.removeItem('user')
  router.push('/login')
}

onMounted(() => {
  loadTemplates()
  loadWorkspaces()
})
</script>

<template>
  <div class="page-container">
    <!-- Header -->
    <header class="header">
      <div class="header-left">
        <img src="/vite.svg" alt="Logo" class="logo-icon" />
        <span class="logo-text">Prompt-Forge</span>
        <a-tag color="purple"><AppstoreOutlined /> 广场</a-tag>
      </div>
      <div class="header-right">
        <a-button @click="router.push('/prompts')">
          <template #icon><FileTextOutlined /></template>
          我的 Prompt
        </a-button>
        <a-button @click="router.push('/arena')">
          <template #icon><ThunderboltOutlined /></template>
          竞技场
        </a-button>
        <span class="username">{{ currentUser?.username }}</span>
        <a-button @click="handleLogout">退出</a-button>
      </div>
    </header>

    <!-- Main Content -->
    <main class="main-content">
      <div class="page-header">
        <div>
          <h1 class="page-title">Prompt 广场</h1>
          <p class="page-desc">发现和使用高质量 Prompt 模板，一键克隆到你的工作空间</p>
        </div>
      </div>

      <!-- 分类筛选 -->
      <div class="category-tabs">
        <button 
          v-for="cat in CATEGORIES" 
          :key="cat.value"
          class="category-tab"
          :class="{ active: selectedCategory === cat.value }"
          @click="handleCategoryChange(cat.value)"
        >
          <span class="cat-icon">{{ cat.icon }}</span>
          <span class="cat-label">{{ cat.label }}</span>
        </button>
      </div>

      <!-- Loading -->
      <div v-if="loading" class="loading">加载中...</div>

      <!-- Template Grid -->
      <div v-else class="template-grid">
        <div 
          v-for="template in templates" 
          :key="template.id" 
          class="template-card"
          @click="openPreview(template)"
        >
          <div class="card-header">
            <span class="template-icon">{{ getCategoryIcon(template.category) }}</span>
            <div class="template-meta">
              <span class="template-name">{{ template.name }}</span>
              <span class="template-category">{{ getCategoryLabel(template.category) }}</span>
            </div>
            <span v-if="template.isOfficial" class="official-badge">官方</span>
          </div>
          <p class="template-desc">{{ template.description || '暂无描述' }}</p>
          <div class="card-footer">
            <div class="footer-left">
              <span class="author">{{ template.authorName || '匿名' }}</span>
              <span class="clone-count"><DownloadOutlined /> {{ template.cloneCount }}</span>
            </div>
            <button class="clone-btn" @click.stop="openCloneDialog(template)">
              Clone
            </button>
          </div>
        </div>
      </div>

      <!-- Empty State -->
      <div v-if="!loading && templates.length === 0" class="empty-state">
        <p>该分类下暂无模板</p>
      </div>
    </main>

    <!-- Clone Dialog -->
    <div v-if="showCloneDialog" class="dialog-overlay" @click.self="showCloneDialog = false">
      <div class="dialog">
        <h3><DownloadOutlined /> 克隆到工作空间</h3>
        <p class="clone-template-name">{{ cloneTargetTemplate?.name }}</p>
        <div class="form-group">
          <label>选择工作空间</label>
          <select v-model="selectedWorkspace" class="workspace-select">
            <option v-for="ws in workspaces" :key="ws.id" :value="ws.id">
              {{ ws.name }}
            </option>
          </select>
        </div>
        <div class="dialog-actions">
          <button class="cancel-btn" @click="showCloneDialog = false">取消</button>
          <button class="submit-btn" @click="handleClone" :disabled="cloning">
            {{ cloning ? '克隆中...' : '确认克隆' }}
          </button>
        </div>
      </div>
    </div>

    <!-- Preview Dialog -->
    <div v-if="showPreview && previewTemplate" class="dialog-overlay" @click.self="showPreview = false">
      <div class="dialog preview-dialog">
        <div class="preview-header">
          <h3>{{ previewTemplate.name }}</h3>
          <button class="close-btn" @click="showPreview = false">×</button>
        </div>
        <p class="preview-desc">{{ previewTemplate.description }}</p>
        <div class="preview-content">
          <pre>{{ previewTemplate.content }}</pre>
        </div>
        <div class="preview-footer">
          <span class="preview-meta">
            {{ getCategoryLabel(previewTemplate.category) }} · 
            {{ previewTemplate.authorName || '匿名' }} · 
            {{ previewTemplate.cloneCount }} 次克隆
          </span>
          <a-button type="primary" size="large" @click="openCloneDialog(previewTemplate); showPreview = false">
            <template #icon><DownloadOutlined /></template>
            Clone 到我的空间
          </a-button>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.page-container {
  min-height: 100vh;
  background: var(--color-bg-primary);
  color: var(--color-text-primary);
}

[data-theme="dark"] .page-container,
:root:not([data-theme="light"]) .page-container {
  background: linear-gradient(135deg, var(--color-bg-primary) 0%, var(--color-bg-tertiary) 100%);
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
  font-size: 24px;
  color: var(--color-primary);
}

.logo-text {
  font-size: 18px;
  font-weight: 600;
  color: var(--color-text-primary);
}

.page-badge {
  margin-left: 12px;
  padding: 4px 12px;
  background: linear-gradient(90deg, #6366f1, #8b5cf6);
  color: white;
  border-radius: 20px;
  font-size: 13px;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 16px;
}

.nav-btn {
  padding: 8px 16px;
  background: transparent;
  border: 1px solid var(--color-border);
  border-radius: 6px;
  color: var(--color-text-tertiary);
  cursor: pointer;
  transition: all 0.2s;
}

.nav-btn:hover {
  border-color: var(--color-primary);
  color: var(--color-primary);
}

.username {
  color: var(--color-text-tertiary);
}

.logout-btn {
  padding: 8px 16px;
  background: transparent;
  border: 1px solid var(--color-border);
  border-radius: 6px;
  color: var(--color-text-tertiary);
  cursor: pointer;
  transition: all 0.2s;
}

.logout-btn:hover {
  border-color: var(--color-primary);
  color: var(--color-primary);
}

.main-content {
  max-width: 1400px;
  margin: 0 auto;
  padding: 32px;
}

.page-header {
  margin-bottom: 24px;
}

.page-title {
  font-size: 28px;
  font-weight: 600;
  margin-bottom: 8px;
  color: var(--color-text-primary);
}

.page-desc {
  color: var(--color-text-tertiary);
}

/* Category Tabs */
.category-tabs {
  display: flex;
  gap: 8px;
  margin-bottom: 24px;
  flex-wrap: wrap;
}

.category-tab {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 16px;
  background: var(--color-bg-elevated);
  border: 1px solid var(--color-border);
  border-radius: 20px;
  color: var(--color-text-secondary);
  cursor: pointer;
  transition: all 0.2s;
}

.category-tab:hover {
  border-color: var(--color-primary);
  color: var(--color-primary);
}

.category-tab.active {
  background: var(--color-primary);
  border-color: var(--color-primary);
  color: white;
}

.cat-icon {
  font-size: 16px;
}

.cat-label {
  font-size: 14px;
}

/* Template Grid */
.template-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
  gap: 20px;
}

.template-card {
  padding: 20px;
  background: var(--color-bg-elevated);
  border: 1px solid var(--color-border);
  border-radius: 12px;
  cursor: pointer;
  transition: all 0.2s;
}

.template-card:hover {
  border-color: var(--color-primary);
  transform: translateY(-2px);
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.15);
}

.card-header {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  margin-bottom: 12px;
}

.template-icon {
  font-size: 32px;
  line-height: 1;
}

.template-meta {
  flex: 1;
}

.template-name {
  display: block;
  font-size: 16px;
  font-weight: 600;
  color: var(--color-text-primary);
  margin-bottom: 4px;
}

.template-category {
  font-size: 12px;
  color: var(--color-text-tertiary);
}

.official-badge {
  padding: 2px 8px;
  background: linear-gradient(90deg, #f59e0b, #f97316);
  color: white;
  border-radius: 10px;
  font-size: 11px;
  font-weight: 500;
}

.template-desc {
  font-size: 14px;
  color: var(--color-text-secondary);
  line-height: 1.5;
  margin-bottom: 16px;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.card-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.footer-left {
  display: flex;
  gap: 12px;
  font-size: 12px;
  color: var(--color-text-tertiary);
}

.clone-btn {
  padding: 6px 16px;
  background: var(--color-primary);
  border: none;
  border-radius: 6px;
  color: white;
  font-size: 13px;
  cursor: pointer;
  transition: all 0.2s;
}

.clone-btn:hover {
  background: var(--color-primary-hover);
}

.clone-btn.large {
  padding: 10px 24px;
  font-size: 14px;
}

/* Dialog */
.dialog-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.7);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 100;
}

.dialog {
  width: 400px;
  padding: 24px;
  background: var(--color-bg-elevated);
  border: 1px solid var(--color-border);
  border-radius: 12px;
}

.preview-dialog {
  width: 700px;
  max-height: 80vh;
  display: flex;
  flex-direction: column;
}

.dialog h3 {
  margin-bottom: 16px;
  font-size: 18px;
  color: var(--color-text-primary);
}

.clone-template-name {
  padding: 12px;
  background: var(--color-bg-secondary);
  border-radius: 8px;
  margin-bottom: 16px;
  font-weight: 500;
}

.form-group {
  margin-bottom: 16px;
}

.form-group label {
  display: block;
  margin-bottom: 8px;
  font-size: 14px;
  color: var(--color-text-tertiary);
}

.workspace-select {
  width: 100%;
  padding: 10px 12px;
  background: var(--color-bg-secondary);
  border: 1px solid var(--color-border);
  border-radius: 8px;
  color: var(--color-text-primary);
  font-size: 14px;
}

.dialog-actions {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  margin-top: 20px;
}

.cancel-btn {
  padding: 10px 20px;
  background: transparent;
  border: 1px solid var(--color-border);
  border-radius: 8px;
  color: var(--color-text-tertiary);
  cursor: pointer;
}

.submit-btn {
  padding: 10px 20px;
  background: var(--color-primary);
  border: none;
  border-radius: 8px;
  color: white;
  cursor: pointer;
}

.submit-btn:disabled {
  opacity: 0.6;
}

/* Preview */
.preview-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.preview-header h3 {
  margin: 0;
}

.close-btn {
  width: 32px;
  height: 32px;
  background: rgba(255, 255, 255, 0.1);
  border: none;
  border-radius: 6px;
  color: var(--color-text-tertiary);
  cursor: pointer;
  font-size: 20px;
}

.close-btn:hover {
  color: var(--color-text-primary);
}

.preview-desc {
  color: var(--color-text-secondary);
  margin-bottom: 16px;
}

.preview-content {
  flex: 1;
  overflow-y: auto;
  max-height: 400px;
  padding: 16px;
  background: var(--color-bg-secondary);
  border-radius: 8px;
  margin-bottom: 16px;
}

.preview-content pre {
  margin: 0;
  white-space: pre-wrap;
  font-family: 'Consolas', monospace;
  font-size: 13px;
  line-height: 1.6;
  color: var(--color-text-primary);
}

.preview-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.preview-meta {
  font-size: 13px;
  color: var(--color-text-tertiary);
}

.loading, .empty-state {
  text-align: center;
  padding: 60px;
  color: var(--color-text-tertiary);
}
</style>
