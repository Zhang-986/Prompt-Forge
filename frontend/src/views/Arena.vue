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
    <!-- Header -->
    <header class="header">
      <div class="header-left">
        <a-button @click="goBack">
          <template #icon>
            <ArrowLeftOutlined />
          </template>
          返回
        </a-button>
        <ThunderboltOutlined class="logo-icon" />
        <span class="page-title">多模型竞技场</span>
      </div>
      <div class="header-right">
        <a-button @click="router.push('/arena/history')">
          <template #icon>
            <HistoryOutlined />
          </template>
          竞技历史
        </a-button>
        <a-button @click="openLeaderboard">
          <BarChartOutlined /> 胜率排行
        </a-button>
      </div>
    </header>

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

/* 深色主题渐变 */
[data-theme="dark"] .arena-container,
:root:not([data-theme="light"]) .arena-container {
  background: linear-gradient(135deg, var(--color-bg-primary) 0%, var(--color-bg-tertiary) 100%);
}

.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 32px;
  border-bottom: 1px solid var(--color-border);
  background: var(--color-bg-secondary);
  backdrop-filter: blur(10px);
  position: sticky;
  top: 0;
  z-index: 100;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.back-btn {
  padding: 8px 16px;
  background: transparent;
  border: 1px solid var(--color-border);
  border-radius: 6px;
  color: var(--color-text-tertiary);
  cursor: pointer;
  transition: all 0.2s;
}

.back-btn:hover {
  border-color: var(--color-primary);
  color: var(--color-primary);
}

.header-right {
  display: flex;
  align-items: center;
  gap: 16px;
}

.history-btn {
  padding: 8px 16px;
  background: transparent;
  border: 1px solid var(--color-border);
  border-radius: 6px;
  color: var(--color-text-tertiary);
  cursor: pointer;
  transition: all 0.2s;
}

.history-btn:hover {
  border-color: var(--color-primary);
  color: var(--color-primary);
}

.logo-icon {
  font-size: 24px;
}

.page-title {
  font-size: 20px;
  font-weight: 600;
  color: var(--color-text-primary);
}

.main-content {
  max-width: 1600px;
  margin: 0 auto;
  padding: 32px;
}

.config-section {
  background: var(--color-bg-elevated);
  border: 1px solid var(--color-border);
  border-radius: 16px;
  padding: 24px;
  margin-bottom: 32px;
  backdrop-filter: blur(10px);
}

.config-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 24px;
  margin-bottom: 20px;
}

.config-item label,
.variables-section>label,
.models-section>label,
.prompt-preview>label {
  display: block;
  margin-bottom: 8px;
  font-size: 14px;
  color: var(--color-text-tertiary);
}

.config-item select,
.variable-item input {
  width: 100%;
  padding: 12px;
  background: var(--color-bg-secondary);
  border: 1px solid var(--color-border);
  border-radius: 8px;
  color: var(--color-text-primary);
  font-size: 14px;
  outline: none;
  transition: all 0.2s;
}

.config-item select:focus,
.variable-item input:focus {
  border-color: var(--color-primary);
  box-shadow: 0 0 0 3px rgba(94, 106, 210, 0.2);
}

.prompt-preview {
  margin-bottom: 20px;
}

.prompt-preview pre {
  padding: 16px;
  background: var(--color-bg-secondary);
  border-radius: 8px;
  font-size: 13px;
  color: var(--color-text-secondary);
  white-space: pre-wrap;
  word-wrap: break-word;
  max-height: 150px;
  overflow-y: auto;
}

.variables-section {
  margin-bottom: 20px;
}

.variables-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
  gap: 16px;
}

.variable-item label {
  display: block;
  margin-bottom: 4px;
  font-size: 12px;
  color: #5e6ad2;
}

.models-section {
  margin-bottom: 20px;
}

.models-grid {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
}

.model-checkbox {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px 18px;
  background: rgba(255, 255, 255, 0.05);
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 10px;
  cursor: pointer;
  transition: all 0.2s;
}

.model-checkbox:has(input:checked) {
  border-color: #5e6ad2;
  background: rgba(94, 106, 210, 0.2);
}

.model-checkbox input {
  accent-color: #5e6ad2;
}

.model-icon {
  font-size: 18px;
}

.action-row {
  display: flex;
  gap: 12px;
}

.compete-btn {
  padding: 14px 32px;
  background: linear-gradient(135deg, #5e6ad2, #8b5cf6);
  border: none;
  border-radius: 10px;
  color: #fff;
  font-size: 16px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.3s;
}

.compete-btn:hover:not(:disabled) {
  transform: translateY(-2px);
  box-shadow: 0 8px 25px rgba(94, 106, 210, 0.4);
}

.compete-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.stop-btn {
  padding: 14px 24px;
  background: linear-gradient(135deg, #e74c3c, #c0392b);
  border: none;
  border-radius: 10px;
  color: #fff;
  cursor: pointer;
  transition: all 0.2s;
}

.stop-btn:hover {
  transform: translateY(-2px);
}

/* 结果区域 */
.results-section {
  margin-top: 32px;
}

.results-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.results-header h3 {
  margin: 0;
  font-size: 20px;
  font-weight: 600;
}

.results-actions {
  display: flex;
  gap: 8px;
}

.results-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.result-card {
  background: rgba(255, 255, 255, 0.03);
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 16px;
  overflow: hidden;
  transition: all 0.3s;
}

.result-card:hover {
  border-color: rgba(94, 106, 210, 0.5);
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.3);
}

.result-card.winner {
  border-color: #f59e0b;
  box-shadow: 0 0 20px rgba(245, 158, 11, 0.2);
  background: linear-gradient(to bottom, rgba(245, 158, 11, 0.05), rgba(255, 255, 255, 0.03));
}

.result-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 20px;
  background: rgba(0, 0, 0, 0.4);
  border-bottom: 1px solid rgba(255, 255, 255, 0.1);
  cursor: pointer;
  transition: background 0.2s;
}

.result-header:hover {
  background: rgba(0, 0, 0, 0.5);
}

.result-card.collapsed .result-header {
  border-bottom: none;
}

.model-info {
  display: flex;
  align-items: center;
  gap: 10px;
}

.model-icon-large {
  font-size: 24px;
}

.model-name {
  font-weight: 600;
  font-size: 16px;
  color: #fff;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 12px;
}

.expand-icon {
  color: rgba(255, 255, 255, 0.6);
  font-size: 14px;
  transition: transform 0.2s;
}

.status-badge {
  padding: 6px 12px;
  border-radius: 20px;
  font-size: 12px;
  font-weight: 500;
}

.status-badge.done {
  background: rgba(39, 174, 96, 0.2);
  color: #27ae60;
}

.status-badge.loading {
  background: rgba(243, 156, 18, 0.2);
  color: #f39c12;
}

.status-badge.error {
  background: rgba(231, 76, 60, 0.2);
  color: #e74c3c;
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
  padding: 20px;
  min-height: 200px;
}

.error-message {
  color: #e74c3c;
  padding: 16px;
  background: rgba(231, 76, 60, 0.1);
  border-radius: 8px;
}

.waiting-message {
  color: #888;
  font-style: italic;
}

.typing-indicator {
  display: flex;
  align-items: center;
  gap: 8px;
}

/* Markdown 样式 */
.markdown-body {
  color: #e0e0e0;
  line-height: 1.7;
  font-size: 15px;
}

.markdown-body :deep(h1),
.markdown-body :deep(h2),
.markdown-body :deep(h3),
.markdown-body :deep(h4) {
  color: #fff;
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
  background: rgba(94, 106, 210, 0.2);
  padding: 2px 6px;
  border-radius: 4px;
  font-family: 'Fira Code', 'Consolas', monospace;
  font-size: 0.9em;
  color: #a5b4fc;
}

.markdown-body :deep(pre) {
  background: rgba(0, 0, 0, 0.5);
  padding: 16px;
  border-radius: 8px;
  overflow-x: auto;
  margin-bottom: 1em;
}

.markdown-body :deep(pre code) {
  background: transparent;
  padding: 0;
}

.markdown-body :deep(blockquote) {
  border-left: 3px solid #5e6ad2;
  padding-left: 16px;
  margin-left: 0;
  color: #aaa;
  font-style: italic;
}

.markdown-body :deep(strong) {
  color: #fff;
  font-weight: 600;
}

.markdown-body :deep(em) {
  color: #c0c0c0;
}

.markdown-body :deep(a) {
  color: #5e6ad2;
  text-decoration: none;
}

.markdown-body :deep(a:hover) {
  text-decoration: underline;
}

.markdown-body :deep(h2) {
  font-size: 1.3em;
  border-bottom: 1px solid rgba(255, 255, 255, 0.1);
  padding-bottom: 0.3em;
}

.vote-section {
  padding: 16px 20px;
  border-top: 1px solid rgba(255, 255, 255, 0.05);
  display: flex;
  justify-content: center;
}

.vote-btn {
  width: 100%;
}

.voted-badge {
  color: #f59e0b;
  font-weight: 600;
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px;
  background: rgba(245, 158, 11, 0.1);
  border-radius: 8px;
  width: 100%;
  justify-content: center;
}

.rank-num {
  font-weight: bold;
  color: #888;
  display: inline-block;
  width: 24px;
  text-align: center;
}

.win-rate {
  font-family: monospace;
  font-weight: bold;
}

.high-rate {
  color: #cf1322;
}

.low-rate {
  color: #3f8600;
}

.markdown-body :deep(table) {
  width: 100%;
  border-collapse: collapse;
  margin-bottom: 1em;
}

.markdown-body :deep(th),
.markdown-body :deep(td) {
  border: 1px solid rgba(255, 255, 255, 0.1);
  padding: 10px;
  text-align: left;
}

.markdown-body :deep(th) {
  background: rgba(255, 255, 255, 0.05);
  font-weight: 600;
}

/* 空模型提示 */
.empty-models {
  padding: 20px;
  background: rgba(255, 193, 7, 0.1);
  border: 1px dashed rgba(255, 193, 7, 0.5);
  border-radius: 8px;
  text-align: center;
}

.empty-models p {
  margin: 8px 0;
  color: #ffc107;
}

.empty-models a {
  color: #5e6ad2;
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
