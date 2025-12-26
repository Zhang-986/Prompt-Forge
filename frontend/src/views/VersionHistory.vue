<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getPrompt, getVersionHistory, commitVersion, rollbackVersion, type Prompt, type PromptVersion } from '../api/prompt'
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
        <h3>📜 版本历史</h3>
        
        <div v-if="loading" class="loading">加载中...</div>
        
        <div v-else-if="versions.length === 0" class="empty">暂无版本历史</div>
        
        <div v-else class="timeline">
          <div 
            v-for="(version, index) in versions" 
            :key="version.id" 
            class="version-item"
            :class="{ current: index === 0 }"
          >
            <div class="version-marker"></div>
            
            <div class="version-content">
              <div class="version-header">
                <div class="version-badges">
                  <span class="version-number">v{{ version.versionNumber }}</span>
                  <span v-if="index === 0" class="current-badge">当前版本</span>
                </div>
                <span class="version-date">{{ new Date(version.createdAt).toLocaleString() }}</span>
              </div>
              
              <p class="commit-message">{{ version.commitMessage || '无提交说明' }}</p>
              
              <div v-if="expandedVersions.has(version.id)" class="version-code">
                <pre>{{ version.content }}</pre>
              </div>
              
              <div class="version-actions">
                <button class="action-btn" @click="toggleExpand(version.id)">
                  {{ expandedVersions.has(version.id) ? '收起' : '查看内容' }}
                </button>
                <button class="action-btn" @click="copyContent(version.content)">复制</button>
                <button 
                  v-if="index !== 0" 
                  class="action-btn primary" 
                  @click="handleRollback(version)"
                >
                  回滚到此版本
                </button>
              </div>
            </div>
          </div>
        </div>
      </div>
    </main>
  </div>
</template>

<style scoped>
.page-container {
  min-height: 100vh;
  background: linear-gradient(135deg, #0a0a0f 0%, #1a1a2e 100%);
  color: #fff;
}

.header {
  display: flex;
  align-items: center;
  padding: 16px 32px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.1);
}

.header-left {
  display: flex;
  align-items: center;
  gap: 16px;
}

.back-btn {
  padding: 8px 16px;
  background: transparent;
  border: 1px solid rgba(255, 255, 255, 0.2);
  border-radius: 6px;
  color: #888;
  cursor: pointer;
}

.back-btn:hover {
  border-color: #5e6ad2;
  color: #5e6ad2;
}

.logo-icon {
  font-size: 24px;
  color: #5e6ad2;
}

.logo-text {
  font-size: 18px;
  font-weight: 600;
}

.main-content {
  max-width: 900px;
  margin: 0 auto;
  padding: 32px;
}

.prompt-info {
  margin-bottom: 32px;
  padding: 20px;
  background: rgba(255, 255, 255, 0.05);
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 12px;
}

.prompt-info h1 {
  font-size: 24px;
  margin-bottom: 8px;
}

.prompt-info p {
  color: #888;
}

.commit-section {
  margin-bottom: 32px;
  padding: 24px;
  background: rgba(94, 106, 210, 0.1);
  border: 1px solid rgba(94, 106, 210, 0.3);
  border-radius: 12px;
}

.commit-section h3 {
  margin-bottom: 16px;
}

.form-group textarea {
  width: 100%;
  padding: 14px;
  background: rgba(0, 0, 0, 0.3);
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 8px;
  color: #fff;
  font-family: monospace;
  font-size: 14px;
  resize: vertical;
  outline: none;
}

.form-group textarea:focus {
  border-color: #5e6ad2;
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

.version-section h3 {
  margin-bottom: 20px;
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

.version-content {
  padding: 16px;
  background: rgba(255, 255, 255, 0.05);
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 10px;
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
</style>
