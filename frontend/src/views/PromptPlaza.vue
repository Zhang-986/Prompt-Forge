<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { getTemplates, getCategories, cloneTemplate, updatePlazaTemplate, deletePlazaTemplate, createCategory, updateCategory, deleteCategory, DEFAULT_CATEGORIES, type PromptTemplate, type PlazaCategory } from '../api/plaza'
import { getWorkspaces, type Workspace } from '../api/workspace'
import { message, Modal } from 'ant-design-vue'
import { AppstoreOutlined, FileTextOutlined, ThunderboltOutlined, DownloadOutlined, EditOutlined, DeleteOutlined, SettingOutlined, PlusOutlined } from '@ant-design/icons-vue'

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

// 管理员编辑弹窗
const showEditDialog = ref(false)
const editTemplate = ref<PromptTemplate | null>(null)
const editForm = ref({
  name: '',
  description: '',
  content: '',
  category: ''
})
const saving = ref(false)

// 动态分类列表
const categories = ref<{ value: string; label: string; icon: string; id?: number }[]>([
  { value: 'ALL', label: '全部', icon: '🌐' }
])

// 分类管理弹窗
const showCategoryDialog = ref(false)
const categoryList = ref<PlazaCategory[]>([])
const editingCategory = ref<PlazaCategory | null>(null)
const categoryForm = ref({
  value: '',
  label: '',
  icon: '📦',
  sortOrder: 0
})
const savingCategory = ref(false)

// 加载分类列表
const loadCategories = async () => {
  try {
    const res = await getCategories()
    if (res.code === 200) {
      categories.value = [
        { value: 'ALL', label: '全部', icon: '🌐' },
        ...res.data.map(c => ({ value: c.value, label: c.label, icon: c.icon, id: c.id }))
      ]
      categoryList.value = res.data
    }
  } catch (error) {
    console.error('加载分类失败:', error)
    // 使用默认分类作为后备
    categories.value = DEFAULT_CATEGORIES.map(c => ({ value: c.value, label: c.label, icon: c.icon }))
  }
}

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
      const firstWs = res.data[0]
      if (firstWs) {
        selectedWorkspace.value = firstWs.id
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
  const cat = categories.value.find(c => c.value === category)
  return cat?.icon || '📦'
}

// 获取分类名称
const getCategoryLabel = (category: string) => {
  const cat = categories.value.find(c => c.value === category)
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
} catch { }

// 是否是管理员
const isAdmin = computed(() => currentUser.value?.role === 'ADMIN')

// 退出登录
const handleLogout = () => {
  localStorage.removeItem('token')
  localStorage.removeItem('user')
  router.push('/login')
}

// ==================== 管理员功能 ====================

// 打开编辑弹窗
const openEditDialog = (template: PromptTemplate) => {
  editTemplate.value = template
  editForm.value = {
    name: template.name,
    description: template.description || '',
    content: template.content,
    category: template.category
  }
  showEditDialog.value = true
}

// 保存编辑
const handleSaveEdit = async () => {
  if (!editTemplate.value) return

  saving.value = true
  try {
    const res = await updatePlazaTemplate(editTemplate.value.id, editForm.value)
    if (res.code === 200) {
      message.success('更新成功')
      showEditDialog.value = false
      loadTemplates() // 刷新列表
    }
  } catch (error: any) {
    message.error(error.response?.data?.message || '更新失败')
  } finally {
    saving.value = false
  }
}

// 删除模板
const handleDelete = (template: PromptTemplate) => {
  Modal.confirm({
    title: '确认删除',
    content: `确定要删除模板「${template.name}」吗？此操作不可恢复。`,
    okText: '删除',
    okType: 'danger',
    cancelText: '取消',
    onOk: async () => {
      try {
        const res = await deletePlazaTemplate(template.id)
        if (res.code === 200) {
          message.success('删除成功')
          loadTemplates() // 刷新列表
        }
      } catch (error: any) {
        message.error(error.response?.data?.message || '删除失败')
      }
    }
  })
}

onMounted(() => {
  loadCategories()
  loadTemplates()
  loadWorkspaces()
})

// ==================== 分类管理 ====================

// 打开分类管理弹窗
const openCategoryDialog = () => {
  editingCategory.value = null
  categoryForm.value = { value: '', label: '', icon: '📦', sortOrder: 0 }
  showCategoryDialog.value = true
}

// 编辑分类
const startEditCategory = (cat: PlazaCategory) => {
  editingCategory.value = cat
  categoryForm.value = {
    value: cat.value,
    label: cat.label,
    icon: cat.icon,
    sortOrder: cat.sortOrder
  }
}

// 保存分类
const handleSaveCategory = async () => {
  if (!categoryForm.value.value || !categoryForm.value.label) {
    message.error('请填写分类值和名称')
    return
  }

  savingCategory.value = true
  try {
    if (editingCategory.value) {
      // 更新
      await updateCategory(editingCategory.value.id, categoryForm.value)
      message.success('更新成功')
    } else {
      // 创建
      await createCategory(categoryForm.value)
      message.success('创建成功')
    }
    editingCategory.value = null
    categoryForm.value = { value: '', label: '', icon: '📦', sortOrder: 0 }
    loadCategories()
  } catch (error: any) {
    message.error(error.response?.data?.message || '保存失败')
  } finally {
    savingCategory.value = false
  }
}

// 删除分类
const handleDeleteCategory = (cat: PlazaCategory) => {
  Modal.confirm({
    title: '确认删除',
    content: `确定要删除分类「${cat.label}」吗？`,
    okText: '删除',
    okType: 'danger',
    cancelText: '取消',
    onOk: async () => {
      try {
        await deleteCategory(cat.id)
        message.success('删除成功')
        loadCategories()
      } catch (error: any) {
        message.error(error.response?.data?.message || '删除失败')
      }
    }
  })
}
</script>

<template>
  <div class="page-container">
    <!-- Header -->
    <header class="header">
      <div class="header-left">
        <img src="/vite.svg" alt="Logo" class="logo-icon" />
        <span class="logo-text">Prompt-Forge</span>
        <a-tag color="purple">
          <AppstoreOutlined /> 广场
        </a-tag>
      </div>
      <div class="header-right">
        <a-button @click="router.push('/prompts')">
          <template #icon>
            <FileTextOutlined />
          </template>
          我的 Prompt
        </a-button>
        <a-button @click="router.push('/arena')">
          <template #icon>
            <ThunderboltOutlined />
          </template>
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
        <button v-for="cat in categories" :key="cat.value" class="category-tab"
          :class="{ active: selectedCategory === cat.value }" @click="handleCategoryChange(cat.value)">
          <span class="cat-icon">{{ cat.icon }}</span>
          <span class="cat-label">{{ cat.label }}</span>
        </button>
        <!-- 管理员设置按钮 -->
        <button v-if="isAdmin" class="category-tab settings-btn" @click="openCategoryDialog" title="管理分类">
          <SettingOutlined />
        </button>
      </div>

      <!-- Loading -->
      <div v-if="loading" class="loading">加载中...</div>

      <!-- Template Grid -->
      <div v-else class="template-grid">
        <div v-for="template in templates" :key="template.id" class="template-card" @click="openPreview(template)">
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
              <span class="clone-count">
                <DownloadOutlined /> {{ template.cloneCount }}
              </span>
            </div>
            <div class="footer-actions">
              <!-- 管理员操作按钮 -->
              <template v-if="isAdmin">
                <button class="action-btn edit-btn" @click.stop="openEditDialog(template)" title="编辑">
                  <EditOutlined />
                </button>
                <button class="action-btn delete-btn" @click.stop="handleDelete(template)" title="删除">
                  <DeleteOutlined />
                </button>
              </template>
              <button class="clone-btn" @click.stop="openCloneDialog(template)">
                Clone
              </button>
            </div>
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
        <h3>
          <DownloadOutlined /> 克隆到工作空间
        </h3>
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
            <template #icon>
              <DownloadOutlined />
            </template>
            Clone 到我的空间
          </a-button>
        </div>
      </div>
    </div>

    <!-- Edit Dialog (Admin) -->
    <div v-if="showEditDialog && editTemplate" class="dialog-overlay" @click.self="showEditDialog = false">
      <div class="dialog edit-dialog">
        <div class="preview-header">
          <h3>
            <EditOutlined /> 编辑模板
          </h3>
          <button class="close-btn" @click="showEditDialog = false">×</button>
        </div>
        <div class="form-group">
          <label>模板名称</label>
          <input v-model="editForm.name" class="form-input" placeholder="请输入模板名称" />
        </div>
        <div class="form-group">
          <label>模板描述</label>
          <input v-model="editForm.description" class="form-input" placeholder="请输入模板描述" />
        </div>
        <div class="form-group">
          <label>分类</label>
          <select v-model="editForm.category" class="workspace-select">
            <option v-for="cat in categories.filter(c => c.value !== 'ALL')" :key="cat.value" :value="cat.value">
              {{ cat.icon }} {{ cat.label }}
            </option>
          </select>
        </div>
        <div class="form-group">
          <label>模板内容</label>
          <textarea v-model="editForm.content" class="form-textarea" rows="8" placeholder="请输入模板内容"></textarea>
        </div>
        <div class="dialog-actions">
          <button class="cancel-btn" @click="showEditDialog = false">取消</button>
          <button class="submit-btn" @click="handleSaveEdit" :disabled="saving">
            {{ saving ? '保存中...' : '保存' }}
          </button>
        </div>
      </div>
    </div>

    <!-- Category Management Dialog (Admin) -->
    <div v-if="showCategoryDialog" class="dialog-overlay" @click.self="showCategoryDialog = false">
      <div class="dialog category-dialog">
        <div class="preview-header">
          <h3>
            <SettingOutlined /> 分类管理
          </h3>
          <button class="close-btn" @click="showCategoryDialog = false">×</button>
        </div>

        <!-- 分类列表 -->
        <div class="category-list">
          <div v-for="cat in categoryList" :key="cat.id" class="category-item">
            <span class="cat-icon">{{ cat.icon }}</span>
            <span class="cat-info">
              <strong>{{ cat.label }}</strong>
              <small>{{ cat.value }}</small>
            </span>
            <span class="cat-order">#{{ cat.sortOrder }}</span>
            <div class="cat-actions">
              <button class="action-btn" @click="startEditCategory(cat)" title="编辑">
                <EditOutlined />
              </button>
              <button class="action-btn delete-btn" @click="handleDeleteCategory(cat)" title="删除">
                <DeleteOutlined />
              </button>
            </div>
          </div>
        </div>

        <!-- 添加/编辑表单 -->
        <div class="category-form">
          <h4>{{ editingCategory ? '编辑分类' : '添加分类' }}</h4>
          <div class="form-row">
            <div class="form-group">
              <label>分类值 (Value)</label>
              <input v-model="categoryForm.value" class="form-input" placeholder="如 WRITING" />
            </div>
            <div class="form-group">
              <label>显示名称</label>
              <input v-model="categoryForm.label" class="form-input" placeholder="如 文案写作" />
            </div>
          </div>
          <div class="form-row">
            <div class="form-group">
              <label>图标 (Emoji)</label>
              <input v-model="categoryForm.icon" class="form-input" placeholder="📦" />
            </div>
            <div class="form-group">
              <label>排序</label>
              <input v-model.number="categoryForm.sortOrder" type="number" class="form-input" placeholder="0" />
            </div>
          </div>
          <div class="dialog-actions">
            <button v-if="editingCategory" class="cancel-btn"
              @click="editingCategory = null; categoryForm = { value: '', label: '', icon: '📦', sortOrder: 0 }">取消编辑</button>
            <button class="submit-btn" @click="handleSaveCategory" :disabled="savingCategory">
              {{ savingCategory ? '保存中...' : (editingCategory ? '更新' : '添加') }}
            </button>
          </div>
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

.loading,
.empty-state {
  text-align: center;
  padding: 60px;
  color: var(--color-text-tertiary);
}

/* Admin Action Buttons */
.footer-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}

.action-btn {
  width: 32px;
  height: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: transparent;
  border: 1px solid var(--color-border);
  border-radius: 6px;
  cursor: pointer;
  transition: all 0.2s;
  color: var(--color-text-tertiary);
}

.action-btn:hover {
  border-color: var(--color-primary);
  color: var(--color-primary);
}

.action-btn.delete-btn:hover {
  border-color: #ef4444;
  color: #ef4444;
}

/* Edit Dialog */
.edit-dialog {
  width: 600px;
  max-height: 80vh;
  overflow-y: auto;
}

.form-input {
  width: 100%;
  padding: 10px 12px;
  background: var(--color-bg-secondary);
  border: 1px solid var(--color-border);
  border-radius: 8px;
  color: var(--color-text-primary);
  font-size: 14px;
}

.form-input:focus {
  outline: none;
  border-color: var(--color-primary);
}

.form-textarea {
  width: 100%;
  padding: 10px 12px;
  background: var(--color-bg-secondary);
  border: 1px solid var(--color-border);
  border-radius: 8px;
  color: var(--color-text-primary);
  font-size: 14px;
  font-family: 'Consolas', monospace;
  resize: vertical;
  min-height: 120px;
}

.form-textarea:focus {
  outline: none;
  border-color: var(--color-primary);
}

/* Category Management Dialog */
.category-dialog {
  width: 600px;
  max-height: 80vh;
  overflow-y: auto;
}

.settings-btn {
  background: transparent !important;
  border-style: dashed !important;
}

.category-list {
  max-height: 250px;
  overflow-y: auto;
  margin-bottom: 20px;
  border: 1px solid var(--color-border);
  border-radius: 8px;
}

.category-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px;
  border-bottom: 1px solid var(--color-border);
}

.category-item:last-child {
  border-bottom: none;
}

.category-item .cat-icon {
  font-size: 20px;
}

.category-item .cat-info {
  flex: 1;
  display: flex;
  flex-direction: column;
}

.category-item .cat-info small {
  color: var(--color-text-tertiary);
  font-size: 12px;
}

.category-item .cat-order {
  color: var(--color-text-tertiary);
  font-size: 12px;
}

.category-item .cat-actions {
  display: flex;
  gap: 4px;
}

.category-form {
  padding: 16px;
  background: var(--color-bg-secondary);
  border-radius: 8px;
}

.category-form h4 {
  margin: 0 0 12px;
  font-size: 14px;
  color: var(--color-text-secondary);
}

.form-row {
  display: flex;
  gap: 12px;
}

.form-row .form-group {
  flex: 1;
}
</style>
