<script setup lang="ts">
import { ref, onMounted, onUnmounted, computed, nextTick } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { getPrompts, getVersionHistory, type Prompt, type PromptVersion } from '../api/prompt'
import { getAvailableModels, submitVote, getLeaderboard, type ArenaEvent, type LeaderboardItem, type AvailableModelInfo } from '../api/arena'
import { message } from 'ant-design-vue'
import { ArrowLeftOutlined, ThunderboltOutlined, HistoryOutlined, PlayCircleOutlined, PauseCircleOutlined, WarningOutlined, CheckCircleOutlined, CloseCircleOutlined, ClockCircleOutlined, BarChartOutlined, TrophyOutlined, UpOutlined, DownOutlined } from '@ant-design/icons-vue'
import { marked } from 'marked'

// 配置 marked
marked.setOptions({
  breaks: true,
  gfm: true
})

const router = useRouter()
const route = useRoute()

// 状态
// loading 状态由 isCompeting 管理
const prompts = ref<Prompt[]>([])
const versions = ref<PromptVersion[]>([])
const models = ref<AvailableModelInfo[]>([])
const modelMap = ref<Map<string, AvailableModelInfo>>(new Map())
const selectedPromptId = ref<number | null>(null)
const selectedVersionId = ref<number | null>(null)
const selectedModels = ref<string[]>([])
const variables = ref<Record<string, string>>({})
const isCompeting = ref(false)
const eventSource = ref<EventSource | null>(null)

// 每个模型的输出
const modelOutputs = ref<Record<string, { content: string; finished: boolean; error?: string }>>({})

// 投票状态
const hasVoted = ref(false)
const votingFor = ref<string | null>(null)

// 排行榜状态
const leaderboardVisible = ref(false)
const leaderboardData = ref<LeaderboardItem[]>([])
const leaderboardLoading = ref(false)

// 卡片展开/折叠状态
const expandedCards = ref<Set<string>>(new Set())

// 切换卡片展开状态
const toggleCard = (modelId: string) => {
  if (expandedCards.value.has(modelId)) {
    expandedCards.value.delete(modelId)
  } else {
    expandedCards.value.add(modelId)
  }
  // 触发响应式更新
  expandedCards.value = new Set(expandedCards.value)
}

// 展开所有卡片
const expandAll = () => {
  selectedModels.value.forEach(id => expandedCards.value.add(id))
  expandedCards.value = new Set(expandedCards.value)
}

// 折叠所有卡片
const collapseAll = () => {
  expandedCards.value = new Set()
}

// 加载 Prompts
const loadPrompts = async () => {
  const workspaceId = localStorage.getItem('currentWorkspaceId')
  if (!workspaceId) {
    message.warning('请先在主页选择或创建一个工作空间')
    return
  }
  try {
    const res = await getPrompts(parseInt(workspaceId))
    if (res.code === 200) {
      prompts.value = res.data
    }
  } catch (error) {
    console.error('加载 Prompts 失败:', error)
  }
}

// 加载版本历史
const loadVersions = async (promptId: number) => {
  try {
    const res = await getVersionHistory(promptId)
    if (res.code === 200) {
      versions.value = res.data
      const firstVersion = versions.value[0]
      if (firstVersion) {
        selectedVersionId.value = firstVersion.id
        parseVariables(firstVersion.content)
      }
    }
  } catch (error) {
    console.error('加载版本失败:', error)
  }
}

// 加载可用模型
const loadModels = async () => {
  try {
    const res = await getAvailableModels()
    if (res.code === 200) {
      models.value = res.data
      // 构建 modelId -> info 的映射
      const map = new Map<string, AvailableModelInfo>()
      res.data.forEach(m => map.set(m.modelId, m))
      modelMap.value = map
      // 默认选中所有模型
      selectedModels.value = res.data.map(m => m.modelId)
    }
  } catch (error) {
    console.error('加载模型失败:', error)
  }
}

// 解析 Prompt 中的变量 {{xxx}}
const parseVariables = (content: string) => {
  const regex = /\{\{(\w+)\}\}/g
  const matches = content.matchAll(regex)
  const vars: Record<string, string> = {}
  for (const match of matches) {
    const varName = match[1]
    if (varName) {
      vars[varName] = ''
    }
  }
  variables.value = vars
}

// Prompt 选择变化
const onPromptChange = (promptId: number) => {
  selectedPromptId.value = promptId
  selectedVersionId.value = null
  versions.value = []
  loadVersions(promptId)
}

// 版本选择变化
const onVersionChange = (versionId: number) => {
  const version = versions.value.find(v => v.id === versionId)
  if (version) {
    parseVariables(version.content)
  }
}

// 渲染 Markdown
const renderMarkdown = (content: string) => {
  if (!content) return ''
  try {
    return marked(content) as string
  } catch (e) {
    return content
  }
}

// 开始竞技
const startCompete = () => {
  if (!selectedVersionId.value) {
    message.warning('请选择一个 Prompt 版本')
    return
  }
  if (selectedModels.value.length === 0) {
    message.warning('请至少选择一个模型')
    return
  }

  // 初始化输出
  modelOutputs.value = {}
  selectedModels.value.forEach(modelId => {
    modelOutputs.value[modelId] = { content: '', finished: false }
  })

  // 重置投票状态
  hasVoted.value = false
  votingFor.value = null

  isCompeting.value = true

  const token = localStorage.getItem('token')
  const baseUrl = import.meta.env.VITE_API_URL || '/api'

  fetch(`${baseUrl}/arena/compete`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      'Authorization': token ? `Bearer ${token}` : ''
    },
    body: JSON.stringify({
      promptVersionId: selectedVersionId.value,
      variables: variables.value,
      modelIds: selectedModels.value
    })
  }).then(response => {
    const reader = response.body?.getReader()
    const decoder = new TextDecoder()
    let buffer = '' // 用于处理跨 chunk 的数据

    const read = () => {
      reader?.read().then(({ done, value }) => {
        if (done) {
          isCompeting.value = false
          return
        }

        // 解码并追加到 buffer
        buffer += decoder.decode(value, { stream: true })

        // 按换行符分割，但保留最后一个可能不完整的行
        const lines = buffer.split('\n')
        buffer = lines.pop() || '' // 最后一行可能不完整，保留到下次处理

        for (const line of lines) {
          const trimmedLine = line.trim()
          if (trimmedLine.startsWith('data:')) {
            try {
              const jsonStr = trimmedLine.substring(5).trim()
              if (jsonStr) {
                const data = JSON.parse(jsonStr) as ArenaEvent
                handleArenaEvent(data)
              }
            } catch (e) {
              console.warn('SSE 解析跳过:', trimmedLine)
            }
          }
        }
        read()
      })
    }
    read()
  }).catch(error => {
    console.error('SSE 连接失败:', error)
    message.error('连接失败，请检查后端是否启动')
    isCompeting.value = false
  })
}

// 处理 Arena 事件
const handleArenaEvent = (event: ArenaEvent) => {
  const output = modelOutputs.value[event.modelId]
  if (!output) return

  if (event.type === 'content') {
    // 直接修改内容，而不是创建新对象，减少 GC 压力
    output.content += event.content
    // 强制触发响应式更新
    modelOutputs.value = { ...modelOutputs.value }
  } else if (event.type === 'finish') {
    output.finished = true
    modelOutputs.value = { ...modelOutputs.value }
  } else if (event.type === 'error') {
    output.error = event.content
    output.finished = true
    modelOutputs.value = { ...modelOutputs.value }
  }
}

// 停止竞技
const stopCompete = () => {
  eventSource.value?.close()
  isCompeting.value = false
}

// 返回列表
const goBack = () => {
  router.push('/prompts')
}

// 当前版本内容预览
const currentVersionContent = computed(() => {
  if (!selectedVersionId.value) return ''
  const version = versions.value.find(v => v.id === selectedVersionId.value)
  return version?.content || ''
})

// 变量列表
const variableNames = computed(() => Object.keys(variables.value))

// 获取模型显示名称（使用后端返回的 displayName，回退到 modelId）
const getModelDisplayName = (modelId: string) => {
  const info = modelMap.value.get(modelId)
  return info?.displayName || modelId
}

// 模型图标（根据 provider 返回图标）
const getModelIcon = (modelId: string) => {
  const info = modelMap.value.get(modelId)
  const provider = info?.provider || modelId.split(':')[0] || modelId
  const icons: Record<string, string> = {
    'google': '🌐',
    'zhipu': '🧠',
    'deepseek': '🔍',
    'openai': '🤖',
    'claude': '🎭',
    'aliyun': '🐱',
    'moonshot': '🌙',
    'cloudflare': '☁️',
    'modelscope': '🔬'
  }
  return icons[provider] || '💬'
}

// 处理投票
const handleVote = async (modelId: string) => {
  if (hasVoted.value) return
  if (selectedModels.value.length < 2) return

  try {
    // 找出另一个模型作为败者（简化逻辑：暂时只支持两两对比的投票，多模型时只记录点击的胜者）
    // 实际业务中可能需要更复杂的投票 UI
    const loser = selectedModels.value.find(m => m !== modelId) || 'other'

    await submitVote({
      winnerModel: modelId,
      loserModel: loser
    })

    hasVoted.value = true
    votingFor.value = modelId
    message.success('感谢您的投票！')
  } catch (error) {
    message.error('投票失败')
  }
}



// 打开排行榜
const openLeaderboard = async () => {
  leaderboardVisible.value = true
  leaderboardLoading.value = true
  try {
    const res = await getLeaderboard()
    if (res.code === 200) {
      leaderboardData.value = res.data
    }
  } catch (error) {
    message.error('加载排行榜失败')
  } finally {
    leaderboardLoading.value = false
  }
}

onMounted(() => {
  loadPrompts()
  loadModels()

  const promptId = route.query.promptId
  if (promptId) {
    selectedPromptId.value = Number(promptId)
    loadVersions(Number(promptId))
  }
})

onUnmounted(() => {
  eventSource.value?.close()
})
</script>

<template>
  <div class="arena-container">
    <!-- Header Removed -->

    <main class="main-content">
      <!-- 配置区 -->
      <div class="config-section">
        <div class="config-row">
          <!-- Prompt 选择 -->
          <div class="config-item">
            <label>选择 Prompt</label>
            <select v-model="selectedPromptId" @change="onPromptChange(selectedPromptId!)">
              <option :value="null" disabled>请选择 Prompt</option>
              <option v-for="p in prompts" :key="p.id" :value="p.id">{{ p.name }}</option>
            </select>
          </div>

          <!-- 版本选择 -->
          <div class="config-item">
            <label>选择版本</label>
            <select v-model="selectedVersionId" @change="onVersionChange(selectedVersionId!)"
              :disabled="!selectedPromptId">
              <option :value="null" disabled>请选择版本</option>
              <option v-for="v in versions" :key="v.id" :value="v.id">
                v{{ v.versionNumber }} - {{ v.commitMessage || '无描述' }}
              </option>
            </select>
          </div>
        </div>

        <!-- Prompt 预览 -->
        <div v-if="currentVersionContent" class="prompt-preview">
          <label>Prompt 内容预览</label>
          <pre>{{ currentVersionContent }}</pre>
        </div>

        <!-- 变量输入 -->
        <div v-if="variableNames.length > 0" class="variables-section">
          <label>变量输入</label>
          <div class="variables-grid">
            <div v-for="varName in variableNames" :key="varName" class="variable-item">
              <label class="var-label">{{ varName }}</label>
              <input v-model="variables[varName]" type="text" placeholder="请输入变量值" />
            </div>
          </div>
        </div>

        <!-- 模型选择 -->
        <div class="models-section">
          <label>选择模型 (可多选)</label>
          <div v-if="models.length === 0" class="empty-models">
            <p>
              <WarningOutlined /> 您还没有配置任何模型
            </p>
            <p>请先前往 <router-link to="/settings/models">模型配置</router-link> 添加您的 API Key</p>
          </div>
          <div v-else class="models-grid">
            <label v-for="model in models" :key="model.modelId" class="model-checkbox">
              <input type="checkbox" v-model="selectedModels" :value="model.modelId" />
              <span class="model-icon">{{ getModelIcon(model.modelId) }}</span>
              <span>{{ model.displayName }}</span>
            </label>
          </div>
        </div>

        <!-- 开始按钮 -->
        <div class="action-row">
          <a-button type="primary" size="large" @click="startCompete" :disabled="isCompeting || !selectedVersionId"
            :loading="isCompeting">
            <template #icon>
              <PlayCircleOutlined />
            </template>
            {{ isCompeting ? '对比中...' : '开始对比' }}
          </a-button>
          <a-button v-if="isCompeting" danger @click="stopCompete">
            <template #icon>
              <PauseCircleOutlined />
            </template>
            停止
          </a-button>
        </div>
      </div>

      <!-- 结果展示区 - 可折叠卡片布局 -->
      <div v-if="Object.keys(modelOutputs).length > 0" class="results-section">
        <div class="results-header">
          <h3>
            <BarChartOutlined /> 对比结果
          </h3>
          <div class="results-actions">
            <a-button size="small" @click="expandAll">展开全部</a-button>
            <a-button size="small" @click="collapseAll">折叠全部</a-button>
          </div>
        </div>
        <div class="results-list">
          <div v-for="modelId in selectedModels" :key="modelId" class="result-card"
            :class="{ 'winner': votingFor === modelId, 'collapsed': !expandedCards.has(modelId) }">
            <!-- 卡片头部 - 可点击展开/折叠 -->
            <div class="result-header" @click="toggleCard(modelId)">
              <div class="model-info">
                <span class="model-icon-large">{{ getModelIcon(modelId) }}</span>
                <span class="model-name">{{ getModelDisplayName(modelId) }}</span>
              </div>
              <div class="header-right">
                <div class="status-badge"
                  :class="{ done: modelOutputs[modelId]?.finished, error: modelOutputs[modelId]?.error, loading: !modelOutputs[modelId]?.finished && !modelOutputs[modelId]?.error }">
                  <span v-if="modelOutputs[modelId]?.error">
                    <CloseCircleOutlined /> 失败
                  </span>
                  <span v-else-if="modelOutputs[modelId]?.finished">
                    <CheckCircleOutlined /> 完成
                  </span>
                  <span v-else class="loading-dots">生成中<span class="dots"></span></span>
                </div>
                <span class="expand-icon">
                  <UpOutlined v-if="expandedCards.has(modelId)" />
                  <DownOutlined v-else />
                </span>
              </div>
            </div>

            <!-- 卡片内容 - 可折叠 -->
            <div v-show="expandedCards.has(modelId)" class="result-content">
              <div v-if="modelOutputs[modelId]?.error" class="error-message">
                <CloseCircleOutlined /> {{ modelOutputs[modelId].error }}
              </div>
              <div v-else-if="modelOutputs[modelId]?.content" class="markdown-body"
                v-html="renderMarkdown(modelOutputs[modelId]?.content || '')">
              </div>
              <div v-else class="waiting-message">
                <span class="typing-indicator">
                  <ClockCircleOutlined /> 等待输出...
                </span>
              </div>
            </div>

            <!-- 投票按钮 (仅当竞技结束且至少2个模型时显示) -->
            <div v-if="expandedCards.has(modelId) && !isCompeting && selectedModels.length >= 2 && modelOutputs[modelId]?.finished && !modelOutputs[modelId]?.error"
              class="vote-section">
              <div v-if="!hasVoted" class="vote-btn-wrapper">
                <a-button type="primary" ghost class="vote-btn" @click.stop="handleVote(modelId)">
                  <template #icon>
                    <TrophyOutlined />
                  </template>
                  投它一票
                </a-button>
              </div>
              <div v-else-if="votingFor === modelId" class="voted-badge">
                <TrophyOutlined /> 已投票给此模型
              </div>
            </div>
          </div>
        </div>
      </div>
    </main>

    <!-- 排行榜 Modal -->
    <a-modal v-model:visible="leaderboardVisible" title="🤖 模型胜率排行榜" :footer="null" width="600px">
      <a-table :dataSource="leaderboardData" :loading="leaderboardLoading" :pagination="false" rowKey="modelId">
        <a-table-column title="排名" width="80px">
          <template #default="{ index }">
            <span v-if="index === 0" style="font-size: 1.5em">🥇</span>
            <span v-else-if="index === 1" style="font-size: 1.5em">🥈</span>
            <span v-else-if="index === 2" style="font-size: 1.5em">🥉</span>
            <span v-else class="rank-num">{{ index + 1 }}</span>
          </template>
        </a-table-column>
        <a-table-column title="模型" dataIndex="modelId">
          <template #default="{ text }">
            <span style="font-size: 1.2em; margin-right: 8px">{{ getModelIcon(text) }}</span>
            <span style="font-weight: 500">{{ getModelDisplayName(text) }}</span>
          </template>
        </a-table-column>
        <a-table-column title="胜率" dataIndex="winRate" align="right">
          <template #default="{ text }">
            <span class="win-rate" :class="{ 'high-rate': text >= 50, 'low-rate': text < 50 }">
              {{ text }}%
            </span>
          </template>
        </a-table-column>
        <a-table-column title="胜/负/总" align="center">
          <template #default="{ record }">
            <span style="color: #52c41a">{{ record.wins }}</span> /
            <span style="color: #ff4d4f">{{ record.losses }}</span> /
            <span style="color: #888">{{ record.total }}</span>
          </template>
        </a-table-column>
      </a-table>
    </a-modal>
  </div>
</template>

<style scoped>
.arena-container {
  min-height: 100vh;
  background: var(--color-bg-primary);
  color: var(--color-text-primary);
}

.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: var(--space-4) var(--space-8);
  border-bottom: 1px solid var(--color-border-light);
  background: var(--color-bg-secondary);
  position: sticky;
  top: 0;
  z-index: 100;
}

.header-left {
  display: flex;
  align-items: center;
  gap: var(--space-3);
}

.back-btn {
  padding: var(--space-2) var(--space-4);
  background: transparent;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  color: var(--color-text-secondary);
  cursor: pointer;
  transition: all var(--transition-fast);
}

.back-btn:hover {
  border-color: var(--color-primary);
  color: var(--color-primary);
  background: var(--color-primary-muted);
}

.header-right {
  display: flex;
  align-items: center;
  gap: var(--space-4);
}

.history-btn {
  padding: var(--space-2) var(--space-4);
  background: transparent;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  color: var(--color-text-secondary);
  cursor: pointer;
  transition: all var(--transition-fast);
}

.history-btn:hover {
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

.main-content {
  max-width: 960px;
  margin: 0 auto;
  padding: var(--space-8);
}

.config-section {
  background: var(--color-bg-secondary);
  border: 1px solid var(--color-border-light);
  border-radius: var(--radius-lg);
  padding: var(--space-6);
  margin-bottom: var(--space-8);
}

.config-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: var(--space-6);
  margin-bottom: var(--space-5);
}

.config-item label,
.variables-section>label,
.models-section>label,
.prompt-preview>label {
  display: block;
  margin-bottom: var(--space-2);
  font-size: var(--text-sm);
  color: var(--color-text-secondary);
}

.config-item select,
.variable-item input {
  width: 100%;
  padding: var(--space-3);
  background: var(--color-bg-tertiary);
  border: 1px solid var(--color-border-light);
  border-radius: var(--radius-md);
  color: var(--color-text-primary);
  font-size: var(--text-sm);
  outline: none;
  transition: all var(--transition-fast);
}

.config-item select:focus,
.variable-item input:focus {
  border-color: var(--color-primary);
}

.prompt-preview {
  margin-bottom: var(--space-5);
}

.prompt-preview pre {
  padding: var(--space-4);
  background: var(--color-bg-tertiary);
  border-radius: var(--radius-md);
  font-size: var(--text-sm);
  color: var(--color-text-secondary);
  white-space: pre-wrap;
  word-wrap: break-word;
  max-height: 150px;
  overflow-y: auto;
}

.variables-section {
  margin-bottom: var(--space-5);
}

.variables-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
  gap: var(--space-4);
}

.variable-item label {
  display: block;
  margin-bottom: var(--space-1);
  font-size: var(--text-xs);
  color: var(--color-primary);
}

.models-section {
  margin-bottom: var(--space-5);
}

.models-grid {
  display: flex;
  gap: var(--space-3);
  flex-wrap: wrap;
}

.model-checkbox {
  display: flex;
  align-items: center;
  gap: var(--space-2);
  padding: var(--space-3) var(--space-4);
  background: var(--color-bg-tertiary);
  border: 1px solid var(--color-border-light);
  border-radius: var(--radius-md);
  cursor: pointer;
  transition: all var(--transition-fast);
}

.model-checkbox:has(input:checked) {
  border-color: var(--color-primary);
  background: var(--color-primary-muted);
}

.model-checkbox input {
  accent-color: var(--color-primary);
}

.model-icon {
  font-size: var(--text-lg);
}

.action-row {
  display: flex;
  gap: var(--space-3);
}

.compete-btn {
  padding: var(--space-3) var(--space-8);
  background: var(--color-primary);
  border: none;
  border-radius: var(--radius-md);
  color: #fff;
  font-size: var(--text-base);
  font-weight: 500;
  cursor: pointer;
  transition: all var(--transition-fast);
}

.compete-btn:hover:not(:disabled) {
  background: var(--color-primary-hover);
}

.compete-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.stop-btn {
  padding: var(--space-3) var(--space-6);
  background: var(--color-danger);
  border: none;
  border-radius: var(--radius-md);
  color: #fff;
  cursor: pointer;
  transition: all var(--transition-fast);
}

.stop-btn:hover {
  opacity: 0.9;
}

/* 结果区域 */
.results-section {
  margin-top: var(--space-8);
}

.results-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: var(--space-4);
}

.results-header h3 {
  margin: 0;
  font-size: var(--text-xl);
  font-weight: 600;
}

.results-actions {
  display: flex;
  gap: var(--space-2);
}

.results-list {
  display: flex;
  flex-direction: column;
  gap: var(--space-3);
}

.result-card {
  background: var(--color-bg-secondary);
  border: 1px solid var(--color-border-light);
  border-radius: var(--radius-lg);
  overflow: hidden;
  transition: all var(--transition-fast);
}

.result-card:hover {
  border-color: var(--color-primary);
}

.result-card.winner {
  border-color: var(--color-warning);
  background: var(--color-bg-secondary);
}

.result-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: var(--space-4) var(--space-5);
  background: var(--color-bg-tertiary);
  border-bottom: 1px solid var(--color-border-light);
  cursor: pointer;
  transition: background var(--transition-fast);
}

.result-header:hover {
  background: var(--color-bg-elevated);
}

.result-card.collapsed .result-header {
  border-bottom: none;
}

.model-info {
  display: flex;
  align-items: center;
  gap: var(--space-3);
}

.model-icon-large {
  font-size: var(--text-2xl);
}

.model-name {
  font-weight: 600;
  font-size: var(--text-base);
  color: var(--color-text-primary);
}

.expand-icon {
  color: var(--color-text-tertiary);
  font-size: var(--text-sm);
  transition: transform var(--transition-fast);
}

.status-badge {
  padding: var(--space-2) var(--space-3);
  border-radius: var(--radius-full);
  font-size: var(--text-xs);
  font-weight: 500;
}

.status-badge.done {
  background: rgba(16, 163, 127, 0.15);
  color: var(--color-success);
}

.status-badge.loading {
  background: rgba(245, 158, 11, 0.15);
  color: var(--color-warning);
}

.status-badge.error {
  background: rgba(239, 68, 68, 0.15);
  color: var(--color-danger);
}

.loading-dots .dots::after {
  content: '';
  animation: dots 1.5s infinite;
}

@keyframes dots {

  0%,
  20% {
    content: '';
  }

  40% {
    content: '.';
  }

  60% {
    content: '..';
  }

  80%,
  100% {
    content: '...';
  }
}

.result-content {
  padding: var(--space-5);
  min-height: 200px;
}

.error-message {
  color: var(--color-danger);
  padding: var(--space-4);
  background: rgba(239, 68, 68, 0.1);
  border-radius: var(--radius-md);
}

.waiting-message {
  color: var(--color-text-tertiary);
  font-style: italic;
}

.typing-indicator {
  display: flex;
  align-items: center;
  gap: var(--space-2);
}

/* Markdown 样式 */
.markdown-body {
  color: var(--color-text-primary);
  line-height: 1.7;
  font-size: var(--text-base);
}

.markdown-body :deep(h1),
.markdown-body :deep(h2),
.markdown-body :deep(h3),
.markdown-body :deep(h4) {
  color: var(--color-text-primary);
  margin-top: 1.5em;
  margin-bottom: 0.5em;
  font-weight: 600;
}

.markdown-body :deep(h1) {
  font-size: 1.5em;
}

.markdown-body :deep(h2) {
  font-size: 1.3em;
}

.markdown-body :deep(h3) {
  font-size: 1.15em;
}

.markdown-body :deep(h4) {
  font-size: 1em;
}

.markdown-body :deep(p) {
  margin-bottom: 1em;
}

.markdown-body :deep(ul),
.markdown-body :deep(ol) {
  margin-left: 1.5em;
  margin-bottom: 1em;
}

.markdown-body :deep(li) {
  margin-bottom: 0.5em;
}

.markdown-body :deep(code) {
  background: var(--color-bg-tertiary);
  padding: 2px 6px;
  border-radius: var(--radius-sm);
  font-family: var(--font-mono);
  font-size: 0.9em;
  color: var(--color-primary);
}

.markdown-body :deep(pre) {
  background: var(--color-bg-tertiary);
  padding: var(--space-4);
  border-radius: var(--radius-md);
  overflow-x: auto;
  margin-bottom: 1em;
}

.markdown-body :deep(pre code) {
  background: transparent;
  padding: 0;
}

.markdown-body :deep(blockquote) {
  border-left: 3px solid var(--color-primary);
  padding-left: var(--space-4);
  margin-left: 0;
  color: var(--color-text-secondary);
  font-style: italic;
}

.markdown-body :deep(strong) {
  color: var(--color-text-primary);
  font-weight: 600;
}

.markdown-body :deep(em) {
  color: var(--color-text-secondary);
}

.markdown-body :deep(a) {
  color: var(--color-primary);
  text-decoration: none;
}

.markdown-body :deep(a:hover) {
  text-decoration: underline;
}

.markdown-body :deep(h2) {
  font-size: 1.3em;
  border-bottom: 1px solid var(--color-border-light);
  padding-bottom: 0.3em;
}

.vote-section {
  padding: var(--space-4) var(--space-5);
  border-top: 1px solid var(--color-border-light);
  display: flex;
  justify-content: center;
}

.vote-btn {
  width: 100%;
}

.voted-badge {
  color: var(--color-warning);
  font-weight: 600;
  display: flex;
  align-items: center;
  gap: var(--space-2);
  padding: var(--space-2);
  background: rgba(245, 158, 11, 0.1);
  border-radius: var(--radius-md);
  width: 100%;
  justify-content: center;
}

.rank-num {
  font-weight: bold;
  color: var(--color-text-tertiary);
  display: inline-block;
  width: 24px;
  text-align: center;
}

.win-rate {
  font-family: var(--font-mono);
  font-weight: bold;
}

.high-rate {
  color: var(--color-danger);
}

.low-rate {
  color: var(--color-success);
}

.markdown-body :deep(table) {
  width: 100%;
  border-collapse: collapse;
  margin-bottom: 1em;
}

.markdown-body :deep(th),
.markdown-body :deep(td) {
  border: 1px solid var(--color-border-light);
  padding: var(--space-3);
  text-align: left;
}

.markdown-body :deep(th) {
  background: var(--color-bg-tertiary);
  font-weight: 600;
}

/* 空模型提示 */
.empty-models {
  padding: var(--space-5);
  background: rgba(245, 158, 11, 0.1);
  border: 1px dashed var(--color-warning);
  border-radius: var(--radius-md);
  text-align: center;
}

.empty-models p {
  margin: var(--space-2) 0;
  color: var(--color-warning);
}

.empty-models a {
  color: var(--color-primary);
  text-decoration: underline;
}

/* 响应式 */
@media (max-width: 900px) {
  .config-row {
    grid-template-columns: 1fr;
  }

  .results-grid {
    grid-template-columns: 1fr;
  }
}
</style>
