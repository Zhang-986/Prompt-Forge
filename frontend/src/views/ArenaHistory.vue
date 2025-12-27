<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { getArenaHistory, getArenaHistoryDetail, type ArenaHistoryItem, type ArenaHistoryDetail } from '../api/arenaHistory'
import { ElMessage } from 'element-plus'
import ThemeToggle from '../components/ThemeToggle.vue'

const router = useRouter()
const loading = ref(false)
const historyList = ref<ArenaHistoryItem[]>([])
const selectedSession = ref<ArenaHistoryDetail | null>(null)
const showDetailDialog = ref(false)

// 加载历史列表
const loadHistory = async () => {
  loading.value = true
  try {
    const res = await getArenaHistory(50)
    if (res.code === 200) {
      historyList.value = res.data
    }
  } catch {
  } finally {
    loading.value = false
  }
}

// 查看详情
const viewDetail = async (sessionId: number) => {
  try {
    const res = await getArenaHistoryDetail(sessionId)
    if (res.code === 200) {
      selectedSession.value = res.data
      showDetailDialog.value = true
    }
  } catch {
    ElMessage.error('加载详情失败')
  }
}

// 解析 models JSON
const parseModels = (modelsJson: string): string[] => {
  try {
    return JSON.parse(modelsJson)
  } catch {
    return []
  }
}

// 状态样式
const statusClass = (status: string) => {
  switch (status) {
    case 'COMPLETED': return 'status-success'
    case 'RUNNING': return 'status-running'
    case 'FAILED': return 'status-failed'
    default: return ''
  }
}

// 计算总 Token 和平均延迟
const totalStats = computed(() => {
  if (!selectedSession.value?.results) return { tokens: 0, avgLatency: 0 }
  const results = selectedSession.value.results.filter(r => r.status === 'SUCCESS')
  const tokens = results.reduce((sum, r) => sum + (r.tokensUsed || 0), 0)
  const avgLatency = results.length > 0 
    ? Math.round(results.reduce((sum, r) => sum + (r.latencyMs || 0), 0) / results.length)
    : 0
  return { tokens, avgLatency }
})

onMounted(() => {
  loadHistory()
})
</script>

<template>
  <div class="page-container">
    <!-- Header -->
    <header class="header">
      <div class="header-left">
        <button class="back-btn" @click="router.push('/arena')">← 返回竞技场</button>
        <span class="logo-icon">⬡</span>
        <span class="page-title">竞技历史</span>
      </div>
      <div class="header-right">
        <ThemeToggle />
        <button class="nav-btn" @click="router.push('/prompts')">📝 Prompts</button>
      </div>
    </header>

    <!-- Main Content -->
    <main class="main-content">
      <div v-if="loading" class="loading">加载中...</div>

      <div v-else-if="historyList.length === 0" class="empty-state">
        <p>暂无竞技历史</p>
        <button class="primary-btn" @click="router.push('/arena')">开始竞技</button>
      </div>

      <div v-else class="history-list">
        <div 
          v-for="item in historyList" 
          :key="item.id" 
          class="history-card"
          @click="viewDetail(item.id)"
        >
          <div class="card-header">
            <span class="session-id">#{{ item.id }}</span>
            <span :class="['status-badge', statusClass(item.status)]">
              {{ item.status }}
            </span>
          </div>
          <div class="model-tags">
            <span 
              v-for="model in parseModels(item.models)" 
              :key="model"
              class="model-tag"
            >
              {{ model }}
            </span>
          </div>
          <div class="card-footer">
            <span class="date">{{ new Date(item.createdAt).toLocaleString() }}</span>
          </div>
        </div>
      </div>
    </main>

    <!-- Detail Dialog -->
    <div v-if="showDetailDialog && selectedSession" class="dialog-overlay" @click.self="showDetailDialog = false">
      <div class="dialog detail-dialog">
        <div class="dialog-header">
          <h3>竞技详情 #{{ selectedSession.id }}</h3>
          <button class="close-btn" @click="showDetailDialog = false">×</button>
        </div>
        
        <div class="dialog-body">
          <!-- Stats Summary -->
          <div class="stats-summary">
            <div class="stat-item">
              <span class="stat-label">总 Token</span>
              <span class="stat-value">{{ totalStats.tokens }}</span>
            </div>
            <div class="stat-item">
              <span class="stat-label">平均延迟</span>
              <span class="stat-value">{{ totalStats.avgLatency }}ms</span>
            </div>
            <div class="stat-item">
              <span class="stat-label">模型数</span>
              <span class="stat-value">{{ selectedSession.results?.length || 0 }}</span>
            </div>
          </div>

          <!-- Prompt -->
          <div class="prompt-section">
            <label>Prompt:</label>
            <pre>{{ selectedSession.finalPrompt }}</pre>
          </div>

          <!-- Results -->
          <div class="results-section">
            <label>模型结果对比:</label>
            <div class="results-grid">
              <div 
                v-for="result in selectedSession.results" 
                :key="result.modelId"
                class="result-card"
                :class="{ 'result-error': result.status !== 'SUCCESS' }"
              >
                <div class="result-header">
                  <span class="model-name">{{ result.modelId }}</span>
                  <span :class="['result-status', result.status === 'SUCCESS' ? 'success' : 'error']">
                    {{ result.status }}
                  </span>
                </div>
                
                <!-- Stats Bar -->
                <div class="result-stats">
                  <span class="stat" title="Token 消耗">
                    🪙 {{ result.tokensUsed || 0 }}
                  </span>
                  <span class="stat" title="响应时间">
                    ⏱️ {{ result.latencyMs || 0 }}ms
                  </span>
                </div>

                <!-- Latency Bar -->
                <div class="latency-bar-container">
                  <div 
                    class="latency-bar" 
                    :style="{ width: Math.min((result.latencyMs || 0) / 50, 100) + '%' }"
                  ></div>
                </div>
                
                <div class="result-content">
                  <template v-if="result.status === 'SUCCESS'">
                    {{ result.content }}
                  </template>
                  <template v-else>
                    <span class="error-text">{{ result.errorMessage || '发生错误' }}</span>
                  </template>
                </div>
              </div>
            </div>
          </div>
        </div>

        <div class="dialog-footer">
          <button class="cancel-btn" @click="showDetailDialog = false">关闭</button>
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
  gap: 12px;
}

.back-btn, .nav-btn {
  padding: 8px 16px;
  background: transparent;
  border: 1px solid var(--color-border);
  border-radius: 6px;
  color: var(--color-text-tertiary);
  cursor: pointer;
  transition: all 0.2s;
}

.back-btn:hover, .nav-btn:hover {
  border-color: var(--color-primary);
  color: var(--color-primary);
}

.logo-icon {
  font-size: 24px;
  color: var(--color-primary);
}

.page-title {
  font-size: 20px;
  font-weight: 600;
  color: var(--color-text-primary);
}

.header-right {
  display: flex;
  align-items: center;
  gap: 16px;
}

.main-content {
  max-width: 1200px;
  margin: 0 auto;
  padding: 32px;
}

.loading, .empty-state {
  text-align: center;
  padding: 60px;
  color: var(--color-text-tertiary);
}

.primary-btn {
  margin-top: 16px;
  padding: 12px 24px;
  background: var(--color-primary-gradient);
  border: none;
  border-radius: 8px;
  color: #fff;
  cursor: pointer;
}

.history-list {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  gap: 20px;
}

.history-card {
  background: var(--color-bg-elevated);
  border: 1px solid var(--color-border);
  border-radius: 12px;
  padding: 20px;
  cursor: pointer;
  transition: all 0.2s;
}

.history-card:hover {
  border-color: var(--color-primary);
  transform: translateY(-2px);
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.session-id {
  font-weight: 600;
  color: var(--color-text-primary);
}

.status-badge {
  padding: 4px 10px;
  border-radius: 12px;
  font-size: 12px;
}

.status-success {
  background: rgba(39, 174, 96, 0.2);
  color: #27ae60;
}

.status-running {
  background: rgba(241, 196, 15, 0.2);
  color: #f1c40f;
}

.status-failed {
  background: rgba(231, 76, 60, 0.2);
  color: #e74c3c;
}

.model-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 12px;
}

.model-tag {
  padding: 4px 10px;
  background: var(--color-primary-light);
  border-radius: 6px;
  font-size: 12px;
  color: var(--color-primary);
}

.card-footer {
  color: var(--color-text-tertiary);
  font-size: 13px;
}

/* Dialog */
.dialog-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.7);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
}

.detail-dialog {
  background: var(--color-bg-elevated);
  border: 1px solid var(--color-border);
  border-radius: 16px;
  width: 90%;
  max-width: 900px;
  max-height: 90vh;
  display: flex;
  flex-direction: column;
}

.dialog-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20px;
  border-bottom: 1px solid var(--color-border);
}

.dialog-header h3 {
  margin: 0;
  color: var(--color-text-primary);
}

.close-btn {
  width: 32px;
  height: 32px;
  border: none;
  background: transparent;
  color: var(--color-text-tertiary);
  font-size: 24px;
  cursor: pointer;
}

.dialog-body {
  padding: 20px;
  overflow-y: auto;
  flex: 1;
}

.stats-summary {
  display: flex;
  gap: 24px;
  margin-bottom: 24px;
  padding: 16px;
  background: var(--color-primary-light);
  border-radius: 12px;
}

.stat-item {
  display: flex;
  flex-direction: column;
  align-items: center;
}

.stat-label {
  font-size: 12px;
  color: var(--color-text-tertiary);
  margin-bottom: 4px;
}

.stat-value {
  font-size: 24px;
  font-weight: 600;
  color: var(--color-primary);
}

.prompt-section {
  margin-bottom: 24px;
}

.prompt-section label,
.results-section label {
  display: block;
  margin-bottom: 8px;
  font-size: 14px;
  color: var(--color-text-tertiary);
}

.prompt-section pre {
  padding: 16px;
  background: var(--color-bg-secondary);
  border-radius: 8px;
  font-size: 13px;
  color: var(--color-text-secondary);
  white-space: pre-wrap;
  max-height: 120px;
  overflow-y: auto;
}

.results-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(280px, 1fr));
  gap: 16px;
}

.result-card {
  background: var(--color-bg-secondary);
  border: 1px solid var(--color-border);
  border-radius: 12px;
  padding: 16px;
}

.result-card.result-error {
  border-color: rgba(231, 76, 60, 0.5);
}

.result-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.model-name {
  font-weight: 600;
  color: var(--color-text-primary);
}

.result-status {
  padding: 2px 8px;
  border-radius: 8px;
  font-size: 11px;
}

.result-status.success {
  background: rgba(39, 174, 96, 0.2);
  color: #27ae60;
}

.result-status.error {
  background: rgba(231, 76, 60, 0.2);
  color: #e74c3c;
}

.result-stats {
  display: flex;
  gap: 16px;
  margin-bottom: 8px;
  font-size: 13px;
  color: var(--color-text-secondary);
}

.latency-bar-container {
  height: 4px;
  background: var(--color-border);
  border-radius: 2px;
  margin-bottom: 12px;
  overflow: hidden;
}

.latency-bar {
  height: 100%;
  background: var(--color-primary-gradient);
  border-radius: 2px;
  transition: width 0.3s;
}

.result-content {
  font-size: 13px;
  color: var(--color-text-secondary);
  max-height: 150px;
  overflow-y: auto;
  white-space: pre-wrap;
}

.error-text {
  color: #e74c3c;
}

.dialog-footer {
  padding: 16px 20px;
  border-top: 1px solid var(--color-border);
  display: flex;
  justify-content: flex-end;
}

.cancel-btn {
  padding: 10px 20px;
  background: transparent;
  border: 1px solid var(--color-border);
  border-radius: 8px;
  color: var(--color-text-tertiary);
  cursor: pointer;
}
</style>
