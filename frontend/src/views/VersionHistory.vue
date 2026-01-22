<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { useRoute } from 'vue-router'
import { getPrompt, getVersionHistory, commitVersion, rollbackVersion, getVersionDiff, type Prompt, type PromptVersion, type DiffResult } from '../api/prompt'
import { message, Modal } from 'ant-design-vue'
import { EditOutlined, RobotOutlined, HistoryOutlined, SwapOutlined, SearchOutlined } from '@ant-design/icons-vue'
import { optimizePrompt } from '../api/optimize'
import { getAvailableModels, type AvailableModelInfo } from '../api/arena'


const route = useRoute()
const promptId = computed(() => Number(route.params.id))

const loading = ref(false)
const prompt = ref<Prompt | null>(null)
const versions = ref<PromptVersion[]>([])
const expandedVersions = ref<Set<number>>(new Set())

// 新版本表单
const newContent = ref('')
const commitMessage = ref('')
const committing = ref(false)
const optimizing = ref(false)

// AI 优化模型选择
const showModelSelectModal = ref(false)
const availableModels = ref<AvailableModelInfo[]>([])
const selectedOptimizeModel = ref<string>('')
const loadingModels = ref(false)

const openOptimizeModal = async () => {
  if (!newContent.value.trim()) {
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
        selectedOptimizeModel.value = firstModel.modelId // 默认选第一个
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
    const res = await optimizePrompt(newContent.value, selectedOptimizeModel.value)
    if (res.code === 200) {
      newContent.value = res.data
      message.success('优化完成，请检查内容')
    } else {
      message.error(res.message || '优化失败')
    }
  } catch (error) {
    message.error('AI 服务暂时不可用，请确保已配置模型')
  } finally {
    optimizing.value = false
  }
}


// Diff 相关状态
const showDiffModal = ref(false)
const diffLoading = ref(false)
const diffResult = ref<DiffResult | null>(null)
const selectedVersions = ref<number[]>([])
const compareMode = ref(false)

// 加载 Prompt 信息
const loadPrompt = async () => {
  try {
    const res = await getPrompt(promptId.value)
    if (res.code === 200) {
      prompt.value = res.data
    }
  } catch (error: any) {
    message.error(error.response?.data?.message || '加载 Prompt 失败')
  }
}

// 加载版本历史
const loadVersions = async () => {
  loading.value = true
  try {
    const res = await getVersionHistory(promptId.value)
    if (res.code === 200) {
      versions.value = res.data
      // 如果有版本，设置最新内容
      const firstVersion = res.data[0]
      if (firstVersion) {
        newContent.value = firstVersion.content
      }
    } else {
      message.error(res.message || '加载版本历史失败')
    }
  } catch (error: any) {
    message.error(error.response?.data?.message || '加载版本历史失败')
  } finally {
    loading.value = false
  }
}

// 提交新版本
const handleCommit = async () => {
  if (!newContent.value.trim()) {
    message.warning('请输入内容')
    return
  }
  if (!commitMessage.value.trim()) {
    message.warning('请输入提交说明')
    return
  }

  committing.value = true
  try {
    const firstVersion = versions.value[0]
    const latestVersionId = firstVersion ? firstVersion.id : 1
    const res = await commitVersion(promptId.value, {
      content: newContent.value,
      parentVersionId: latestVersionId,
      commitMessage: commitMessage.value
    })
    if (res.code === 200) {
      message.success('提交成功')
      commitMessage.value = ''
      loadVersions()
    } else {
      message.error(res.message || '提交失败')
    }
  } catch (error: any) {
    message.error(error.response?.data?.message || '提交失败')
  } finally {
    committing.value = false
  }
}

// 回滚版本
const handleRollback = (version: PromptVersion) => {
  Modal.confirm({
    title: '确认回滚',
    content: `确定要回滚到 v${version.versionNumber} 吗？这将创建一个新版本。`,
    okType: 'danger',
    onOk: async () => {
      const res = await rollbackVersion(promptId.value, version.id)
      if (res.code === 200) {
        message.success('回滚成功')
        loadVersions()
      } else {
        message.error(res.message || '回滚失败')
      }
    }
  })
}

// 切换展开
const toggleExpand = (versionId: number) => {
  const newSet = new Set(expandedVersions.value)
  if (newSet.has(versionId)) {
    newSet.delete(versionId)
  } else {
    newSet.add(versionId)
  }
  expandedVersions.value = newSet
}

// 复制内容

// 加载最新版本到编辑器
const loadLatest = () => {
  const latestVersion = versions.value[0]
  if (latestVersion) {
    newContent.value = latestVersion.content
    message.success('已加载最新版本')
  }
}

// ============ Diff 功能 ============

// 进入对比模式
const enterCompareMode = () => {
  compareMode.value = true
  selectedVersions.value = []
  message.info('请选择两个版本进行对比')
}

// 退出对比模式
const exitCompareMode = () => {
  compareMode.value = false
  selectedVersions.value = []
}

// 选择版本进行对比
const selectVersionForCompare = (versionId: number) => {
  if (!compareMode.value) return

  const index = selectedVersions.value.indexOf(versionId)
  if (index > -1) {
    // 已选中，取消选择
    selectedVersions.value.splice(index, 1)
  } else if (selectedVersions.value.length < 2) {
    // 未选中且少于2个，添加选择
    selectedVersions.value.push(versionId)
  } else {
    // 已有2个，替换第一个
    selectedVersions.value.shift()
    selectedVersions.value.push(versionId)
  }

  // 如果选中了两个版本，自动开始对比
  if (selectedVersions.value.length === 2) {
    performDiff()
  }
}

// 检查版本是否被选中
const isVersionSelected = (versionId: number) => {
  return selectedVersions.value.includes(versionId)
}

// 执行 Diff
const performDiff = async () => {
  if (selectedVersions.value.length !== 2) {
    message.warning('请选择两个版本')
    return
  }

  diffLoading.value = true
  showDiffModal.value = true

  try {
    // 确保较小的版本ID在前（较老的版本）
    const sortedIds = [...selectedVersions.value].sort((a, b) => a - b)
    const versionId1 = sortedIds[0]
    const versionId2 = sortedIds[1]
    if (versionId1 === undefined || versionId2 === undefined) {
      message.warning('版本选择错误')
      return
    }
    const res = await getVersionDiff(versionId1, versionId2)
    if (res.code === 200) {
      diffResult.value = res.data
    } else {
      message.error(res.message || '获取差异失败')
      showDiffModal.value = false
    }
  } catch (error: any) {
    message.error(error.response?.data?.message || '获取差异失败')
    showDiffModal.value = false
  } finally {
    diffLoading.value = false
  }
}

// 关闭 Diff 弹窗
const closeDiffModal = () => {
  showDiffModal.value = false
  diffResult.value = null
  exitCompareMode()
}

// 快速对比：与上一个版本对比
const compareWithPrevious = async (version: PromptVersion, index: number) => {
  if (index >= versions.value.length - 1) {
    message.warning('这是最早的版本，无法对比')
    return
  }

  const previousVersion = versions.value[index + 1]
  if (!previousVersion) {
    message.warning('找不到上一个版本')
    return
  }
  selectedVersions.value = [previousVersion.id, version.id]
  await performDiff()
}

onMounted(() => {
  loadPrompt()
  loadVersions()
})
</script>

<template>
  <div class="page-container">
    <!-- Header -->
    <!-- Header Removed -->

    <!-- Main Content -->

    <!-- Main Content -->
    <main class="main-content">
      <!-- Prompt Info -->
      <div class="prompt-info">
        <h1>{{ prompt?.name || '加载中...' }}</h1>
        <p>{{ prompt?.description || '' }}</p>
      </div>

      <!-- Commit Section -->
      <div class="commit-section">
        <h3>
          <EditOutlined /> 提交新版本
        </h3>
        <form @submit.prevent="handleCommit">
          <div class="form-group">
            <div class="editor-header">
              <label>内容</label>
              <a-button size="small" @click="openOptimizeModal" :loading="optimizing || loadingModels"
                :disabled="!newContent.trim()">
                <template #icon>
                  <RobotOutlined />
                </template>
                AI 优化
              </a-button>
            </div>
            <textarea v-model="newContent" rows="8" placeholder="输入或者粘贴你的 Prompt，点击 'AI 优化' 让 AI 帮你重写..."></textarea>
          </div>
          <div class="form-row">
            <input v-model="commitMessage" type="text" placeholder="提交说明（如：优化变量结构）" />
            <div class="form-actions">
              <button type="button" class="secondary-btn" @click="loadLatest">加载最新</button>
              <button type="submit" class="submit-btn" :disabled="committing">
                {{ committing ? '提交中...' : '提交版本' }}
              </button>
            </div>
          </div>
        </form>
      </div>

      <!-- Version Timeline -->
      <div class="version-section">
        <div class="version-section-header">
          <h3>
            <HistoryOutlined /> 版本历史
          </h3>
          <div class="compare-controls">
            <a-button v-if="!compareMode && versions.length > 1" @click="enterCompareMode">
              <template #icon>
                <SwapOutlined />
              </template>
              版本对比
            </a-button>
            <template v-if="compareMode">
              <span class="compare-hint">
                已选择 {{ selectedVersions.length }}/2 个版本
              </span>
              <button class="cancel-btn" @click="exitCompareMode">取消对比</button>
            </template>
          </div>
        </div>

        <div v-if="loading" class="loading">加载中...</div>

        <div v-else-if="versions.length === 0" class="empty">暂无版本历史</div>

        <div v-else class="timeline">
          <div v-for="(version, index) in versions" :key="version.id" class="version-item" :class="{
            current: index === 0,
            'compare-mode': compareMode,
            'selected': isVersionSelected(version.id)
          }" @click="compareMode && selectVersionForCompare(version.id)">
            <div class="version-marker"></div>

            <div class="version-content">
              <div class="version-header">
                <div class="version-badges">
                  <span class="version-number">v{{ version.versionNumber }}</span>
                  <span v-if="index === 0" class="current-badge">当前版本</span>
                  <span v-if="isVersionSelected(version.id)" class="selected-badge">
                    {{ selectedVersions.indexOf(version.id) + 1 }}
                  </span>
                </div>
                <span class="version-date">{{ new Date(version.createdAt).toLocaleString() }}</span>
              </div>

              <!-- 内容预览（默认显示前3行） -->
              <div class="version-preview" @click.stop="toggleExpand(version.id)">
                <pre v-if="!expandedVersions.has(version.id)">{{ (version.content || '').split('\n').slice(0, 3).join('\n') }}{{
                  (version.content || '').split('\n').length > 3 ? '\n...' : '' }}</pre>
                <pre v-else>{{ version.content || '' }}</pre>
              </div>

              <!-- 版本说明（小字） -->
              <!-- 版本说明（小字） -->
              <div class="commit-message-container">
                <span class="commit-label">版本说明：</span>
                <span class="commit-limit">{{ version.commitMessage || '无提交说明' }}</span>
              </div>

              <div class="version-actions" v-if="!compareMode">
                <button v-if="index < versions.length - 1" class="action-btn diff"
                  @click.stop="compareWithPrevious(version, index)">
                  对比上一版
                </button>
                <button v-if="index !== 0" class="action-btn primary" @click.stop="handleRollback(version)">
                  回滚到此版本
                </button>
              </div>
            </div>
          </div>
        </div>
      </div>
    </main>

    <!-- Diff Modal -->
    <div v-if="showDiffModal" class="diff-modal-overlay" @click.self="closeDiffModal">
      <div class="diff-modal">
        <div class="diff-modal-header">
          <h3>
            <SearchOutlined /> 版本对比
          </h3>
          <button class="close-btn" @click="closeDiffModal">✕</button>
        </div>

        <div v-if="diffLoading" class="diff-loading">
          加载中...
        </div>

        <template v-else-if="diffResult">
          <div class="diff-stats">
            <span class="stat-item">
              v{{ diffResult.sourceVersionNumber }} → v{{ diffResult.targetVersionNumber }}
            </span>
            <span class="stat-added">+{{ diffResult.addedLines }} 新增</span>
            <span class="stat-deleted">-{{ diffResult.deletedLines }} 删除</span>
          </div>

          <div class="diff-content">
            <div v-for="(line, idx) in diffResult.lines" :key="idx" class="diff-line" :class="line.type.toLowerCase()">
              <span class="line-number source">{{ line.sourceLineNumber ?? '' }}</span>
              <span class="line-number target">{{ line.targetLineNumber ?? '' }}</span>
              <span class="line-type">
                {{ line.type === 'INSERT' ? '+' : line.type === 'DELETE' ? '-' : ' ' }}
              </span>
              <span class="line-content">{{ line.content }}</span>
            </div>
          </div>
        </template>
      </div>
    </div>

    <!-- Model Selection Modal for AI Optimize -->
    <a-modal v-model:open="showModelSelectModal" title="选择优化模型" :footer="null" width="400px">
      <div class="model-select-content">
        <p class="model-hint">请选择用于优化 Prompt 的 AI 模型：</p>
        <a-radio-group v-model:value="selectedOptimizeModel" class="model-radio-group">
          <a-radio v-for="model in availableModels" :key="model.modelId" :value="model.modelId"
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
  </div>
</template>

<style scoped>
/* Page container styling */
.page-container {
  min-height: 100vh;
  background: var(--color-bg-primary);
  color: var(--color-text-primary);
}

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
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  transition: all var(--transition-fast);
}

.model-radio-item:hover {
  border-color: var(--color-primary);
  background: var(--color-bg-secondary);
}

.modal-actions {
  margin-top: var(--space-6);
  display: flex;
  justify-content: flex-end;
  gap: var(--space-3);
}

.main-content {
  max-width: 900px;
  margin: 0 auto;
  padding: var(--space-8);
}

/* Prompt Info - Minimalist Header */
.prompt-info {
  margin-bottom: var(--space-8);
  /* padding: var(--space-5); */
  /* background: var(--color-bg-secondary); */
  /* border: 1px solid var(--color-border); */
  /* border-radius: var(--radius-lg); */
}

.prompt-info h1 {
  font-size: var(--text-2xl);
  margin-bottom: var(--space-2);
  color: var(--color-text-primary);
  font-weight: 600;
}

.prompt-info p {
  color: var(--color-text-secondary);
  font-size: var(--text-base);
}

/* Commit Section - Clean Editor Style */
.commit-section {
  margin-bottom: var(--space-10);
  padding: var(--space-6);
  background: #fff;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.02);
}

.commit-section h3 {
  margin-bottom: var(--space-5);
  color: var(--color-text-primary);
  font-size: 16px;
  font-weight: 600;
  display: flex;
  align-items: center;
  gap: 8px;
}

.form-group textarea {
  width: 100%;
  padding: var(--space-4);
  background: var(--color-bg-secondary);
  /* Slight gray bg for input area */
  border: 1px solid transparent;
  border-radius: var(--radius-md);
  color: var(--color-text-primary);
  font-family: var(--font-mono);
  font-size: var(--text-sm);
  resize: vertical;
  outline: none;
  min-height: 200px;
  line-height: 1.6;
  transition: all 0.2s;
}

.form-group textarea:focus {
  background: #fff;
  border-color: var(--color-primary);
  box-shadow: 0 0 0 2px rgba(0, 0, 0, 0.05);
}

.editor-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: var(--space-2);
}

.editor-header label {
  font-weight: 500;
  font-size: var(--text-sm);
}

.form-row {
  display: flex;
  gap: var(--space-3);
  margin-top: var(--space-4);
  align-items: flex-start;
}

.form-row input {
  flex: 1;
  padding: var(--space-3);
  background: #fff;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  color: var(--color-text-primary);
  outline: none;
  transition: border 0.2s;
}

.form-row input:focus {
  border-color: var(--color-primary);
}

.compare-hint {
  font-size: var(--text-sm);
  color: var(--color-warning);
}

.cancel-btn {
  padding: var(--space-2) var(--space-3);
  background: white;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  color: var(--color-text-primary);
  cursor: pointer;
  font-size: var(--text-xs);
  transition: all 0.2s;
}

.cancel-btn:hover {
  background: var(--color-bg-secondary);
  border-color: var(--color-primary);
}

.form-actions {
  display: flex;
  gap: var(--space-2);
}

.secondary-btn {
  padding: var(--space-3) var(--space-4);
  background: transparent;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  color: var(--color-text-secondary);
  cursor: pointer;
  transition: all 0.2s;
}

.secondary-btn:hover {
  border-color: var(--color-text-primary);
  color: var(--color-text-primary);
}

.submit-btn {
  padding: var(--space-3) var(--space-5);
  background: #000;
  border: none;
  border-radius: var(--radius-md);
  color: #fff;
  cursor: pointer;
  font-weight: 500;
  transition: background 0.2s;
}

.submit-btn:hover {
  background: #333;
}

.submit-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

/* Timeline - Clean Minimalist Style */
/* Timeline - Refined & Elegant */
.version-section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: var(--space-8);
  padding-bottom: var(--space-4);
  border-bottom: 1px solid var(--color-border-light);
}

.timeline {
  position: relative;
  padding-left: 32px;
}

.timeline::before {
  content: '';
  position: absolute;
  left: 10px;
  top: 12px;
  bottom: 0;
  width: 1px;
  background: var(--color-border);
  opacity: 0.5;
}

.version-item {
  position: relative;
  margin-bottom: var(--space-8);
}

/* Marker - Ring Style */
.version-marker {
  position: absolute;
  left: -22px;
  top: 6px;
  /* Aligned with title */
  width: 12px;
  height: 12px;
  border-radius: 50%;
  background: #fff;
  border: 2px solid var(--color-border);
  z-index: 2;
  transition: all 0.2s;
}

.version-item.current .version-marker {
  background: var(--color-primary);
  border-color: var(--color-primary);
  box-shadow: 0 0 0 4px var(--color-primary-light);
}

/* Feature: Compare Mode Styles (Restored) */
.version-item.compare-mode .version-marker {
  width: 20px;
  height: 20px;
  left: -29px;
  /* Re-align for larger size */
  background: #fff;
  border-width: 2px;
  cursor: pointer;
}

.version-item.compare-mode:hover .version-marker {
  transform: scale(1.1);
  border-color: var(--color-primary);
}

.version-item.selected .version-marker {
  background: var(--color-primary);
  border-color: var(--color-primary);
  width: 20px;
  height: 20px;
  left: -29px;
  box-shadow: none;
}

.version-item.selected .version-marker::after {
  content: '';
  position: absolute;
  top: 45%;
  left: 50%;
  width: 5px;
  height: 9px;
  border: solid #fff;
  /* White checkmark */
  border-width: 0 2px 2px 0;
  transform: translate(-50%, -60%) rotate(45deg);
}

/* Current Version Marker - Prominent */


/* Version Content - Card Style */
.version-content {
  background: #fff;
  border-radius: var(--radius-lg);
  padding: var(--space-5);
  box-shadow: var(--shadow-sm);
  border: 1px solid transparent;
  transition: all 0.2s;
  margin-bottom: var(--space-4);
}

.version-item:hover .version-content {
  box-shadow: var(--shadow-md);
  transform: translateY(-2px);
}

.version-item.selected .version-content {
  border-color: var(--color-primary);
  box-shadow: 0 0 0 1px var(--color-primary-light);
}

.version-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: var(--space-3);
}

.version-badges {
  display: flex;
  align-items: center;
  gap: var(--space-3);
}

.version-number {
  font-weight: 600;
  font-size: 16px;
  color: var(--color-text-primary);
}

.current-badge {
  font-size: 12px;
  padding: 2px 8px;
  background: #000;
  color: #fff;
  border-radius: 99px;
  font-weight: 500;
}

.version-date {
  font-size: var(--text-sm);
  color: var(--color-text-tertiary);
  font-family: var(--font-mono);
}

.version-preview {
  margin-top: var(--space-3);
  padding: var(--space-4);
  background: var(--color-bg-secondary);
  border-radius: var(--radius-md);
  cursor: pointer;
  transition: background 0.2s;
  border: 1px solid rgba(0, 0, 0, 0.03);
}

.version-preview:hover {
  background: #f1f3f5;
  border-color: rgba(0, 0, 0, 0.05);
}

.version-preview pre {
  margin: 0;
  white-space: pre-wrap;
  font-family: var(--font-mono);
  font-size: 13px;
  color: var(--color-text-primary);
  line-height: 1.6;
}

.commit-message-container {
  margin-top: var(--space-3);
  font-size: var(--text-sm);
  display: flex;
  align-items: flex-start;
  gap: var(--space-2);
}

.commit-label {
  color: var(--color-text-tertiary);
  font-weight: 500;
  flex-shrink: 0;
}

.commit-limit {
  color: var(--color-text-primary);
}

.version-actions {
  margin-top: var(--space-3);
  display: flex;
  gap: var(--space-4);
  opacity: 0;
  /* Hide actions by default for cleaner look */
  transition: opacity 0.2s;
}

.version-item:hover .version-actions {
  opacity: 1;
  /* Show on hover */
}

.action-btn {
  font-size: var(--text-xs);
  background: transparent;
  border: none;
  cursor: pointer;
  padding: 0;
  color: var(--color-text-secondary);
  text-decoration: underline;
}

.action-btn:hover {
  color: var(--color-text-primary);
}

.action-btn.primary {
  color: var(--color-text-primary);
  font-weight: 500;
}

/* Diff Modal Styles... (Reuse existing or simplify) */
.diff-modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
  backdrop-filter: blur(4px);
}

.diff-modal {
  background: #fff;
  border-radius: var(--radius-lg);
  width: 90%;
  max-width: 1000px;
  height: 80vh;
  display: flex;
  flex-direction: column;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.15);
}

.diff-modal-header {
  padding: var(--space-4) var(--space-6);
  border-bottom: 1px solid var(--color-border);
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.diff-content {
  flex: 1;
  overflow-y: auto;
  padding: var(--space-4);
  font-family: var(--font-mono);
  font-size: 13px;
}

.diff-line {
  display: flex;
  line-height: 1.5;
}

.diff-line.insert {
  background-color: rgba(16, 185, 129, 0.1);
}

.diff-line.delete {
  background-color: rgba(239, 68, 68, 0.1);
}

.line-number {
  width: 40px;
  color: var(--color-text-tertiary);
  text-align: right;
  padding-right: var(--space-2);
  user-select: none;
}

.line-content {
  white-space: pre-wrap;
  flex: 1;
  padding-left: var(--space-2);
}

.stat-item {
  font-weight: 600;
  margin-right: var(--space-4);
}

.stat-added {
  color: var(--color-success);
  margin-right: var(--space-2);
}

.stat-deleted {
  color: var(--color-danger);
}

.close-btn {
  background: transparent;
  border: none;
  font-size: 18px;
  cursor: pointer;
}
</style>
