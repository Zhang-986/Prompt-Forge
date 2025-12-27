<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { getPrompts, createPrompt, deletePrompt, type Prompt } from '../api/prompt'
import { getTags, createTag, deleteTag, getPromptTags, setPromptTags, TAG_COLORS, type Tag } from '../api/tag'
import { exportPrompt, importPromptFile } from '../api/promptExport'
import { ElMessage, ElMessageBox } from 'element-plus'
import WorkspaceSelector from '../components/WorkspaceSelector.vue'
import ThemeToggle from '../components/ThemeToggle.vue'
import type { Workspace } from '../api/workspace'

const router = useRouter()
const loading = ref(false)
const prompts = ref<Prompt[]>([])
const showCreateDialog = ref(false)
const creating = ref(false)
const currentWorkspaceId = ref(1)

// 标签相关状态
const tags = ref<Tag[]>([])
const selectedTagFilter = ref<number | null>(null)
const showTagDialog = ref(false)
const newTagName = ref('')
const newTagColor = ref(TAG_COLORS[0])
const creatingTag = ref(false)
const showTagManagerForPrompt = ref<Prompt | null>(null)
const promptTags = ref<Tag[]>([])
const selectedPromptTags = ref<number[]>([])

// Prompt -> TagIds 的映射 (使用对象以保证 Vue 响应式)
const promptTagsMap = ref<Record<number, number[]>>({})

const newPrompt = ref({
  name: '',
  description: '',
  content: ''
})

// 过滤后的 Prompts
const filteredPrompts = computed(() => {
  if (!selectedTagFilter.value) return prompts.value
  // 根据标签过滤
  return prompts.value.filter(prompt => {
    const tagIds = promptTagsMap.value[prompt.id]
    return tagIds && tagIds.includes(selectedTagFilter.value!)
  })
})

// 加载所有 Prompt 的标签
const loadAllPromptTags = async () => {
  const newMap: Record<number, number[]> = {}
  for (const prompt of prompts.value) {
    try {
      const res = await getPromptTags(prompt.id)
      if (res.code === 200) {
        newMap[prompt.id] = res.data.map((t: Tag) => t.id)
      }
    } catch {}
  }
  // 整体赋值以触发响应式更新
  promptTagsMap.value = newMap
}

// 工作空间切换处理
const handleWorkspaceChange = (workspace: Workspace) => {
  currentWorkspaceId.value = workspace.id
  loadPrompts()
  loadTags()
}

// 加载标签
const loadTags = async () => {
  try {
    const res = await getTags(currentWorkspaceId.value)
    if (res.code === 200) {
      tags.value = res.data
    }
  } catch {}
}

// 创建标签
const handleCreateTag = async () => {
  if (!newTagName.value.trim()) {
    ElMessage.warning('请输入标签名称')
    return
  }
  creatingTag.value = true
  try {
    const res = await createTag({ name: newTagName.value.trim(), color: newTagColor.value }, currentWorkspaceId.value)
    if (res.code === 200) {
      ElMessage.success('标签创建成功')
      newTagName.value = ''
      newTagColor.value = TAG_COLORS[0]
      showTagDialog.value = false
      loadTags()
    }
  } catch {} finally {
    creatingTag.value = false
  }
}

// 删除标签
const handleDeleteTag = async (tag: Tag) => {
  try {
    await ElMessageBox.confirm(`确定要删除标签 "${tag.name}" 吗？`, '确认删除', { type: 'warning' })
    const res = await deleteTag(tag.id)
    if (res.code === 200) {
      ElMessage.success('删除成功')
      loadTags()
    }
  } catch {}
}

// 打开标签管理弹窗
const openTagManager = async (prompt: Prompt) => {
  showTagManagerForPrompt.value = prompt
  try {
    const res = await getPromptTags(prompt.id)
    if (res.code === 200) {
      promptTags.value = res.data
      selectedPromptTags.value = res.data.map((t: Tag) => t.id)
    }
  } catch {}
}

// 保存 Prompt 标签
const savePromptTags = async () => {
  if (!showTagManagerForPrompt.value) return
  const promptId = showTagManagerForPrompt.value.id
  try {
    const res = await setPromptTags(promptId, selectedPromptTags.value)
    if (res.code === 200) {
      // 立即更新本地 promptTagsMap，无需刷新页面
      promptTagsMap.value = {
        ...promptTagsMap.value,
        [promptId]: [...selectedPromptTags.value]
      }
      ElMessage.success('标签已更新')
      showTagManagerForPrompt.value = null
    }
  } catch {}
}

// ==================== 导入/导出 ====================
const showImportDialog = ref(false)
const importFileInput = ref<HTMLInputElement | null>(null)
const importing = ref(false)

// 导出 Prompt
const handleExport = async (prompt: Prompt) => {
  try {
    await exportPrompt(prompt.id)
    ElMessage.success('导出成功')
  } catch (err) {
    ElMessage.error('导出失败')
  }
}

// 触发文件选择
const triggerImportFile = () => {
  importFileInput.value?.click()
}

// 处理文件导入
const handleImportFile = async (event: Event) => {
  const target = event.target as HTMLInputElement
  const file = target.files?.[0]
  if (!file) return
  
  importing.value = true
  try {
    const res = await importPromptFile(file, currentWorkspaceId.value)
    if (res.code === 200) {
      ElMessage.success(`导入成功：${res.data.name}`)
      showImportDialog.value = false
      loadPrompts()
    }
  } catch (err) {
    ElMessage.error('导入失败')
  } finally {
    importing.value = false
    // 清空文件选择
    target.value = ''
  }
}

// 加载 Prompt 列表
const loadPrompts = async () => {
  loading.value = true
  try {
    const res = await getPrompts(currentWorkspaceId.value)
    if (res.code === 200) {
      prompts.value = res.data
      // 加载所有 Prompt 的标签用于筛选
      await loadAllPromptTags()
    }
  } catch (error: any) {
    // 错误已由 request.ts 全局处理
  } finally {
    loading.value = false
  }
}

// 创建 Prompt
const handleCreate = async () => {
  if (!newPrompt.value.name || !newPrompt.value.content) {
    ElMessage.warning('请填写名称和内容')
    return
  }

  creating.value = true
  try {
    const res = await createPrompt({
      name: newPrompt.value.name,
      description: newPrompt.value.description,
      content: newPrompt.value.content,
      workspaceId: currentWorkspaceId.value
    })
    if (res.code === 200) {
      ElMessage.success('创建成功')
      showCreateDialog.value = false
      newPrompt.value = { name: '', description: '', content: '' }
      loadPrompts()
    }
  } catch (error: any) {
    // 错误已由 request.ts 全局处理
  } finally {
    creating.value = false
  }
}

// 删除 Prompt
const handleDelete = async (prompt: Prompt) => {
  try {
    await ElMessageBox.confirm(`确定要删除 "${prompt.name}" 吗？`, '确认删除', {
      type: 'warning'
    })
    
    const res = await deletePrompt(prompt.id)
    if (res.code === 200) {
      ElMessage.success('删除成功')
      loadPrompts()
    }
  } catch (error: any) {
    if (error !== 'cancel') {
      // 错误已由 request.ts 全局处理
    }
  }
}

// 查看版本历史
const viewVersions = (prompt: Prompt) => {
  router.push(`/prompts/${prompt.id}/versions`)
}

// 退出登录
const handleLogout = () => {
  localStorage.removeItem('token')
  localStorage.removeItem('user')
  router.push('/login')
}

// 获取当前用户
const currentUser = ref<any>(null)
try {
  const userStr = localStorage.getItem('user')
  if (userStr) {
    currentUser.value = JSON.parse(userStr)
  }
} catch {}

onMounted(() => {
  loadPrompts()
  loadTags()
})
</script>

<template>
  <div class="page-container">
    <!-- Header -->
    <header class="header">
      <div class="header-left">
        <span class="logo-icon">⬡</span>
        <span class="logo-text">Prompt-Forge</span>
        <WorkspaceSelector 
          v-model="currentWorkspaceId" 
          @change="handleWorkspaceChange" 
        />
      </div>
      <div class="header-right">
        <ThemeToggle />
        <button class="arena-btn" @click="router.push('/arena')">⚔️ 竞技场</button>
        <button class="settings-btn" @click="router.push('/settings/models')">⚙️ 模型配置</button>
        <span class="username">{{ currentUser?.username }}</span>
        <button class="logout-btn" @click="handleLogout">退出</button>
      </div>
    </header>

    <!-- Main Content -->
    <main class="main-content">
      <div class="page-header">
        <div>
          <h1 class="page-title">Prompt 库</h1>
          <p class="page-desc">管理和版本控制您的 Prompt 模板</p>
        </div>
        <div class="header-actions">
          <button class="import-btn" @click="triggerImportFile">📥 导入</button>
          <input 
            ref="importFileInput"
            type="file" 
            accept=".json" 
            style="display: none" 
            @change="handleImportFile"
          />
          <button class="tag-manage-btn" @click="showTagDialog = true">🏷️ 管理标签</button>
          <button class="create-btn" @click="showCreateDialog = true">
            + 新建 Prompt
          </button>
        </div>
      </div>

      <!-- 标签筛选栏 -->
      <div v-if="tags.length > 0" class="tag-filter-bar">
        <span class="filter-label">标签筛选：</span>
        <button 
          class="tag-filter-item" 
          :class="{ active: !selectedTagFilter }"
          @click="selectedTagFilter = null"
        >
          全部
        </button>
        <button 
          v-for="tag in tags" 
          :key="tag.id"
          class="tag-filter-item"
          :class="{ active: selectedTagFilter === tag.id }"
          :style="{ '--tag-color': tag.color }"
          @click="selectedTagFilter = tag.id"
        >
          {{ tag.name }}
        </button>
      </div>

      <!-- Loading -->
      <div v-if="loading" class="loading">加载中...</div>

      <!-- Empty State -->
      <div v-else-if="filteredPrompts.length === 0" class="empty-state">
        <p>暂无 Prompt</p>
        <button class="create-btn" @click="showCreateDialog = true">创建第一个</button>
      </div>

      <!-- Prompt List -->
      <div v-else class="prompt-grid">
        <div 
          v-for="prompt in filteredPrompts" 
          :key="prompt.id" 
          class="prompt-card"
        >
          <div class="card-header">
            <span class="prompt-name">{{ prompt.name }}</span>
            <span class="version-badge">v{{ prompt.latestVersionNumber || 1 }}</span>
          </div>
          <p class="prompt-desc">{{ prompt.description || '暂无描述' }}</p>
          <div class="card-footer">
            <span class="date">{{ new Date(prompt.createdAt).toLocaleDateString() }}</span>
            <div class="actions">
              <button class="action-btn" @click="handleExport(prompt)" title="导出">
                📤
              </button>
              <button class="action-btn" @click="openTagManager(prompt)" title="标签">
                🏷️
              </button>
              <button class="action-btn" @click="viewVersions(prompt)" title="版本历史">
                📜
              </button>
              <button class="action-btn danger" @click="handleDelete(prompt)" title="删除">
                🗑️
              </button>
            </div>
          </div>
        </div>
      </div>
    </main>

    <!-- Create Dialog -->
    <div v-if="showCreateDialog" class="dialog-overlay" @click.self="showCreateDialog = false">
      <div class="dialog">
        <h3>新建 Prompt</h3>
        <form @submit.prevent="handleCreate">
          <div class="form-group">
            <label>名称 *</label>
            <input v-model="newPrompt.name" type="text" placeholder="输入 Prompt 名称" />
          </div>
          <div class="form-group">
            <label>描述</label>
            <input v-model="newPrompt.description" type="text" placeholder="简短描述" />
          </div>
          <div class="form-group">
            <label>内容 *</label>
            <textarea 
              v-model="newPrompt.content" 
              rows="6" 
              placeholder="输入 Prompt 内容，支持 {{variable}} 变量"
            ></textarea>
          </div>
          <div class="dialog-actions">
            <button type="button" class="cancel-btn" @click="showCreateDialog = false">取消</button>
            <button type="submit" class="submit-btn" :disabled="creating">
              {{ creating ? '创建中...' : '创建' }}
            </button>
          </div>
        </form>
      </div>
    </div>

    <!-- 标签管理弹窗 -->
    <div v-if="showTagDialog" class="dialog-overlay" @click.self="showTagDialog = false">
      <div class="dialog tag-dialog">
        <h3>🏷️ 标签管理</h3>
        <div class="tag-create-form">
          <input v-model="newTagName" type="text" placeholder="新标签名称" />
          <div class="color-picker">
            <button 
              v-for="color in TAG_COLORS" 
              :key="color"
              class="color-option"
              :class="{ active: newTagColor === color }"
              :style="{ backgroundColor: color }"
              @click="newTagColor = color"
            ></button>
          </div>
          <button class="add-tag-btn" @click="handleCreateTag" :disabled="creatingTag">
            {{ creatingTag ? '...' : '添加' }}
          </button>
        </div>
        <div class="tag-list">
          <div v-for="tag in tags" :key="tag.id" class="tag-item">
            <span class="tag-badge" :style="{ backgroundColor: tag.color }">{{ tag.name }}</span>
            <button class="delete-tag-btn" @click="handleDeleteTag(tag)">×</button>
          </div>
          <p v-if="tags.length === 0" class="empty-tags">暂无标签</p>
        </div>
        <div class="dialog-actions">
          <button class="cancel-btn" @click="showTagDialog = false">关闭</button>
        </div>
      </div>
    </div>

    <!-- Prompt 标签设置弹窗 -->
    <div v-if="showTagManagerForPrompt" class="dialog-overlay" @click.self="showTagManagerForPrompt = null">
      <div class="dialog tag-dialog">
        <h3>🏷️ 设置标签 - {{ showTagManagerForPrompt.name }}</h3>
        <div class="tag-selection">
          <label 
            v-for="tag in tags" 
            :key="tag.id" 
            class="tag-checkbox"
            :style="{ '--tag-color': tag.color }"
          >
            <input 
              type="checkbox" 
              :value="tag.id" 
              v-model="selectedPromptTags"
            />
            <span class="tag-label" :style="{ backgroundColor: tag.color }">{{ tag.name }}</span>
          </label>
          <p v-if="tags.length === 0" class="empty-tags">
            请先<a href="#" @click.prevent="showTagDialog = true; showTagManagerForPrompt = null">创建标签</a>
          </p>
        </div>
        <div class="dialog-actions">
          <button class="cancel-btn" @click="showTagManagerForPrompt = null">取消</button>
          <button class="submit-btn" @click="savePromptTags">保存</button>
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

/* 深色主题梯度背景 */
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
  font-size: 24px;
  color: var(--color-primary);
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

.arena-btn {
  padding: 8px 16px;
  background: var(--color-primary-gradient);
  border: none;
  border-radius: 6px;
  color: #fff;
  cursor: pointer;
  transition: all 0.2s;
}

.arena-btn:hover {
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(94, 106, 210, 0.4);
}

.settings-btn {
  padding: 8px 16px;
  background: transparent;
  border: 1px solid var(--color-border);
  border-radius: 6px;
  color: var(--color-text-tertiary);
  cursor: pointer;
  transition: all 0.2s;
}

.settings-btn:hover {
  border-color: var(--color-primary);
  color: var(--color-primary);
}

.main-content {
  max-width: 1200px;
  margin: 0 auto;
  padding: 32px;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 32px;
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

.create-btn {
  padding: 12px 24px;
  background: var(--color-primary);
  border: none;
  border-radius: 8px;
  color: #fff;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: background 0.2s;
}

.create-btn:hover {
  background: var(--color-primary-hover);
}

.loading, .empty-state {
  text-align: center;
  padding: 60px 20px;
  color: var(--color-text-tertiary);
}

.prompt-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  gap: 20px;
}

.prompt-card {
  padding: 20px;
  background: var(--color-bg-elevated);
  border: 1px solid var(--color-border);
  border-radius: 12px;
  transition: all 0.2s;
}

.prompt-card:hover {
  border-color: var(--color-border-hover);
  transform: translateY(-2px);
  box-shadow: var(--shadow-md);
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.prompt-name {
  font-size: 16px;
  font-weight: 600;
  color: var(--color-text-primary);
}

.version-badge {
  padding: 4px 8px;
  background: var(--color-primary);
  border-radius: 4px;
  font-size: 12px;
  color: #fff;
}

.prompt-desc {
  color: var(--color-text-tertiary);
  font-size: 14px;
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

.date {
  font-size: 12px;
  color: var(--color-text-muted);
}

.actions {
  display: flex;
  gap: 8px;
}

.action-btn {
  padding: 6px 10px;
  background: var(--color-bg-tertiary);
  border: none;
  border-radius: 6px;
  cursor: pointer;
  font-size: 14px;
  transition: background 0.2s;
}

.action-btn:hover {
  background: var(--color-primary-light);
}

.action-btn.danger:hover {
  background: var(--color-danger-light);
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
  width: 500px;
  padding: 24px;
  background: var(--color-bg-elevated);
  border: 1px solid var(--color-border);
  border-radius: 12px;
}

.dialog h3 {
  margin-bottom: 20px;
  font-size: 18px;
  color: var(--color-text-primary);
}

.form-group {
  margin-bottom: 16px;
}

.form-group label {
  display: block;
  margin-bottom: 6px;
  font-size: 14px;
  color: var(--color-text-tertiary);
}

.form-group input,
.form-group textarea {
  width: 100%;
  padding: 12px;
  background: var(--color-bg-secondary);
  border: 1px solid var(--color-border);
  border-radius: 8px;
  color: var(--color-text-primary);
  font-size: 14px;
  outline: none;
}

.form-group input:focus,
.form-group textarea:focus {
  border-color: var(--color-primary);
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

.cancel-btn:hover {
  border-color: var(--color-border-hover);
}

.submit-btn {
  padding: 10px 20px;
  background: var(--color-primary);
  border: none;
  border-radius: 8px;
  color: #fff;
  cursor: pointer;
}

.submit-btn:hover {
  background: var(--color-primary-hover);
}

.submit-btn:disabled {
  opacity: 0.6;
}

/* ==================== 标签相关样式 ==================== */

.header-actions {
  display: flex;
  gap: 12px;
}

.tag-manage-btn {
  padding: 8px 16px;
  background: transparent;
  border: 1px solid var(--color-border);
  border-radius: 6px;
  color: var(--color-text-tertiary);
  cursor: pointer;
  transition: all 0.2s;
}

.tag-manage-btn:hover {
  border-color: var(--color-primary);
  color: var(--color-primary);
}

.import-btn {
  padding: 8px 16px;
  background: transparent;
  border: 1px solid var(--color-border);
  border-radius: 6px;
  color: var(--color-text-tertiary);
  cursor: pointer;
  transition: all 0.2s;
}

.import-btn:hover {
  border-color: #10b981;
  color: #10b981;
}

.tag-filter-bar {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 16px 0;
  margin-bottom: 16px;
  flex-wrap: wrap;
}

.filter-label {
  color: var(--color-text-tertiary);
  font-size: 14px;
}

.tag-filter-item {
  padding: 6px 14px;
  background: var(--color-bg-elevated);
  border: 1px solid var(--color-border);
  border-radius: 20px;
  color: var(--color-text-secondary);
  font-size: 13px;
  cursor: pointer;
  transition: all 0.2s;
}

.tag-filter-item:hover {
  border-color: var(--tag-color, var(--color-primary));
  color: var(--tag-color, var(--color-primary));
}

.tag-filter-item.active {
  background: var(--tag-color, var(--color-primary));
  border-color: var(--tag-color, var(--color-primary));
  color: #fff;
}

.tag-dialog {
  width: 450px;
  max-width: 90vw;
}

.tag-create-form {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  align-items: center;
  margin-bottom: 16px;
}

.tag-create-form input {
  flex: 1;
  min-width: 150px;
  padding: 10px 12px;
  background: var(--color-bg-secondary);
  border: 1px solid var(--color-border);
  border-radius: 8px;
  color: var(--color-text-primary);
  font-size: 14px;
  outline: none;
}

.color-picker {
  display: flex;
  gap: 6px;
  flex-wrap: wrap;
}

.color-option {
  width: 28px;
  height: 28px;
  border: 2px solid transparent;
  border-radius: 50%;
  cursor: pointer;
  transition: all 0.2s;
  flex-shrink: 0;
}

.color-option.active {
  border-color: #fff;
  transform: scale(1.1);
  box-shadow: 0 0 0 2px var(--color-primary);
}

.add-tag-btn {
  padding: 10px 20px;
  background: var(--color-primary);
  border: none;
  border-radius: 8px;
  color: #fff;
  cursor: pointer;
  font-size: 14px;
  white-space: nowrap;
}

.color-option.active {
  border-color: #fff;
  transform: scale(1.1);
  box-shadow: 0 0 0 2px var(--color-primary);
}

.add-tag-btn {
  padding: 10px 16px;
  background: var(--color-primary);
  border: none;
  border-radius: 8px;
  color: #fff;
  cursor: pointer;
  font-size: 14px;
}

.add-tag-btn:disabled {
  opacity: 0.6;
}

.tag-list {
  max-height: 200px;
  overflow-y: auto;
  margin-bottom: 16px;
}

.tag-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8px 0;
  border-bottom: 1px solid var(--color-border);
}

.tag-item:last-child {
  border-bottom: none;
}

.tag-badge {
  padding: 4px 12px;
  border-radius: 12px;
  color: #fff;
  font-size: 13px;
}

.delete-tag-btn {
  width: 24px;
  height: 24px;
  background: transparent;
  border: none;
  color: var(--color-text-tertiary);
  font-size: 18px;
  cursor: pointer;
  border-radius: 4px;
}

.delete-tag-btn:hover {
  background: rgba(239, 68, 68, 0.2);
  color: #ef4444;
}

.empty-tags {
  color: var(--color-text-tertiary);
  text-align: center;
  padding: 20px;
}

.empty-tags a {
  color: var(--color-primary);
}

.tag-selection {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-bottom: 16px;
  max-height: 200px;
  overflow-y: auto;
}

.tag-checkbox {
  display: flex;
  align-items: center;
  cursor: pointer;
}

.tag-checkbox input {
  display: none;
}

.tag-checkbox .tag-label {
  padding: 6px 14px;
  border-radius: 16px;
  color: #fff;
  font-size: 13px;
  opacity: 0.5;
  transition: all 0.2s;
}

.tag-checkbox input:checked + .tag-label {
  opacity: 1;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.3);
}
</style>
