<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getPrompt, getVersionHistory, commitVersion, rollbackVersion, getVersionDiff, type Prompt, type PromptVersion, type DiffResult } from '../api/prompt'
import { ElMessage, ElMessageBox } from 'element-plus'

const route = useRoute()
const router = useRouter()
const promptId = computed(() => Number(route.params.id))

const loading = ref(false)
const prompt = ref<Prompt | null>(null)
const versions = ref<PromptVersion[]>([])
const expandedVersions = ref<Set<number>>(new Set())

// 新版本表单
const newContent = ref('')
const commitMessage = ref('')
const committing = ref(false)

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
    ElMessage.error(error.response?.data?.message || '加载 Prompt 失败')
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
      if (res.data.length > 0) {
        newContent.value = res.data[0].content
      }
    } else {
      ElMessage.error(res.message || '加载版本历史失败')
    }
  } catch (error: any) {
    ElMessage.error(error.response?.data?.message || '加载版本历史失败')
  } finally {
    loading.value = false
  }
}

// 提交新版本
const handleCommit = async () => {
  if (!newContent.value.trim()) {
    ElMessage.warning('请输入内容')
    return
  }
  if (!commitMessage.value.trim()) {
    ElMessage.warning('请输入提交说明')
    return
  }

  committing.value = true
  try {
    const latestVersionId = versions.value.length > 0 ? versions.value[0].id : 1
    const res = await commitVersion(promptId.value, {
      content: newContent.value,
      parentVersionId: latestVersionId,
      commitMessage: commitMessage.value
    })
    if (res.code === 200) {
      ElMessage.success('提交成功')
      commitMessage.value = ''
      loadVersions()
    } else {
      ElMessage.error(res.message || '提交失败')
    }
  } catch (error: any) {
    ElMessage.error(error.response?.data?.message || '提交失败')
  } finally {
    committing.value = false
  }
}

// 回滚版本
const handleRollback = async (version: PromptVersion) => {
  try {
    await ElMessageBox.confirm(
      `确定要回滚到 v${version.versionNumber} 吗？这将创建一个新版本。`,
      '确认回滚',
      { type: 'warning' }
    )

    const res = await rollbackVersion(promptId.value, version.id)
    if (res.code === 200) {
      ElMessage.success('回滚成功')
      loadVersions()
    } else {
      ElMessage.error(res.message || '回滚失败')
    }
  } catch (error: any) {
    if (error !== 'cancel') {
      ElMessage.error(error.response?.data?.message || '回滚失败')
    }
  }
}

// 切换展开
const toggleExpand = (versionId: number) => {
  if (expandedVersions.value.has(versionId)) {
    expandedVersions.value.delete(versionId)
  } else {
    expandedVersions.value.add(versionId)
  }
}

// 复制内容
const copyContent = async (content: string) => {
  try {
    await navigator.clipboard.writeText(content)
    ElMessage.success('已复制')
  } catch {
    ElMessage.error('复制失败')
  }
}

// 加载最新版本到编辑器
const loadLatest = () => {
  if (versions.value.length > 0) {
    newContent.value = versions.value[0].content
    ElMessage.success('已加载最新版本')
  }
}

// ============ Diff 功能 ============

// 进入对比模式
const enterCompareMode = () => {
  compareMode.value = true
  selectedVersions.value = []
  ElMessage.info('请选择两个版本进行对比')
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
    ElMessage.warning('请选择两个版本')
    return
  }

  diffLoading.value = true
  showDiffModal.value = true
  
  try {
    // 确保较小的版本ID在前（较老的版本）
    const sortedIds = [...selectedVersions.value].sort((a, b) => a - b)
    const res = await getVersionDiff(sortedIds[0], sortedIds[1])
    if (res.code === 200) {
      diffResult.value = res.data
    } else {
      ElMessage.error(res.message || '获取差异失败')
      showDiffModal.value = false
    }
  } catch (error: any) {
    ElMessage.error(error.response?.data?.message || '获取差异失败')
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
    ElMessage.warning('这是最早的版本，无法对比')
    return
  }
  
  const previousVersion = versions.value[index + 1]
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
    <header class="header">
      <div class="header-left">
        <button class="back-btn" @click="router.push('/prompts')">← 返回</button>
        <span class="logo-icon">⬡</span>
        <span class="logo-text">Prompt-Forge</span>
      </div>
    </header>

    <!-- Main Content -->
    <main class="main-content">
      <!-- Prompt Info -->
      <div class="prompt-info">
        <h1>{{ prompt?.name || '加载中...' }}</h1>
        <p>{{ prompt?.description || '' }}</p>
      </div>

      <!-- Commit Section -->
      <div class="commit-section">
        <h3>📝 提交新版本</h3>
        <form @submit.prevent="handleCommit">
          <div class="form-group">
            <textarea 
              v-model="newContent" 
              rows="8" 
              placeholder="输入新的 Prompt 内容..."
            ></textarea>
          </div>
          <div class="form-row">
            <input 
              v-model="commitMessage" 
              type="text" 
              placeholder="提交说明（如：优化变量结构）" 
            />
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
          <h3>📜 版本历史</h3>
          <div class="compare-controls">
            <button 
              v-if="!compareMode && versions.length > 1" 
              class="compare-btn" 
              @click="enterCompareMode"
            >
              🔀 版本对比
            </button>
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
          <div 
            v-for="(version, index) in versions" 
            :key="version.id" 
            class="version-item"
            :class="{ 
              current: index === 0,
              'compare-mode': compareMode,
              'selected': isVersionSelected(version.id)
            }"
            @click="compareMode && selectVersionForCompare(version.id)"
          >
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
              
              <p class="commit-message">{{ version.commitMessage || '无提交说明' }}</p>
              
              <div v-if="expandedVersions.has(version.id)" class="version-code">
                <pre>{{ version.content }}</pre>
              </div>
              
              <div class="version-actions" v-if="!compareMode">
                <button class="action-btn" @click.stop="toggleExpand(version.id)">
                  {{ expandedVersions.has(version.id) ? '收起' : '查看内容' }}
                </button>
                <button class="action-btn" @click.stop="copyContent(version.content)">复制</button>
                <button 
                  v-if="index < versions.length - 1" 
                  class="action-btn diff"
                  @click.stop="compareWithPrevious(version, index)"
                >
                  对比上一版
                </button>
                <button 
                  v-if="index !== 0" 
                  class="action-btn primary" 
                  @click.stop="handleRollback(version)"
                >
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
          <h3>🔍 版本对比</h3>
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
            <div 
              v-for="(line, idx) in diffResult.lines" 
              :key="idx" 
              class="diff-line"
              :class="line.type.toLowerCase()"
            >
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
  </div>
</template>

<style scoped>
.page-container {
  min-height: 100vh;
  background: var(--color-bg-primary);
  color: var(--color-text-primary);
}

/* 深色主题渐变 */
[data-theme="dark"] .page-container,
:root:not([data-theme="light"]) .page-container {
  background: linear-gradient(135deg, var(--color-bg-primary) 0%, var(--color-bg-tertiary) 100%);
}

.header {
  display: flex;
  align-items: center;
  padding: 16px 32px;
  border-bottom: 1px solid var(--color-border);
  background: var(--color-bg-secondary);
}

.header-left {
  display: flex;
  align-items: center;
  gap: 16px;
}

.back-btn {
  padding: 8px 16px;
  background: transparent;
  border: 1px solid var(--color-border);
  border-radius: 6px;
  color: var(--color-text-tertiary);
  cursor: pointer;
}

.back-btn:hover {
  border-color: var(--color-primary);
  color: var(--color-primary);
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

.main-content {
  max-width: 900px;
  margin: 0 auto;
  padding: 32px;
}

.prompt-info {
  margin-bottom: 32px;
  padding: 20px;
  background: var(--color-bg-elevated);
  border: 1px solid var(--color-border);
  border-radius: 12px;
}

.prompt-info h1 {
  font-size: 24px;
  margin-bottom: 8px;
  color: var(--color-text-primary);
}

.prompt-info p {
  color: var(--color-text-tertiary);
}

.commit-section {
  margin-bottom: 32px;
  padding: 24px;
  background: var(--color-primary-light);
  border: 1px solid rgba(94, 106, 210, 0.3);
  border-radius: 12px;
}

.commit-section h3 {
  margin-bottom: 16px;
  color: var(--color-text-primary);
}

.form-group textarea {
  width: 100%;
  padding: 14px;
  background: var(--color-bg-secondary);
  border: 1px solid var(--color-border);
  border-radius: 8px;
  color: var(--color-text-primary);
  font-family: monospace;
  font-size: 14px;
  resize: vertical;
  outline: none;
}

.form-group textarea:focus {
  border-color: var(--color-primary);
}

.form-row {
  display: flex;
  gap: 12px;
  margin-top: 12px;
}

.form-row input {
  flex: 1;
  padding: 12px 14px;
  background: var(--color-bg-secondary);
  border: 1px solid var(--color-border);
  border-radius: 8px;
  color: var(--color-text-primary);
  outline: none;
}

.form-row input:focus {
  border-color: var(--color-primary);
}

.form-row {
  display: flex;
  gap: 12px;
  margin-top: 12px;
}

.form-row input {
  flex: 1;
  padding: 12px 14px;
  background: rgba(0, 0, 0, 0.3);
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 8px;
  color: #fff;
  outline: none;
}

.form-row input:focus {
  border-color: #5e6ad2;
}

.form-actions {
  display: flex;
  gap: 8px;
}

.secondary-btn {
  padding: 12px 16px;
  background: transparent;
  border: 1px solid rgba(255, 255, 255, 0.2);
  border-radius: 8px;
  color: #888;
  cursor: pointer;
}

.submit-btn {
  padding: 12px 20px;
  background: #5e6ad2;
  border: none;
  border-radius: 8px;
  color: #fff;
  cursor: pointer;
}

.submit-btn:disabled {
  opacity: 0.6;
}

.version-section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.version-section-header h3 {
  margin: 0;
}

.compare-controls {
  display: flex;
  align-items: center;
  gap: 12px;
}

.compare-btn {
  padding: 8px 16px;
  background: rgba(245, 158, 11, 0.2);
  border: 1px solid rgba(245, 158, 11, 0.5);
  border-radius: 6px;
  color: #f59e0b;
  cursor: pointer;
  font-size: 13px;
  transition: all 0.2s;
}

.compare-btn:hover {
  background: rgba(245, 158, 11, 0.3);
}

.compare-hint {
  font-size: 13px;
  color: #f59e0b;
}

.cancel-btn {
  padding: 6px 12px;
  background: transparent;
  border: 1px solid rgba(255, 255, 255, 0.2);
  border-radius: 6px;
  color: #888;
  cursor: pointer;
  font-size: 12px;
}

.loading, .empty {
  text-align: center;
  padding: 40px;
  color: #888;
}

.timeline {
  position: relative;
  padding-left: 32px;
}

.timeline::before {
  content: '';
  position: absolute;
  left: 7px;
  top: 0;
  bottom: 0;
  width: 2px;
  background: rgba(255, 255, 255, 0.1);
}

.version-item {
  position: relative;
  margin-bottom: 24px;
}

.version-item.compare-mode {
  cursor: pointer;
}

.version-item.compare-mode:hover .version-content {
  border-color: rgba(245, 158, 11, 0.5);
}

.version-item.selected .version-content {
  border-color: #f59e0b;
  background: rgba(245, 158, 11, 0.1);
}

.version-marker {
  position: absolute;
  left: -32px;
  top: 4px;
  width: 16px;
  height: 16px;
  background: #1a1a2e;
  border: 3px solid #5e6ad2;
  border-radius: 50%;
}

.version-item.current .version-marker {
  background: #5e6ad2;
  box-shadow: 0 0 0 4px rgba(94, 106, 210, 0.3);
}

.version-item.selected .version-marker {
  border-color: #f59e0b;
  background: #f59e0b;
}

.version-content {
  padding: 16px;
  background: rgba(255, 255, 255, 0.05);
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 10px;
  transition: all 0.2s;
}

.version-item.current .version-content {
  border-color: rgba(94, 106, 210, 0.3);
}

.version-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.version-badges {
  display: flex;
  gap: 8px;
}

.version-number {
  padding: 4px 10px;
  background: #5e6ad2;
  border-radius: 4px;
  font-size: 12px;
  font-weight: 600;
}

.current-badge {
  padding: 4px 8px;
  background: rgba(16, 185, 129, 0.2);
  color: #10b981;
  border-radius: 4px;
  font-size: 11px;
  font-weight: 600;
}

.selected-badge {
  padding: 4px 8px;
  background: rgba(245, 158, 11, 0.3);
  color: #f59e0b;
  border-radius: 4px;
  font-size: 11px;
  font-weight: 600;
}

.version-date {
  font-size: 12px;
  color: #666;
}

.commit-message {
  margin-bottom: 12px;
  color: #ccc;
  font-size: 14px;
}

.version-code {
  margin-bottom: 12px;
  padding: 12px;
  background: rgba(0, 0, 0, 0.3);
  border-radius: 8px;
  max-height: 200px;
  overflow-y: auto;
}

.version-code pre {
  margin: 0;
  font-family: monospace;
  font-size: 13px;
  white-space: pre-wrap;
  color: #aaa;
}

.version-actions {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.action-btn {
  padding: 8px 14px;
  background: rgba(255, 255, 255, 0.1);
  border: none;
  border-radius: 6px;
  color: #888;
  font-size: 13px;
  cursor: pointer;
  transition: all 0.2s;
}

.action-btn:hover {
  background: rgba(255, 255, 255, 0.2);
  color: #fff;
}

.action-btn.primary {
  background: #5e6ad2;
  color: #fff;
}

.action-btn.primary:hover {
  background: #4c5bb5;
}

.action-btn.diff {
  background: rgba(245, 158, 11, 0.2);
  color: #f59e0b;
}

.action-btn.diff:hover {
  background: rgba(245, 158, 11, 0.3);
}

/* Diff Modal */
.diff-modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.8);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
}

.diff-modal {
  width: 90%;
  max-width: 900px;
  max-height: 80vh;
  background: #1a1a2e;
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 16px;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.diff-modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20px 24px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.1);
}

.diff-modal-header h3 {
  margin: 0;
  font-size: 18px;
}

.close-btn {
  width: 32px;
  height: 32px;
  background: rgba(255, 255, 255, 0.1);
  border: none;
  border-radius: 6px;
  color: #888;
  cursor: pointer;
  font-size: 16px;
}

.close-btn:hover {
  background: rgba(255, 255, 255, 0.2);
  color: #fff;
}

.diff-loading {
  padding: 60px;
  text-align: center;
  color: #888;
}

.diff-stats {
  display: flex;
  gap: 16px;
  padding: 16px 24px;
  background: rgba(0, 0, 0, 0.3);
  border-bottom: 1px solid rgba(255, 255, 255, 0.1);
}

.stat-item {
  font-size: 14px;
  color: #ccc;
}

.stat-added {
  color: #10b981;
  font-size: 14px;
}

.stat-deleted {
  color: #ef4444;
  font-size: 14px;
}

.diff-content {
  flex: 1;
  overflow-y: auto;
  padding: 0;
  font-family: 'Consolas', 'Monaco', monospace;
  font-size: 13px;
}

.diff-line {
  display: flex;
  min-height: 24px;
  line-height: 24px;
}

.diff-line.equal {
  background: transparent;
}

.diff-line.insert {
  background: rgba(16, 185, 129, 0.15);
}

.diff-line.delete {
  background: rgba(239, 68, 68, 0.15);
}

.line-number {
  min-width: 40px;
  padding: 0 8px;
  text-align: right;
  color: #666;
  background: rgba(0, 0, 0, 0.2);
  user-select: none;
}

.line-type {
  width: 24px;
  text-align: center;
  color: #888;
}

.diff-line.insert .line-type {
  color: #10b981;
}

.diff-line.delete .line-type {
  color: #ef4444;
}

.line-content {
  flex: 1;
  padding: 0 12px;
  white-space: pre-wrap;
  word-break: break-all;
}

.diff-line.insert .line-content {
  color: #6ee7b7;
}

.diff-line.delete .line-content {
  color: #fca5a5;
}
</style>
