<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { getPrompts, createPrompt, deletePrompt, updatePrompt, getLatestVersion, type Prompt } from '../api/prompt'
import { getTags, createTag, deleteTag, getPromptTags, setPromptTags, getAllPromptTagMappings, TAG_COLORS, type Tag } from '../api/tag'
import { exportPrompt, importPromptFile } from '../api/promptExport'
import { optimizePrompt } from '../api/optimize'
import { publishToPlaza, DEFAULT_CATEGORIES } from '../api/plaza'
import { getAvailableModels, type AvailableModelInfo } from '../api/arena'
import { message, Modal } from 'ant-design-vue'
import {
  PlusOutlined, ImportOutlined, TagsOutlined, ExportOutlined, SendOutlined,
  EditOutlined, HistoryOutlined, DeleteOutlined, RobotOutlined,
} from '@ant-design/icons-vue'

const router = useRouter()
const loading = ref(false)
const prompts = ref<Prompt[]>([])
const showCreateDialog = ref(false)
const creating = ref(false)
const currentWorkspaceId = ref<number>()

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

// Prompt -> TagIds 的映射
const promptTagsMap = ref<Record<number, number[]>>({})

const newPrompt = ref({
  name: '',
  description: '',
  content: ''
})

const optimizing = ref(false)

// AI 优化模型选择
const showModelSelectModal = ref(false)
const availableModels = ref<AvailableModelInfo[]>([])
const selectedOptimizeModel = ref<string>('')
const loadingModels = ref(false)

const openOptimizeModal = async () => {
  if (!newPrompt.value.content.trim()) {
    message.warning('请先输入一些内容作为基础，AI 才能帮你优化')
    return
  }

  // Check for Default Model Preference
  const defaultModel = localStorage.getItem('PF_DEFAULT_OPTIMIZE_MODEL')
  if (defaultModel) {
    selectedOptimizeModel.value = defaultModel
    await handleOptimize()
    return
  }

  // 加载可用模型
  loadingModels.value = true
  try {
    const res = await getAvailableModels()
    if (res.code === 200 && res.data.length > 0) {
      availableModels.value = res.data
      const firstModel = res.data[0]
      if (firstModel) {
        selectedOptimizeModel.value = firstModel.provider // 默认选第一个
      }
      showModelSelectModal.value = true
    } else {
      message.error('请先在模型配置中添加至少一个 AI 模型')
    }
  } catch (error) {
    message.error('加载模型列表失败')
  } finally {
    loadingModels.value = false
  }
}

const handleOptimize = async () => {
  if (!selectedOptimizeModel.value) {
    message.warning('请选择一个模型')
    return
  }

  showModelSelectModal.value = false
  optimizing.value = true
  try {
    const res = await optimizePrompt(newPrompt.value.content, selectedOptimizeModel.value)
    if (res.code === 200) {
      newPrompt.value.content = res.data
      message.success('优化完成')
    } else {
      message.error(res.message || '优化失败')
    }
  } catch (error) {
    message.error('AI 服务暂时不可用')
  } finally {
    optimizing.value = false
  }
}

// 详情弹窗
const showDetailModal = ref(false)
const detailPrompt = ref<Prompt | null>(null)
const detailContent = ref('')
const loadingDetail = ref(false)

const openDetailModal = async (prompt: Prompt) => {
  detailPrompt.value = prompt
  showDetailModal.value = true
  detailContent.value = ''
  loadingDetail.value = true

  try {
    const res = await getLatestVersion(prompt.id)
    if (res.code === 200) {
      detailContent.value = res.data.content
    }
  } catch (error) {
    console.error(error)
  } finally {
    loadingDetail.value = false
  }
}

// 过滤后的 Prompts
const filteredPrompts = computed(() => {
  if (!selectedTagFilter.value) return prompts.value
  return prompts.value.filter(prompt => {
    const tagIds = promptTagsMap.value[prompt.id]
    return tagIds && tagIds.includes(selectedTagFilter.value!)
  })
})

// 加载所有 Prompt 的标签 (批量获取，减少 N+1 请求)
const loadAllPromptTags = async () => {
  if (currentWorkspaceId.value === undefined) return
  try {
    const res = await getAllPromptTagMappings(currentWorkspaceId.value)
    if (res.code === 200) {
      promptTagsMap.value = res.data || {}
    }
  } catch { }
}

// 工作空间切换处理

// 加载标签
const loadTags = async () => {
  if (currentWorkspaceId.value === undefined) return
  try {
    const res = await getTags(currentWorkspaceId.value)
    if (res.code === 200) {
      tags.value = res.data
    }
  } catch { }
}

// 创建标签
const handleCreateTag = async () => {
  if (!newTagName.value.trim()) {
    message.warning('请输入标签名称')
    return
  }
  creatingTag.value = true
  try {
    const res = await createTag({ name: newTagName.value.trim(), color: newTagColor.value }, currentWorkspaceId.value!)
    if (res.code === 200) {
      message.success('标签创建成功')
      newTagName.value = ''
      newTagColor.value = TAG_COLORS[0]
      showTagDialog.value = false
      loadTags()
    }
  } catch { } finally {
    creatingTag.value = false
  }
}

// 删除标签
const handleDeleteTag = async (tag: Tag) => {
  Modal.confirm({
    title: '确认删除',
    content: `确定要删除标签 "${tag.name}" 吗？`,
    okType: 'danger',
    onOk: async () => {
      const res = await deleteTag(tag.id)
      if (res.code === 200) {
        message.success('删除成功')
        loadTags()
      }
    }
  })
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
  } catch { }
}

// 保存 Prompt 标签
const savePromptTags = async () => {
  if (!showTagManagerForPrompt.value) return
  const promptId = showTagManagerForPrompt.value.id
  try {
    const res = await setPromptTags(promptId, selectedPromptTags.value)
    if (res.code === 200) {
      promptTagsMap.value = {
        ...promptTagsMap.value,
        [promptId]: [...selectedPromptTags.value]
      }
      message.success('标签已更新')
      showTagManagerForPrompt.value = null
    }
  } catch { }
}

// ==================== 导入/导出 ====================
const importFileInput = ref<HTMLInputElement | null>(null)
const importing = ref(false)

// 导出 Prompt
const handleExport = async (prompt: Prompt) => {
  try {
    await exportPrompt(prompt.id)
    message.success('导出成功')
  } catch (err) {
    message.error('导出失败')
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
      message.success(`导入成功：${res.data.name}`)
      loadPrompts()
    }
  } catch (err) {
    message.error('导入失败')
  } finally {
    importing.value = false
    target.value = ''
  }
}

// ==================== 发布到广场 ====================
const showPublishDialog = ref(false)
const publishTarget = ref<Prompt | null>(null)
const publishCategory = ref('WRITING')
const publishing = ref(false)

const openPublishDialog = (prompt: Prompt) => {
  publishTarget.value = prompt
  showPublishDialog.value = true
}

const handlePublish = async () => {
  if (!publishTarget.value) return

  publishing.value = true
  try {
    const user = JSON.parse(localStorage.getItem('user') || '{}')
    const res = await publishToPlaza(
      publishTarget.value.id,
      publishCategory.value,
      user.username || '匿名'
    )
    if (res.code === 200) {
      message.success('发布成功！你的 Prompt 已上架到广场')
      showPublishDialog.value = false
    } else {
      message.error(res.message || '发布失败')
    }
  } catch (error: any) {
    message.error(error.response?.data?.message || '发布失败')
  } finally {
    publishing.value = false
  }
}

// ==================== 编辑 Prompt ====================
const showEditDialog = ref(false)
const editTarget = ref<Prompt | null>(null)
const editForm = ref({ name: '', description: '' })
const editing = ref(false)

const openEditDialog = (prompt: Prompt) => {
  editTarget.value = prompt
  editForm.value = {
    name: prompt.name,
    description: prompt.description || ''
  }
  showEditDialog.value = true
}

const handleEdit = async () => {
  if (!editTarget.value || !editForm.value.name.trim()) {
    message.warning('名称不能为空')
    return
  }

  editing.value = true
  try {
    const res = await updatePrompt(editTarget.value.id, {
      name: editForm.value.name.trim(),
      description: editForm.value.description.trim()
    })
    if (res.code === 200) {
      message.success('更新成功')
      showEditDialog.value = false
      await loadPrompts()
    } else {
      message.error(res.message || '更新失败')
    }
  } catch (error: any) {
    message.error(error.response?.data?.message || '更新失败')
  } finally {
    editing.value = false
  }
}

// 加载 Prompt 列表
const loadPrompts = async () => {
  if (!currentWorkspaceId.value) return



  loading.value = true
  try {
    const res = await getPrompts(currentWorkspaceId.value)
    if (res.code === 200) {
      prompts.value = res.data
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
    message.warning('请填写名称和内容')
    return
  }

  creating.value = true
  try {
    const res = await createPrompt({
      name: newPrompt.value.name,
      description: newPrompt.value.description,
      content: newPrompt.value.content,
      workspaceId: currentWorkspaceId.value!
    })
    if (res.code === 200) {
      message.success('创建成功')
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
const handleDelete = (prompt: Prompt) => {
  Modal.confirm({
    title: '确认删除',
    content: `确定要删除 "${prompt.name}" 吗？`,
    okType: 'danger',
    onOk: async () => {
      const res = await deletePrompt(prompt.id)
      if (res.code === 200) {
        message.success('删除成功')
        loadPrompts()
      }
    }
  })
}

// 查看版本历史
const viewVersions = (prompt: Prompt) => {
  router.push({ name: 'VersionHistory', params: { id: prompt.id } })
}

// 退出登录

// 获取当前用户
const currentUser = ref<any>(null)
try {
  const userStr = localStorage.getItem('user')
  if (userStr) {
    currentUser.value = JSON.parse(userStr)
  }
} catch { }

onMounted(() => {
  const wsId = localStorage.getItem('currentWorkspaceId')
  if (wsId) {
    currentWorkspaceId.value = parseInt(wsId)
    loadPrompts()
    loadTags()
  } else {
    // try default load or just wait
    // loadTags() relies on currentWorkspaceId, so no point calling it if empty
  }
})
</script>

<template>
  <div class="page-container">
    <!-- Header Removed -->

    <!-- Main Content -->
    <main class="main-content">
      <div class="page-header">
        <div>
          <h1 class="page-title">Prompt 库</h1>
          <p class="page-desc">管理和版本控制您的 Prompt 模板</p>
        </div>
        <a-space>
          <input ref="importFileInput" type="file" accept=".json" style="display: none" @change="handleImportFile" />
          <a-button @click="triggerImportFile" :loading="importing">
            <template #icon>
              <ImportOutlined />
            </template>
            导入
          </a-button>
          <a-button @click="showTagDialog = true">
            <template #icon>
              <TagsOutlined />
            </template>
            管理标签
          </a-button>
          <a-button type="primary" @click="showCreateDialog = true">
            <template #icon>
              <PlusOutlined />
            </template>
            新建 Prompt
          </a-button>
        </a-space>
      </div>

      <!-- 标签筛选栏 -->
      <div v-if="tags.length > 0" class="tag-filter-bar">
        <span class="filter-label">标签筛选：</span>
        <a-tag :bordered="false" :color="!selectedTagFilter ? 'purple' : 'default'" class="filter-tag"
          @click="selectedTagFilter = null">
          全部
        </a-tag>
        <a-tag v-for="tag in tags" :key="tag.id" :bordered="false"
          :color="selectedTagFilter === tag.id ? tag.color : 'default'" class="filter-tag"
          @click="selectedTagFilter = tag.id">
          {{ tag.name }}
        </a-tag>
      </div>

      <!-- Loading -->
      <div v-if="loading" class="loading-container">
        <a-spin size="large" />
      </div>

      <!-- Empty State -->
      <a-empty v-else-if="filteredPrompts.length === 0" description="暂无 Prompt">
        <a-button type="primary" @click="showCreateDialog = true">
          <template #icon>
            <PlusOutlined />
          </template>
          创建第一个
        </a-button>
      </a-empty>

      <!-- Prompt List -->
      <div v-else class="prompt-grid">
        <a-card v-for="prompt in filteredPrompts" :key="prompt.id" :bordered="false" class="prompt-card" hoverable
          @click="openDetailModal(prompt)">
          <template #title>
            <div class="card-title">
              <span class="prompt-name">{{ prompt.name }}</span>
              <a-tag class="version-tag">v{{ prompt.latestVersionNumber || 1 }}</a-tag>
            </div>
          </template>
          <template #extra>
            <span class="date">{{ new Date(prompt.createdAt).toLocaleDateString() }}</span>
          </template>
          <p class="prompt-desc">{{ prompt.description || '暂无描述' }}</p>
          <template #actions>
            <a-tooltip title="导出">
              <a-button type="text" size="small" @click.stop="handleExport(prompt)">
                <template #icon>
                  <ExportOutlined />
                </template>
              </a-button>
            </a-tooltip>
            <a-tooltip title="发布到广场">
              <a-button type="text" size="small" @click.stop="openPublishDialog(prompt)">
                <template #icon>
                  <SendOutlined />
                </template>
              </a-button>
            </a-tooltip>
            <a-tooltip title="编辑">
              <a-button type="text" size="small" @click.stop="openEditDialog(prompt)">
                <template #icon>
                  <EditOutlined />
                </template>
              </a-button>
            </a-tooltip>
            <a-tooltip title="标签">
              <a-button type="text" size="small" @click.stop="openTagManager(prompt)">
                <template #icon>
                  <TagsOutlined />
                </template>
              </a-button>
            </a-tooltip>
            <a-tooltip title="版本历史">
              <a-button type="text" size="small" @click.stop="viewVersions(prompt)">
                <template #icon>
                  <HistoryOutlined />
                </template>
              </a-button>
            </a-tooltip>
            <a-tooltip title="删除">
              <a-button type="text" size="small" danger @click.stop="handleDelete(prompt)">
                <template #icon>
                  <DeleteOutlined />
                </template>
              </a-button>
            </a-tooltip>
          </template>
        </a-card>
      </div>
    </main>

    <!-- Create Dialog -->
    <a-modal v-model:open="showCreateDialog" title="新建 Prompt" :footer="null" width="600px">
      <a-form layout="vertical" :model="newPrompt" @finish="handleCreate">
        <a-form-item label="名称" name="name" :rules="[{ required: true, message: '请输入名称' }]">
          <a-input v-model:value="newPrompt.name" placeholder="输入 Prompt 名称" />
        </a-form-item>
        <a-form-item label="描述" name="description">
          <a-input v-model:value="newPrompt.description" placeholder="简短描述" />
        </a-form-item>
        <a-form-item label="内容" name="content" :rules="[{ required: true, message: '请输入内容' }]">
          <div class="editor-header">
            <a-button size="small" @click="openOptimizeModal" :loading="optimizing || loadingModels"
              :disabled="!newPrompt.content">
              <template #icon>
                <RobotOutlined />
              </template>
              AI 优化
            </a-button>
          </div>
          <a-textarea v-model:value="newPrompt.content" :rows="6" placeholder="输入 Prompt 内容，支持 {{variable}} 变量" />
        </a-form-item>
        <a-form-item>
          <a-space>
            <a-button @click="showCreateDialog = false">取消</a-button>
            <a-button type="primary" html-type="submit" :loading="creating">创建</a-button>
          </a-space>
        </a-form-item>
      </a-form>
    </a-modal>

    <!-- Tag Management Dialog -->
    <a-modal v-model:open="showTagDialog" title="管理标签" :footer="null" width="500px">
      <div class="tag-create-form">
        <a-input v-model:value="newTagName" placeholder="新标签名称" style="flex: 1" />
        <div class="color-picker">
          <span v-for="color in TAG_COLORS" :key="color" class="color-option" :class="{ active: newTagColor === color }"
            :style="{ background: color }" @click="newTagColor = color" />
        </div>
        <a-button type="primary" @click="handleCreateTag" :loading="creatingTag">
          <template #icon>
            <PlusOutlined />
          </template>
          创建
        </a-button>
      </div>
      <a-divider />
      <div class="existing-tags">
        <a-tag v-for="tag in tags" :key="tag.id" :bordered="false" :color="tag.color" closable
          @close="handleDeleteTag(tag)">
          {{ tag.name }}
        </a-tag>
        <a-empty v-if="tags.length === 0" description="暂无标签" :image-style="{ height: '60px' }" />
      </div>
    </a-modal>

    <!-- Prompt Tag Manager Dialog -->
    <a-modal v-model:open="showTagManagerForPrompt" title="为 Prompt 添加标签" @ok="savePromptTags" width="400px">
      <a-checkbox-group v-model:value="selectedPromptTags" style="width: 100%">
        <a-row :gutter="[12, 12]">
          <a-col v-for="tag in tags" :key="tag.id" :span="12">
            <a-checkbox :value="tag.id">
              <a-tag :bordered="false" :color="tag.color">{{ tag.name }}</a-tag>
            </a-checkbox>
          </a-col>
        </a-row>
      </a-checkbox-group>
      <a-empty v-if="tags.length === 0" description="暂无标签，请先创建" />
    </a-modal>

    <!-- Publish Dialog -->
    <a-modal v-model:open="showPublishDialog" title="发布到广场" @ok="handlePublish" :confirmLoading="publishing"
      okText="确认发布">
      <p class="publish-prompt-name">{{ publishTarget?.name }}</p>
      <a-form-item label="选择分类">
        <a-select v-model:value="publishCategory" style="width: 100%">
          <a-select-option v-for="cat in DEFAULT_CATEGORIES.filter(c => c.value !== 'ALL')" :key="cat.value"
            :value="cat.value">
            {{ cat.label }}
          </a-select-option>
        </a-select>
      </a-form-item>
      <p class="publish-hint">发布后，其他用户可以在广场中看到并克隆你的 Prompt</p>
    </a-modal>

    <!-- Edit Dialog -->
    <a-modal v-model:open="showEditDialog" title="编辑 Prompt" @ok="handleEdit" :confirmLoading="editing" okText="保存">
      <a-form layout="vertical">
        <a-form-item label="名称" required>
          <a-input v-model:value="editForm.name" placeholder="输入名称" />
        </a-form-item>
        <a-form-item label="描述">
          <a-input v-model:value="editForm.description" placeholder="简短描述" />
        </a-form-item>
      </a-form>
    </a-modal>

    <!-- Model Selection Modal for AI Optimize -->
    <a-modal v-model:open="showModelSelectModal" title="选择优化模型" :footer="null" width="400px">
      <div class="model-select-content">
        <p class="model-hint">请选择用于优化 Prompt 的 AI 模型：</p>
        <a-radio-group v-model:value="selectedOptimizeModel" class="model-radio-group">
          <a-radio v-for="model in availableModels" :key="model.provider" :value="model.provider"
            class="model-radio-item">
            {{ model.displayName }}
          </a-radio>
        </a-radio-group>
        <div class="modal-actions">
          <a-button @click="showModelSelectModal = false">取消</a-button>
          <a-button type="primary" @click="handleOptimize" :loading="optimizing">
            开始优化
          </a-button>
        </div>
      </div>
    </a-modal>

    <!-- Detail Modal -->
    <a-modal v-model:open="showDetailModal" :title="null" :footer="null" width="600px" wrapClassName="rounded-modal">
      <div v-if="detailPrompt" class="detail-container">

        <div class="detail-header">
          <h3>{{ detailPrompt.name }}</h3>
          <div class="detail-meta-badges">
            <a-tag color="blue">v{{ detailPrompt.latestVersionNumber || 1 }}</a-tag>
            <span class="detail-date">{{ new Date(detailPrompt.updatedAt || detailPrompt.createdAt).toLocaleDateString()
            }}</span>
          </div>
          <button class="close-btn-absolute" @click="showDetailModal = false">×</button>
        </div>

        <div class="detail-body">
          <div class="detail-section" v-if="detailPrompt.description">
            <h4 class="section-title">描述</h4>
            <p class="description-text">{{ detailPrompt.description }}</p>
          </div>

          <div class="detail-section">
            <h4 class="section-title">Prompt 内容</h4>
            <div class="code-preview-box">
              <a-spin v-if="loadingDetail" />
              <pre v-else class="code-content">{{ detailContent || '暂无内容' }}</pre>
            </div>
          </div>

          <div class="detail-section" v-if="promptTagsMap[detailPrompt.id]?.length">
            <h4 class="section-title">标签</h4>
            <div class="tags-row">
              <a-tag v-for="tagId in promptTagsMap[detailPrompt.id]" :key="tagId" :bordered="false"
                :color="tags.find(t => t.id === tagId)?.color || 'default'">
                {{tags.find(t => t.id === tagId)?.name}}
              </a-tag>
            </div>
          </div>
        </div>

        <div class="detail-footer">
          <a-button class="modal-btn" @click="showDetailModal = false">关闭</a-button>
          <a-button type="primary" class="modal-btn primary" @click="viewVersions(detailPrompt)">
            <template #icon>
              <HistoryOutlined />
            </template>
            查看历史版本
          </a-button>
        </div>

      </div>
    </a-modal>
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
  padding: 0 var(--space-6);
  background: var(--color-bg-secondary) !important;
  border-bottom: 1px solid var(--color-border-light);
  height: 56px;
}

.header-left {
  display: flex;
  align-items: center;
  gap: var(--space-6);
}

.logo {
  display: flex;
  align-items: center;
  gap: var(--space-2);
}

.logo-icon {
  width: 28px;
  height: 28px;
}

.logo-text {
  font-size: var(--text-lg);
  font-weight: 600;
  color: var(--color-text-primary);
}

.header-right {
  display: flex;
  align-items: center;
}

.username {
  color: var(--color-text-secondary);
  margin-right: var(--space-2);
}

.main-content {
  max-width: 960px;
  margin: 0 auto;
  padding: var(--space-8);
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
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

/* Tag Filter Bar */
.tag-filter-bar {
  display: flex;
  align-items: center;
  gap: var(--space-2);
  margin-bottom: var(--space-6);
  flex-wrap: wrap;
}

.filter-label {
  color: var(--color-text-secondary);
  font-size: var(--text-sm);
}

.filter-tag {
  cursor: pointer;
  transition: all var(--transition-fast);
}

.filter-tag:hover {
  opacity: 0.8;
}

/* Loading */
.loading-container {
  display: flex;
  justify-content: center;
  align-items: center;
  padding: var(--space-12);
}

/* Prompt Grid */
.prompt-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  gap: var(--space-5);
}

.prompt-card {
  background: var(--color-bg-elevated);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-sm);
  transition: all var(--transition-normal);
  border: 1px solid transparent;
}

.prompt-card:hover {
  transform: translateY(-4px);
  box-shadow: var(--shadow-lg);
  border-color: var(--color-primary-light);
}

.prompt-card :deep(.ant-card-head) {
  border-bottom: none;
  padding: 0 24px;
  min-height: auto;
  padding-top: 20px;
}

.prompt-card :deep(.ant-card-body) {
  padding: 12px 24px;
}

.prompt-card :deep(.ant-card-actions) {
  background: transparent;
  border-top: none;
  opacity: 0.6;
  transition: opacity 0.2s;
}

.prompt-card:hover :deep(.ant-card-actions) {
  opacity: 1;
}

.prompt-card :deep(.ant-card-actions > li) {
  margin: var(--space-2) 0;
}

.card-title {
  display: flex;
  align-items: center;
  gap: var(--space-2);
}

.prompt-name {
  font-weight: 500;
  color: var(--color-text-primary);
}

.date {
  font-size: var(--text-xs);
  color: var(--color-text-secondary);
}

.prompt-desc {
  color: var(--color-text-secondary);
  font-size: var(--text-sm);
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  margin: 0;
}

/* Tag Create Form */
.tag-create-form {
  display: flex;
  align-items: center;
  gap: var(--space-3);
}

.color-picker {
  display: flex;
  gap: var(--space-2);
}

.color-option {
  width: 24px;
  height: 24px;
  border-radius: var(--radius-sm);
  cursor: pointer;
  border: 2px solid transparent;
  transition: all var(--transition-fast);
}

.color-option:hover {
  transform: scale(1.1);
}

.color-option.active {
  border-color: var(--color-text-primary);
}

.existing-tags {
  display: flex;
  flex-wrap: wrap;
  gap: var(--space-2);
}

/* Editor Header */
.editor-header {
  display: flex;
  justify-content: flex-end;
  margin-bottom: var(--space-2);
}

/* Publish */
.publish-prompt-name {
  padding: var(--space-3);
  background: var(--color-primary-muted);
  border-radius: var(--radius-md);
  margin-bottom: var(--space-4);
  font-weight: 500;
  color: var(--color-text-primary);
}

.publish-hint {
  font-size: var(--text-sm);
  color: var(--color-text-secondary);
  margin-top: var(--space-3);
}
</style>

<style scoped>
/* Modal Styling */
.model-select-content {
  padding: var(--space-5) 0;
}

.model-hint {
  margin-bottom: var(--space-4);
  color: var(--color-text-primary);
  font-size: var(--text-sm);
}

.model-radio-group {
  display: flex !important;
  flex-direction: column;
  gap: var(--space-3);
  width: 100%;
}

.model-radio-item {
  display: flex;
  align-items: center;
  padding: var(--space-3);
  border: 1px solid var(--color-border-light);
  border-radius: var(--radius-md);
  transition: all var(--transition-fast);
  color: var(--color-text-primary);
}

.model-radio-item:hover {
  border-color: var(--color-primary);
  background: var(--color-primary-muted);
}

.modal-actions {
  margin-top: var(--space-6);
  display: flex;
  justify-content: flex-end;
  gap: var(--space-3);
}
</style>

<style scoped>
/* Detail Modal */
.detail-section {
  margin-bottom: var(--space-5);
}

.detail-label {
  font-size: var(--text-sm);
  color: var(--color-text-secondary);
  margin-bottom: var(--space-2);
}

.detail-value {
  color: var(--color-text-primary);
}

.text-desc {
  font-style: italic;
  color: var(--color-text-secondary);
}

.content-box {
  background: var(--color-bg-tertiary);
  padding: var(--space-3);
  border-radius: var(--radius-md);
  border: 1px solid var(--color-border-light);
}

.content-box pre {
  margin: 0;
  white-space: pre-wrap;
  word-break: break-all;
  font-family: var(--font-mono);
  font-size: var(--text-sm);
  line-height: 1.5;
}

.tags-row {
  display: flex;
  gap: var(--space-2);
  flex-wrap: wrap;
}

.detail-meta {
  display: flex;
  justify-content: space-between;
  margin-top: var(--space-6);
  padding-top: var(--space-4);
  border-top: 1px solid var(--color-border-light);
  font-size: var(--text-xs);
  color: var(--color-text-secondary);
}

.detail-actions {
  display: flex;
  justify-content: flex-end;
  gap: var(--space-3);
  margin-top: var(--space-6);
}

/* Refined UI Elements for Premium Feel */
.version-tag {
  background: var(--color-bg-secondary);
  border: 1px solid var(--color-border);
  color: var(--color-text-secondary);
  font-size: 11px;
  font-weight: 500;
  border-radius: 4px;
  padding: 0 6px;
  line-height: 20px;
}

.delete-btn {
  color: var(--color-text-tertiary);
  transition: all var(--transition-fast);
}

.delete-btn:hover {
  color: var(--color-danger);
  background: rgba(239, 68, 68, 0.1);
}

.logout-btn {
  color: var(--color-text-secondary);
}

.logout-btn:hover {
  color: var(--color-text-primary);
  background: var(--color-bg-tertiary);
}

/* Detail Modal Styles */
.detail-container {
  /* No padding here, use inner sections */
}

.detail-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: var(--space-5) var(--space-6);
  border-bottom: 1px solid var(--color-border);
  position: relative;
}

.detail-header h3 {
  font-size: 18px;
  font-weight: 600;
  margin: 0;
  color: var(--color-text-primary);
}

.detail-meta-badges {
  flex: 1;
  display: flex;
  align-items: center;
  gap: var(--space-3);
  margin-left: var(--space-4);
}

.detail-date {
  font-size: var(--text-xs);
  color: var(--color-text-tertiary);
}

.close-btn-absolute {
  background: transparent;
  border: none;
  font-size: 24px;
  color: var(--color-text-tertiary);
  cursor: pointer;
  line-height: 1;
  padding: 0;
  margin-left: var(--space-4);
  /* Reset */
}

.close-btn-absolute:hover {
  color: var(--color-text-primary);
}

.detail-body {
  padding: var(--space-6);
  max-height: 60vh;
  overflow-y: auto;
}

.detail-section {
  margin-bottom: var(--space-6);
}

.detail-section:last-child {
  margin-bottom: 0;
}

.section-title {
  font-size: var(--text-sm);
  font-weight: 600;
  color: var(--color-text-primary);
  margin: 0 0 var(--space-2) 0;
}

.description-text {
  font-size: var(--text-base);
  color: var(--color-text-secondary);
  line-height: 1.6;
}

.code-preview-box {
  background: var(--color-bg-secondary);
  border-radius: var(--radius-lg);
  padding: var(--space-4);
  /* border: 1px solid var(--color-border); */
  /* Optional: subtle border */
}

.code-content {
  margin: 0;
  white-space: pre-wrap;
  font-family: var(--font-mono);
  font-size: var(--text-sm);
  color: var(--color-text-primary);
  line-height: 1.6;
}

.tags-row {
  display: flex;
  flex-wrap: wrap;
  gap: var(--space-2);
}

.detail-footer {
  padding: var(--space-4) var(--space-6);
  border-top: 1px solid var(--color-border);
  display: flex;
  justify-content: flex-end;
  gap: var(--space-3);
  background: var(--color-bg-secondary);
  /* Slight contrast for footer */
  border-bottom-left-radius: var(--radius-xl);
  border-bottom-right-radius: var(--radius-xl);
}

.modal-btn {
  border-radius: var(--radius-md);
}
</style>
