<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { getTemplates, getCategories, cloneTemplate, updatePlazaTemplate, deletePlazaTemplate, createCategory, updateCategory, deleteCategory, DEFAULT_CATEGORIES, type PromptTemplate, type PlazaCategory } from '../api/plaza'
import { getWorkspaces, type Workspace } from '../api/workspace'
import { message, Modal } from 'ant-design-vue'
import { DownloadOutlined, EditOutlined, DeleteOutlined, SettingOutlined } from '@ant-design/icons-vue'

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
const categories = ref<{ value: string; label: string; id?: number }[]>([
  { value: 'ALL', label: '全部' }
])

// 分类管理弹窗
const showCategoryDialog = ref(false)
const categoryList = ref<PlazaCategory[]>([])
const editingCategory = ref<PlazaCategory | null>(null)
const categoryForm = ref({
  value: '',
  label: '',
  sortOrder: 0
})
const savingCategory = ref(false)

// 加载分类列表
const loadCategories = async () => {
  try {
    const res = await getCategories()
    if (res.code === 200) {
      categories.value = [
        { value: 'ALL', label: '全部' },
        ...res.data.map(c => ({ value: c.value, label: c.label, id: c.id }))
      ]
      categoryList.value = res.data
    }
  } catch (error) {
    console.error('加载分类失败:', error)
    // 使用默认分类作为后备
    categories.value = DEFAULT_CATEGORIES.map(c => ({ value: c.value, label: c.label }))
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
          router.push(`/app/prompts/${res.data.id}/versions`)
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
  categoryForm.value = { value: '', label: '', sortOrder: 0 }
  showCategoryDialog.value = true
}

// 编辑分类
const startEditCategory = (cat: PlazaCategory) => {
  editingCategory.value = cat
  categoryForm.value = {
    value: cat.value,
    label: cat.label,
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
    categoryForm.value = { value: '', label: '', sortOrder: 0 }
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
    <!-- Header Removed -->

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
            <!-- Icon Removed -->
            <div class="template-meta">
              <span class="template-category">{{ getCategoryLabel(template.category) }}</span>
              <span class="template-name">{{ template.name }}</span>
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
              {{ cat.label }}
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
          <div class="form-group">
            <label>排序</label>
            <input v-model.number="categoryForm.sortOrder" type="number" class="form-input" placeholder="0" />
          </div>
        </div>
        <div class="dialog-actions">
          <button v-if="editingCategory" class="cancel-btn"
            @click="editingCategory = null; categoryForm = { value: '', label: '', sortOrder: 0 }">取消编辑</button>
          <button class="submit-btn" @click="handleSaveCategory" :disabled="savingCategory">
            {{ savingCategory ? '保存中...' : (editingCategory ? '更新' : '添加') }}
          </button>
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

.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: var(--space-4) var(--space-8);
  border-bottom: 1px solid var(--color-border);
  background: var(--color-bg-primary);
}

.header-left {
  display: flex;
  align-items: center;
  gap: var(--space-2);
}

.logo-icon {
  width: 28px;
  height: 28px;
  font-size: var(--text-2xl);
  color: var(--color-primary);
}

.logo-text {
  font-size: var(--text-lg);
  font-weight: 600;
  color: var(--color-text-primary);
}

.page-badge {
  margin-left: var(--space-3);
  padding: var(--space-1) var(--space-3);
  background: var(--color-primary);
  color: white;
  border-radius: var(--radius-full);
  font-size: var(--text-sm);
}

.header-right {
  display: flex;
  align-items: center;
  gap: var(--space-4);
}

.nav-btn {
  padding: var(--space-2) var(--space-4);
  background: transparent;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  color: var(--color-text-secondary);
  cursor: pointer;
  transition: all var(--transition-fast);
}

.nav-btn:hover {
  border-color: var(--color-primary);
  color: var(--color-primary);
}

.username {
  color: var(--color-text-secondary);
}

.logout-btn {
  padding: var(--space-2) var(--space-4);
  background: transparent;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  color: var(--color-text-secondary);
  cursor: pointer;
  transition: all var(--transition-fast);
}

.logout-btn:hover {
  border-color: var(--color-primary);
  color: var(--color-primary);
}

.main-content {
  max-width: 960px;
  margin: 0 auto;
  padding: var(--space-8);
}

.page-header {
  margin-bottom: var(--space-6);
}

.page-title {
  font-size: var(--text-2xl);
  font-weight: 600;
  margin-bottom: var(--space-2);
  color: var(--color-text-primary);
}

.page-desc {
  color: var(--color-text-secondary);
}

/* Category Tabs */
.category-tabs {
  display: flex;
  gap: var(--space-2);
  margin-bottom: var(--space-6);
  flex-wrap: wrap;
}

.category-tab {
  display: flex;
  align-items: center;
  gap: var(--space-2);
  padding: 6px 16px;
  background: transparent;
  border: 1px solid transparent;
  border-radius: var(--radius-full);
  color: var(--color-text-secondary);
  cursor: pointer;
  transition: all var(--transition-normal);
  font-weight: 500;
  font-size: 14px;
}

.category-tab:hover {
  background: var(--color-bg-secondary);
  color: var(--color-text-primary);
}

.category-tab.active {
  background: var(--color-text-primary);
  color: var(--color-bg-primary);
  box-shadow: var(--shadow-md);
}

.cat-icon {
  font-size: var(--text-base);
}

.cat-label {
  font-size: var(--text-sm);
}

/* Template Grid */
.template-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  gap: var(--space-5);
}

.template-card {
  padding: var(--space-5);
  background: var(--color-bg-elevated);
  border: 1px solid transparent;
  border-radius: var(--radius-lg);
  cursor: pointer;
  transition: all var(--transition-normal);
  box-shadow: var(--shadow-sm);
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  height: 100%;
}

.template-card:hover {
  transform: translateY(-4px);
  box-shadow: var(--shadow-lg);
  border-color: var(--color-primary-light);
}

.card-header {
  display: flex;
  align-items: flex-start;
  gap: var(--space-3);
  margin-bottom: var(--space-3);
}

.template-icon {
  font-size: var(--text-2xl);
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
}

.template-category {
  font-size: var(--text-xs);
  color: var(--color-text-tertiary);
}

.official-badge {
  padding: 2px var(--space-2);
  background: var(--color-primary);
  color: white;
  border-radius: var(--radius-sm);
  font-size: 11px;
  font-weight: 500;
}

.template-desc {
  font-size: var(--text-sm);
  color: var(--color-text-secondary);
  line-height: 1.5;
  margin-bottom: var(--space-4);
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
  gap: var(--space-3);
  font-size: var(--text-xs);
  color: var(--color-text-tertiary);
}

.clone-btn {
  padding: var(--space-2) var(--space-4);
  background: var(--color-primary);
  border: none;
  border-radius: var(--radius-md);
  color: white;
  font-size: var(--text-sm);
  cursor: pointer;
  transition: all var(--transition-fast);
}

.clone-btn:hover {
  background: var(--color-primary-hover);
}

.clone-btn.large {
  padding: var(--space-3) var(--space-6);
  font-size: var(--text-sm);
}

/* Dialog */
.dialog-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 100;
}

.dialog {
  width: 400px;
  padding: var(--space-6);
  background: var(--color-bg-primary);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
}

.preview-dialog {
  width: 700px;
  max-height: 80vh;
  display: flex;
  flex-direction: column;
}

.dialog h3 {
  margin-bottom: var(--space-4);
  font-size: var(--text-lg);
  color: var(--color-text-primary);
}

.clone-template-name {
  padding: var(--space-3);
  background: var(--color-bg-secondary);
  border-radius: var(--radius-md);
  margin-bottom: var(--space-4);
  font-weight: 500;
}

.form-group {
  margin-bottom: var(--space-4);
}

.form-group label {
  display: block;
  margin-bottom: var(--space-2);
  font-size: var(--text-sm);
  color: var(--color-text-secondary);
}

.workspace-select {
  width: 100%;
  padding: var(--space-3);
  background: var(--color-bg-secondary);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  color: var(--color-text-primary);
  font-size: var(--text-sm);
}

.dialog-actions {
  display: flex;
  justify-content: flex-end;
  gap: var(--space-3);
  margin-top: var(--space-5);
}

.cancel-btn {
  padding: var(--space-3) var(--space-5);
  background: transparent;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  color: var(--color-text-secondary);
  cursor: pointer;
}

.submit-btn {
  padding: var(--space-3) var(--space-5);
  background: var(--color-primary);
  border: none;
  border-radius: var(--radius-md);
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
  margin-bottom: var(--space-3);
}

.preview-header h3 {
  margin: 0;
}

.close-btn {
  width: 32px;
  height: 32px;
  background: var(--color-bg-secondary);
  border: none;
  border-radius: var(--radius-md);
  color: var(--color-text-tertiary);
  cursor: pointer;
  font-size: var(--text-xl);
}

.close-btn:hover {
  color: var(--color-text-primary);
}

.preview-desc {
  color: var(--color-text-secondary);
  margin-bottom: var(--space-4);
}

.preview-content {
  flex: 1;
  overflow-y: auto;
  max-height: 400px;
  padding: var(--space-4);
  background: var(--color-bg-secondary);
  border-radius: var(--radius-md);
  margin-bottom: var(--space-4);
}

.preview-content pre {
  margin: 0;
  white-space: pre-wrap;
  font-family: var(--font-mono);
  font-size: var(--text-sm);
  line-height: 1.6;
  color: var(--color-text-primary);
}

.preview-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.preview-meta {
  font-size: var(--text-sm);
  color: var(--color-text-tertiary);
}

.loading,
.empty-state {
  text-align: center;
  padding: var(--space-12);
  color: var(--color-text-tertiary);
}

/* Admin Action Buttons */
.footer-actions {
  display: flex;
  align-items: center;
  gap: var(--space-2);
}

.action-btn {
  width: 32px;
  height: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--color-bg-secondary);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  color: var(--color-text-tertiary);
  cursor: pointer;
  transition: all var(--transition-fast);
}

.action-btn:hover {
  background: var(--color-bg-tertiary);
  color: var(--color-text-primary);
}

.action-btn.delete-btn:hover {
  background: rgba(239, 68, 68, 0.1);
  color: var(--color-danger);
  border-color: rgba(239, 68, 68, 0.2);
}

/* Edit Dialog */
.edit-dialog {
  width: 600px;
  max-height: 80vh;
  overflow-y: auto;
}

.form-input {
  width: 100%;
  padding: var(--space-3);
  background: var(--color-bg-secondary);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  color: var(--color-text-primary);
  font-size: var(--text-sm);
}

.form-input:focus {
  outline: none;
  border-color: var(--color-primary);
}

.form-textarea {
  width: 100%;
  padding: var(--space-3);
  background: var(--color-bg-secondary);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  color: var(--color-text-primary);
  font-size: var(--text-sm);
  font-family: var(--font-mono);
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
  margin-bottom: var(--space-5);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
}

.category-item {
  display: flex;
  align-items: center;
  gap: var(--space-3);
  padding: var(--space-3);
  border-bottom: 1px solid var(--color-border);
}

.category-item:last-child {
  border-bottom: none;
}

.category-item .cat-icon {
  font-size: var(--text-xl);
}

.category-item .cat-info {
  flex: 1;
  display: flex;
  flex-direction: column;
}

.category-item .cat-info small {
  color: var(--color-text-tertiary);
  font-size: var(--text-xs);
}

.category-item .cat-order {
  color: var(--color-text-tertiary);
  font-size: var(--text-xs);
}

.category-item .cat-actions {
  display: flex;
  gap: var(--space-1);
}

.category-form {
  padding: var(--space-4);
  background: var(--color-bg-secondary);
  border-radius: var(--radius-md);
}

.category-form h4 {
  margin: 0 0 var(--space-3);
  font-size: var(--text-sm);
  color: var(--color-text-secondary);
}

.form-row {
  display: flex;
  gap: var(--space-3);
}

.form-row .form-group {
  flex: 1;
}
</style>
