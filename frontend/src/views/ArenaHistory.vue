<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { getArenaHistory, getArenaHistoryDetail, type ArenaHistoryItem, type ArenaHistoryDetail } from '../api/arenaHistory'
import { message } from 'ant-design-vue'
import { ArrowLeftOutlined, HistoryOutlined, FileTextOutlined, DollarOutlined, ClockCircleOutlined } from '@ant-design/icons-vue'

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
    message.error('加载详情失败')
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
    <!-- Header Removed -->

    <!-- Main Content -->

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
                    <DollarOutlined /> {{ result.tokensUsed || 0 }}
                  </span>
                  <span class="stat" title="响应时间">
                    <ClockCircleOutlined /> {{ result.latencyMs || 0 }}ms
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
  gap: var(--space-3);
}

.back-btn, .nav-btn {
  padding: var(--space-2) var(--space-4);
  background: transparent;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  color: var(--color-text-secondary);
  cursor: pointer;
  transition: all var(--transition-fast);
}

.back-btn:hover, .nav-btn:hover {
  border-color: var(--color-primary);
  color: var(--color-primary);
}

.logo-icon {
  font-size: var(--text-2xl);
  color: var(--color-primary);
}

.page-title {
  font-size: var(--text-xl);
  font-weight: 600;
  color: var(--color-text-primary);
}

.header-right {
  display: flex;
  align-items: center;
  gap: var(--space-4);
}

.main-content {
  max-width: 960px;
  margin: 0 auto;
  padding: var(--space-8);
}

.loading, .empty-state {
  text-align: center;
  padding: var(--space-12);
  color: var(--color-text-tertiary);
}

.primary-btn {
  margin-top: var(--space-4);
  padding: var(--space-3) var(--space-6);
  background: var(--color-primary);
  border: none;
  border-radius: var(--radius-md);
  color: #fff;
  cursor: pointer;
}

.history-list {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  gap: var(--space-5);
}

.history-card {
  background: var(--color-bg-secondary);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  padding: var(--space-5);
  cursor: pointer;
  transition: all var(--transition-fast);
}

.history-card:hover {
  border-color: var(--color-primary);
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: var(--space-3);
}

.session-id {
  font-weight: 600;
  color: var(--color-text-primary);
}

.status-badge {
  padding: var(--space-1) var(--space-3);
  border-radius: var(--radius-full);
  font-size: var(--text-xs);
}

.status-success {
  background: rgba(16, 163, 127, 0.1);
  color: var(--color-success);
}

.status-running {
  background: rgba(245, 158, 11, 0.1);
  color: var(--color-warning);
}

.status-failed {
  background: rgba(239, 68, 68, 0.1);
  color: var(--color-danger);
}

.model-tags {
  display: flex;
  flex-wrap: wrap;
  gap: var(--space-2);
  margin-bottom: var(--space-3);
}

.model-tag {
  padding: var(--space-1) var(--space-3);
  background: var(--color-primary-muted);
  border-radius: var(--radius-md);
  font-size: var(--text-xs);
  color: var(--color-primary);
}

.card-footer {
  color: var(--color-text-tertiary);
  font-size: var(--text-sm);
}

/* Dialog */
.dialog-overlay {
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
}

.detail-dialog {
  background: var(--color-bg-primary);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-xl);
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
  padding: var(--space-5);
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
  font-size: var(--text-2xl);
  cursor: pointer;
}

.dialog-body {
  padding: var(--space-5);
  overflow-y: auto;
  flex: 1;
}

.stats-summary {
  display: flex;
  gap: var(--space-6);
  margin-bottom: var(--space-6);
  padding: var(--space-4);
  background: var(--color-primary-muted);
  border-radius: var(--radius-lg);
}

.stat-item {
  display: flex;
  flex-direction: column;
  align-items: center;
}

.stat-label {
  font-size: var(--text-xs);
  color: var(--color-text-tertiary);
  margin-bottom: var(--space-1);
}

.stat-value {
  font-size: var(--text-2xl);
  font-weight: 600;
  color: var(--color-primary);
}

.prompt-section {
  margin-bottom: var(--space-6);
}

.prompt-section label,
.results-section label {
  display: block;
  margin-bottom: var(--space-2);
  font-size: var(--text-sm);
  color: var(--color-text-secondary);
}

.prompt-section pre {
  padding: var(--space-4);
  background: var(--color-bg-secondary);
  border-radius: var(--radius-md);
  font-size: var(--text-sm);
  color: var(--color-text-primary);
  white-space: pre-wrap;
  max-height: 120px;
  overflow-y: auto;
}

.results-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(280px, 1fr));
  gap: var(--space-4);
}

.result-card {
  background: var(--color-bg-secondary);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  padding: var(--space-4);
}

.result-card.result-error {
  border-color: var(--color-danger);
}

.result-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: var(--space-3);
}

.model-name {
  font-weight: 600;
  color: var(--color-text-primary);
}

.result-status {
  padding: var(--space-1) var(--space-2);
  border-radius: var(--radius-md);
  font-size: 11px;
}

.result-status.success {
  background: rgba(16, 163, 127, 0.1);
  color: var(--color-success);
}

.result-status.error {
  background: rgba(239, 68, 68, 0.1);
  color: var(--color-danger);
}

.result-stats {
  display: flex;
  gap: var(--space-4);
  margin-bottom: var(--space-2);
  font-size: var(--text-sm);
  color: var(--color-text-secondary);
}

.latency-bar-container {
  height: 4px;
  background: var(--color-border);
  border-radius: var(--radius-sm);
  margin-bottom: var(--space-3);
  overflow: hidden;
}

.latency-bar {
  height: 100%;
  background: var(--color-primary);
  border-radius: var(--radius-sm);
  transition: width var(--transition-base);
}

.result-content {
  font-size: var(--text-sm);
  color: var(--color-text-secondary);
  max-height: 150px;
  overflow-y: auto;
  white-space: pre-wrap;
}

.error-text {
  color: var(--color-danger);
}

.dialog-footer {
  padding: var(--space-4) var(--space-5);
  border-top: 1px solid var(--color-border);
  display: flex;
  justify-content: flex-end;
}

.cancel-btn {
  padding: var(--space-3) var(--space-5);
  background: transparent;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  color: var(--color-text-secondary);
  cursor: pointer;
}
</style>
