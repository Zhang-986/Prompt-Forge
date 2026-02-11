<script setup lang="ts">
import { ref, onMounted, onUnmounted, computed } from 'vue'
import { useRoute } from 'vue-router'
import { getPrompts, getVersionHistory, type Prompt, type PromptVersion } from '../api/prompt'
import { getAvailableModels, submitVote, getLeaderboard, type ArenaEvent, type LeaderboardItem, type AvailableModelInfo } from '../api/arena'
import { message } from 'ant-design-vue'
import {
  ThunderboltOutlined,
  TrophyOutlined,
  PlayCircleOutlined,
  StopOutlined,
  FireOutlined,
  SwapOutlined,
  DownOutlined,
  HistoryOutlined
} from '@ant-design/icons-vue'
import ArenaHistory from './components/ArenaHistory.vue'
import ModelSelectorModal from '../components/ModelSelectorModal.vue'
import ProviderLogo from '../components/ProviderLogo.vue'
import { marked } from 'marked'

// Import Config Assets
import iconOpenAI from '@/assets/openai.svg'
import iconGemini from '@/assets/gemini-color.svg'
import iconClaude from '@/assets/claude-color.svg'
import iconDeepSeek from '@/assets/deepseek-color.svg'
import iconQwen from '@/assets/qwen-color.svg'
import iconZhipu from '@/assets/zhipu-color.svg'
import iconHunyuan from '@/assets/hunyuan-color.svg'
import iconCloudflare from '@/assets/cloudflare-color.svg'
import iconGithub from '@/assets/githubcopilot.svg'
import iconMoonshot from '@/assets/moonshot.svg'

// 配置 marked
marked.setOptions({
  breaks: true,
  gfm: true
})

const route = useRoute()

// Provider Logos Map
const logoMap: Record<string, string> = {
  openai: iconOpenAI,
  google: iconGemini,
  claude: iconClaude,
  deepseek: iconDeepSeek,
  aliyun: iconQwen,
  zhipu: iconZhipu,
  hunyuan: iconHunyuan,
  cloudflare: iconCloudflare,
  github: iconGithub,
  moonshot: iconMoonshot
}

// 状态
const prompts = ref<Prompt[]>([])
const versions = ref<PromptVersion[]>([])
const models = ref<AvailableModelInfo[]>([])
const modelMap = ref<Map<string, AvailableModelInfo>>(new Map())

// 1v1 Selection
const selectedPromptId = ref<number | undefined>(undefined)
const selectedVersionId = ref<number | undefined>(undefined)
const modelA = ref<string | undefined>(undefined)
const modelB = ref<string | undefined>(undefined)
const variables = ref<Record<string, string>>({})

const isCompeting = ref(false)
const eventSource = ref<EventSource | null>(null)

// Model Modal State
const showModelModal = ref(false)
const modelSelectorTarget = ref<'A' | 'B' | null>(null)

const openModelSelector = (target: 'A' | 'B') => {
  modelSelectorTarget.value = target
  showModelModal.value = true
}

const handleModelSelect = (modelId: string) => {
  if (modelSelectorTarget.value === 'A') {
    modelA.value = modelId
    // Prevent same model
    if (modelA.value === modelB.value) {
      modelB.value = undefined
    }
  } else if (modelSelectorTarget.value === 'B') {
    modelB.value = modelId
    if (modelB.value === modelA.value) {
      modelA.value = undefined
    }
  }
}

const currentSelectionId = computed(() => {
  if (modelSelectorTarget.value === 'A') return modelA.value
  if (modelSelectorTarget.value === 'B') return modelB.value
  return undefined
})

// Output State
interface ModelOutput {
  content: string
  reasoning: string  // 深度思考内容
  finished: boolean
  error?: string
  time?: number
}
const outputA = ref<ModelOutput>({ content: '', reasoning: '', finished: false })
const outputB = ref<ModelOutput>({ content: '', reasoning: '', finished: false })

// Voting State
const hasVoted = ref(false)
const votedWinner = ref<string | 'tie' | null>(null)

// 排行榜
const leaderboardVisible = ref(false)
const leaderboardData = ref<LeaderboardItem[]>([])
const leaderboardLoading = ref(false)
const historyRef = ref()
const isViewingHistory = ref(false)

const openHistory = () => {
  historyRef.value?.show()
}

// Restore Session
// Fix: Accept full history item to know who won
const restoreSession = async (historyItem: any) => {
  const sessionId = historyItem.sessionId
  // console.log('Restoring session:', sessionId)
  try {
    const { getSessionDetail } = await import('../api/arena')
    const res = await getSessionDetail(sessionId)
    if (res.code === 200) {
      const detail = res.data
      currentSessionId.value = detail.id

      // Stop current competition if any
      if (isCompeting.value) stopCompete()

      // Set Mode
      isViewingHistory.value = true
      isCompeting.value = false
      hasVoted.value = true

      // Restore Vote Winner (from History Item)
      // Note: historyItem.winnerModel is the displayName(modified by backend) or modelId?
      // Backend getUserVotes returns DISPLAY NAME if mapped, or short name.
      // But submitVote expects modelId.
      // Here we just want to DISPLAY it.
      // Wait, getLeaderboard/getUserVotes DTO might return transformed names.
      // But locally we use modelId for `votedWinner` to match `getModelDisplayName`.
      // The history item from `getUserVotes` might have overwritten names. 
      // Let's rely on model matching if possible, OR just display what we have.
      // Since `getUserVotes` already formatted names, we can force display.
      // But `votedWinner` is used as key in `getModelDisplayName`.
      // Let's try to map back or just set it if it matches modelA/B.

      // Populate Data
      // Fix 1: Load Prompts and Versions to show correct Selector State
      if ((detail as any).promptId) {
        selectedPromptId.value = (detail as any).promptId
        await loadVersions((detail as any).promptId)
      }
      selectedVersionId.value = detail.promptVersionId
      variables.value = detail.variables

      // Models
      if (detail.models && detail.models.length >= 2) {
        modelA.value = detail.models[0]
        modelB.value = detail.models[1]
      }

      // Restore winner selection for display
      // historyItem.winnerModel might be "GPT-4o", but modelA might be "openai:gpt-4o"
      // If names don't match exactly, we might fail to show "You voted for X" correctly using computed.
      // But `getModelDisplayName` tries to match ID.
      // For now, let's treat historyItem.winnerModel as the truth to show.
      // We will override votedWinner logic slightly or just set it.
      // Since frontend `getUserVotes` maps IDs to display names (in Backend Service Lines 499-500), 
      // historyItem.winnerModel IS the display name.
      // So we can't use it as ID.
      // We need to determine which model ID won.
      // We know modelA and modelB IDs. We can check which one's display name matches historyItem.winnerModel?
      // Or simply: checking latency? No.
      // The backend `getUserVotes` returns display names, losing IDs. That was a design flaw in Phase 1.
      // However, we can deduce it if we assume the user clicked the item where winner was X.
      // For now, to quick fix UI: 
      // We will introduce `votedWinnerDisplayName` to explicitly show string.
      votedWinner.value = 'HISTORY_RESTORED' // Special flag?
      // Or just use a new ref for display.

      // Let's hack: find which model ID in (modelA, modelB) roughly matches the winner string?
      // Or simply add `votedWinnerDisplayName` ref.

      // Outputs
      const resA = detail.results.find((r: any) => r.modelId === modelA.value)
      const resB = detail.results.find((r: any) => r.modelId === modelB.value)

      outputA.value = {
        content: resA?.content || '',
        reasoning: '',
        finished: true,
        error: resA?.error,
        time: resA?.latencyMs
      }

      outputB.value = {
        content: resB?.content || '',
        reasoning: '',
        finished: true,
        error: resB?.error,
        time: resB?.latencyMs
      }

      // Restore winner display
      // Since historyItem.winnerModel is the name, let's use it directly if possible.
      // We need to modify the template to use a separate ref for history winner name.
      restoreWinnerName.value = historyItem.winnerModel

      // Close history
      historyRef.value?.hide()
    }
  } catch (e) {
    console.error(e)
    message.error('Failed to load session details')
  }
}
const restoreWinnerName = ref<string | null>(null)

// Computed
const currentVersionContent = computed(() => {
  if (!selectedVersionId.value) return ''
  const v = versions.value.find(ver => ver.id === selectedVersionId.value)
  return v?.content || ''
})

const compiledPrompt = computed(() => {
  let content = currentVersionContent.value
  if (!content) return ''

  // Replace variables
  for (const [key, val] of Object.entries(variables.value)) {
    // Simple replacement for {{key}}
    const regex = new RegExp(`\\{\\{\\s*${key}\\s*\\}\\}`, 'g')
    const replacement = val ? val : `{{${key}}}`
    content = content.replace(regex, replacement)
  }
  return content
})


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
      const map = new Map<string, AvailableModelInfo>()
      res.data.forEach(m => map.set(m.modelId, m))
      modelMap.value = map

      // Auto-select first two if available
      if (models.value.length >= 2 && models.value[0] && models.value[1]) {
        modelA.value = models.value[0].modelId
        modelB.value = models.value[1].modelId
      }
    }
  } catch (error) {
    console.error('加载模型失败:', error)
  }
}

// 解析变量
const parseVariables = (content: string) => {
  // Use non-greedy match for content inside {{ }} to support UTF-8 chars (e.g. Chinese)
  const regex = /\{\{(.*?)\}\}/g
  const matches = content.matchAll(regex)
  const vars: Record<string, string> = {}
  for (const match of matches) {
    const varName = match[1]?.trim()
    if (varName) vars[varName] = ''
  }
  variables.value = vars
}

const onPromptChange = (promptId: number) => {
  selectedPromptId.value = promptId
  selectedVersionId.value = undefined
  versions.value = []
  loadVersions(promptId)
}

const onVersionChange = (versionId: number) => {
  const version = versions.value.find(v => v.id === versionId)
  if (version) parseVariables(version.content)
}

// Render Markdown
const renderMarkdown = (content: string) => {
  if (!content) return ''
  try {
    return marked(content) as string
  } catch (e) {
    return content
  }
}

// Start Battle
const currentSessionId = ref<number | null>(null)

// Start Battle
const startCompete = () => {
  if (!selectedVersionId.value) return message.warning('请选择 Prompt 版本')
  if (!modelA.value || !modelB.value) return message.warning('请选择两个对比模型')
  if (modelA.value === modelB.value) return message.warning('请选择两个不同的模型进行对比')

  // Reset State
  outputA.value = { content: '', reasoning: '', finished: false }
  outputB.value = { content: '', reasoning: '', finished: false }
  hasVoted.value = false
  votedWinner.value = null
  isCompeting.value = true
  isViewingHistory.value = false
  currentSessionId.value = null
  restoreWinnerName.value = null

  const token = localStorage.getItem('token')
  const baseUrl = import.meta.env.VITE_API_BASE_URL || '/api'

  fetch(`${baseUrl}/arena/compete`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      'Authorization': token ? `Bearer ${token}` : ''
    },
    body: JSON.stringify({
      promptVersionId: selectedVersionId.value,
      variables: variables.value,
      modelIds: [modelA.value, modelB.value]
    })
  }).then(response => {
    const reader = response.body?.getReader()
    const decoder = new TextDecoder()
    let buffer = ''

    const read = () => {
      reader?.read().then(({ done, value }) => {
        if (done) {
          isCompeting.value = false
          return
        }
        buffer += decoder.decode(value, { stream: true })
        const lines = buffer.split('\n')
        buffer = lines.pop() || ''

        for (const line of lines) {
          const trimmedLine = line.trim()
          if (trimmedLine.startsWith('data:')) {
            try {
              const jsonStr = trimmedLine.substring(5).trim()
              if (jsonStr) {
                // Handle different event types including 'message' wrapper from SSE
                // Note: Standard SSE sends "data: { ... }". Our backend sends "data: {...}"
                // If backend wrapper sends .name("message"), the browser EventSource usually handles it.
                // But here we are using fetch + reader, so we parse raw lines.
                // The backend sends: data: {"type":"session", ...}
                // or data: {"modelId":..., "type":"content", ...}

                const data = JSON.parse(jsonStr) as ArenaEvent
                handleArenaEvent(data)
              }
            } catch (e) {
              console.warn('SSE Parse Error', e)
            }
          }
        }
        read()
      })
    }
    read()
  }).catch(error => {
    console.error('SSE Error:', error)
    message.error('连接失败')
    isCompeting.value = false
  })
}

const handleArenaEvent = (event: ArenaEvent) => {
  if (event.type === 'session') {
    if (event.sessionId) {
      currentSessionId.value = event.sessionId
      console.log('Captured Session ID:', event.sessionId)
    }
    return
  }

  let target = null
  if (event.modelId === modelA.value) target = outputA.value
  else if (event.modelId === modelB.value) target = outputB.value

  if (!target) return

  if (event.type === 'content') {
    target.content += event.content || ''
  } else if (event.type === 'reasoning') {
    // 深度思考内容
    target.reasoning += event.content || ''
  } else if (event.type === 'finish') {
    target.finished = true
  } else if (event.type === 'error') {
    target.error = event.content
    target.finished = true
  }
}

const stopCompete = () => {
  eventSource.value?.close()
  isCompeting.value = false
}

// Voting
const handleVote = async (choice: 'A' | 'B' | 'Tie') => {
  if (hasVoted.value) return

  let winner = ''
  let loser = ''

  if (choice === 'A') {
    winner = modelA.value!
    loser = modelB.value!
    votedWinner.value = modelA.value!
  } else if (choice === 'B') {
    winner = modelB.value!
    loser = modelA.value!
    votedWinner.value = modelB.value!
  } else {
    hasVoted.value = true
    votedWinner.value = 'tie'
    message.success('感谢投票！(平局)')
    return
  }

  try {
    // Send sessionId if available
    await submitVote({
      sessionId: currentSessionId.value || undefined,
      winnerModel: winner,
      loserModel: loser
    })
    hasVoted.value = true
    message.success('投票成功！')
  } catch (e) {
    message.error('投票失败')
  }
}

// Helpers
const getModelDisplayName = (modelId: string) => {
  const info = modelMap.value.get(modelId)
  return info?.displayName || modelId
}

const getProviderLogo = (modelId: string) => {
  const info = modelMap.value.get(modelId)
  const provider = info?.provider || modelId.split(':')[0] || modelId
  return logoMap[provider.toLowerCase()] || ''
}

// Leaderboard
const openLeaderboard = async () => {
  leaderboardVisible.value = true
  leaderboardLoading.value = true
  try {
    const res = await getLeaderboard()
    if (res.code === 200) leaderboardData.value = res.data
  } catch (error) {
    message.error('加载失败')
  } finally {
    leaderboardLoading.value = false
  }
}

const swapModels = () => {
  const temp = modelA.value
  modelA.value = modelB.value
  modelB.value = temp
}

onMounted(() => {
  loadPrompts()
  loadModels()
  if (route.query.promptId) {
    selectedPromptId.value = Number(route.query.promptId)
    loadVersions(Number(route.query.promptId))
  }
})
onUnmounted(() => stopCompete())
</script>

<template>
  <div class="arena-container">
    <main class="main-content">

      <!-- Top Bar: Config & Controls -->
      <div class="control-bar-wrapper">
        <div class="control-bar">
          <div class="config-section">
            <div class="prompt-select-group">
              <a-select v-model:value="selectedPromptId" placeholder="Select Prompt" style="width: 220px"
                @change="onPromptChange" class="custom-select" :bordered="false" popupClassName="custom-dropdown">
                <a-select-option v-for="p in prompts" :key="p.id" :value="p.id">{{ p.name }}</a-select-option>
              </a-select>

              <a-select v-model:value="selectedVersionId" placeholder="Version" style="width: 100px"
                :disabled="!selectedPromptId" @change="onVersionChange" class="custom-select" :bordered="false"
                popupClassName="custom-dropdown">
                <a-select-option v-for="v in versions" :key="v.id" :value="v.id">v{{ v.versionNumber
                }}</a-select-option>
              </a-select>
            </div>

            <div class="divider-vertical" v-if="Object.keys(variables).length > 0"></div>

            <div class="vars-group" v-if="Object.keys(variables).length > 0">
              <div v-for="(_val, key) in variables" :key="key" class="var-input-wrapper">
                <input v-model="variables[key]" :placeholder="key" />
              </div>
            </div>
          </div>

          <div class="actions-group">
            <button class="btn-primary" @click="startCompete" :disabled="isCompeting || !selectedVersionId"
              title="Start Battle">
              <span v-if="!isCompeting">
                <PlayCircleOutlined />
              </span>
              <span v-else>
                <StopOutlined />
              </span>
            </button>
            <button class="btn-ghost" @click="openHistory" title="Vote History" style="margin-right: 8px">
              <HistoryOutlined />
            </button>
            <button class="btn-ghost" @click="openLeaderboard" title="Leaderboard">
              <TrophyOutlined />
            </button>
          </div>
        </div>
      </div>

      <!-- Prompt Preview Inline -->
      <div v-if="compiledPrompt" class="prompt-preview-section">
        <div class="section-label">SYSTEM PROMPT</div>
        <div class="prompt-content-box">
          {{ compiledPrompt }}
        </div>
      </div>

      <!-- Battle Arena -->
      <div class="battle-ground">

        <!-- MODEL A COLUMN -->
        <div class="model-column">
          <div class="model-header">
            <div class="corner-label">Model A</div>
            <div class="model-trigger-btn" @click="openModelSelector('A')">
              <template v-if="modelA">
                <ProviderLogo :providerId="modelMap.get(modelA)?.provider || modelA.split(':')[0] || ''" :size="20" />
                <span class="trigger-text">{{ getModelDisplayName(modelA) }}</span>
              </template>
              <span v-else class="trigger-placeholder">Select Model</span>
              <DownOutlined class="trigger-arrow" />
            </div>
          </div>

          <div class="chat-area">
            <div v-if="!outputA.content && !outputA.reasoning && !outputA.error && !isCompeting" class="placeholder-state">
              <div class="placeholder-icon">
                <FireOutlined style="color: #9ca3af; opacity: 0.2" />
              </div>
            </div>
            <div v-else>
              <!-- 深度思考 -->
              <div v-if="outputA.reasoning" class="reasoning-block">
                <div class="reasoning-header">💭 思考过程</div>
                <div class="reasoning-text">{{ outputA.reasoning }}</div>
              </div>
              <!-- 正式回答 -->
              <div class="message-bubble model-msg">
                <div class="msg-content markdown-body" v-html="renderMarkdown(outputA.content)"></div>
                <div v-if="outputA.error" class="error-text">Result Error: {{ outputA.error }}</div>
                <div v-if="!outputA.finished && isCompeting" class="typing-cursor"></div>
              </div>
            </div>
          </div>
        </div>

        <!-- VS Divider -->
        <div class="vs-divider">
          <div class="line"></div>
          <div class="swap-btn" @click="swapModels" title="Switch Sides">
            <SwapOutlined />
          </div>
        </div>

        <!-- MODEL B COLUMN -->
        <div class="model-column">
          <div class="model-header">
            <div class="corner-label">Model B</div>
            <div class="model-trigger-btn" @click="openModelSelector('B')">
              <template v-if="modelB">
                <ProviderLogo :providerId="modelMap.get(modelB)?.provider || modelB.split(':')[0] || ''" :size="20" />
                <span class="trigger-text">{{ getModelDisplayName(modelB) }}</span>
              </template>
              <span v-else class="trigger-placeholder">Select Model</span>
              <DownOutlined class="trigger-arrow" />
            </div>
          </div>

          <div class="chat-area">
            <div v-if="!outputB.content && !outputB.reasoning && !outputB.error && !isCompeting" class="placeholder-state">
              <div class="placeholder-icon">
                <ThunderboltOutlined style="color: #9ca3af; opacity: 0.2" />
              </div>
            </div>
            <div v-else>
              <!-- 深度思考 -->
              <div v-if="outputB.reasoning" class="reasoning-block">
                <div class="reasoning-header">💭 思考过程</div>
                <div class="reasoning-text">{{ outputB.reasoning }}</div>
              </div>
              <!-- 正式回答 -->
              <div class="message-bubble model-msg">
                <div class="msg-content markdown-body" v-html="renderMarkdown(outputB.content)"></div>
                <div v-if="outputB.error" class="error-text">Result Error: {{ outputB.error }}</div>
                <div v-if="!outputB.finished && isCompeting" class="typing-cursor"></div>
              </div>
            </div>
          </div>
        </div>

      </div>

      <!-- Voting Floating Bar -->
      <div class="voting-bar" v-if="!isCompeting && outputA.finished && outputB.finished">
        <div class="vote-inner">
          <span class="vote-label">Choose winner:</span>
          <div v-if="!hasVoted" class="vote-actions">
            <button class="v-btn v-a" @click="handleVote('A')">👈 {{ getModelDisplayName(modelA!) }}</button>
            <button class="v-btn v-tie" @click="handleVote('Tie')">Tie</button>
            <button class="v-btn v-b" @click="handleVote('B')">{{ getModelDisplayName(modelB!) }} 👉</button>
          </div>
          <div class="vote-result" v-else>
            <span v-if="votedWinner === 'tie'">🤝 It's a Tie!</span>
            <span v-else>🎉 You voted for: <strong>{{ restoreWinnerName || getModelDisplayName(votedWinner!)
            }}</strong></span>
          </div>
        </div>
      </div>

    </main>

    <!-- Leaderboard Modal -->
    <a-modal v-model:open="leaderboardVisible" title="🏆 Leaderboard" :footer="null" width="600px">
      <a-table :dataSource="leaderboardData" :loading="leaderboardLoading" :pagination="false" rowKey="modelId">
        <a-table-column title="Rank" width="80px">
          <template #default="{ index }">
            <span class="rank-badge" :class="'rank-' + (index + 1)">{{ index + 1 }}</span>
          </template>
        </a-table-column>
        <a-table-column title="Model" dataIndex="modelId">
          <template #default="{ text }">
            <div class="lb-model-cell">
              <img v-if="getProviderLogo(text)" :src="getProviderLogo(text)" class="lb-icon" />
              <span>{{ getModelDisplayName(text) }}</span>
            </div>
          </template>
        </a-table-column>
        <a-table-column title="Battles" dataIndex="total" align="center" :width="100">
          <template #default="{ text }">
            <span style="color: #6b7280; font-size: 13px;">{{ text }}</span>
          </template>
        </a-table-column>
        <a-table-column title="Win Rate" dataIndex="winRate" align="right">
          <template #default="{ text }">
            <span class="win-rate">{{ text }}%</span>
          </template>
        </a-table-column>
      </a-table>
    </a-modal>

    <!-- History Drawer -->
    <ArenaHistory ref="historyRef" @restore="restoreSession" />

    <ModelSelectorModal v-model:open="showModelModal" :models="models" :selectedModelId="currentSelectionId"
      @select="handleModelSelect" />
  </div>
</template>

<style scoped>
.arena-container {
  min-height: 100vh;
  background: #f9fafb;
  color: #1f2937;
  font-family: 'Inter', -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif;
  display: flex;
  flex-direction: column;
}

.main-content {
  flex: 1;
  display: flex;
  flex-direction: column;
  max-width: 1600px;
  margin: 0 auto;
  width: 100%;
  padding: 24px;
  height: 100vh;
  box-sizing: border-box;
}

/* Control Bar */
.control-bar-wrapper {
  margin-bottom: 20px;
}

.control-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: #fff;
  padding: 8px 16px;
  /* Smaller padding for tighter look */
  border-radius: 12px;
  border: 1px solid #e5e7eb;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.02);
  flex-wrap: wrap;
  gap: 16px;
}

.config-section {
  display: flex;
  align-items: center;
  gap: 16px;
  flex: 1;
  flex-wrap: wrap;
}

.prompt-select-group {
  display: flex;
  align-items: center;
  gap: 10px;
}

.divider-vertical {
  width: 1px;
  height: 24px;
  background: #e5e7eb;
}

.vars-group {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.var-input-wrapper input {
  padding: 6px 12px;
  border: 1px solid #e5e7eb;
  background: #f9fafb;
  border-radius: 6px;
  font-size: 13px;
  outline: none;
  transition: all 0.2s;
  min-width: 120px;
}

.var-input-wrapper input:focus {
  border-color: #d1d5db;
  background: #fff;
  box-shadow: 0 0 0 2px rgba(0, 0, 0, 0.02);
}

.actions-group {
  display: flex;
  gap: 8px;
  margin-left: auto;
}

.btn-primary {
  background: #18181b;
  color: #fff;
  border: none;
  width: 36px;
  /* Smaller buttons */
  height: 36px;
  border-radius: 8px;
  font-weight: 500;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s;
  font-size: 14px;
  box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.1);
}

.btn-primary:hover:not(:disabled) {
  background: #27272a;
  transform: translateY(-1px);
}

.btn-primary:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.btn-ghost {
  background: #fff;
  border: 1px solid #e5e7eb;
  color: #374151;
  width: 36px;
  height: 36px;
  border-radius: 8px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s;
  font-size: 14px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.btn-ghost:hover {
  background: #f9fafb;
  border-color: #d1d5db;
}


/* Prompt Preview Section */
.prompt-preview-section {
  margin-bottom: 20px;
  background: transparent;
  border-radius: 12px;
  overflow: hidden;
  animation: fadeIn 0.3s ease;
}

.section-label {
  font-size: 11px;
  font-weight: 700;
  color: #9ca3af;
  margin-bottom: 8px;
  padding-left: 4px;
  letter-spacing: 0.5px;
}

.prompt-content-box {
  background: #fff;
  border: 1px solid #e5e7eb;
  border-radius: 12px;
  padding: 16px 20px;
  font-size: 14px;
  line-height: 1.6;
  color: #4b5563;
  white-space: pre-wrap;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.02);
  /* Max height for long prompts */
  max-height: 200px;
  overflow-y: auto;
}

/* Battle Ground Card */
.battle-ground {
  display: grid;
  grid-template-columns: 1fr auto 1fr;
  gap: 0;
  flex: 1;
  min-height: 0;
  background: #fff;
  border-radius: 16px;
  overflow: hidden;
  border: 1px solid #e5e7eb;
  box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.05);
}

.model-column {
  display: flex;
  flex-direction: column;
  background: #fff;
  position: relative;
  overflow: hidden;
}

.model-header {
  padding: 16px 24px;
  border-bottom: 1px solid #f3f4f6;
  height: auto;
  /* Let it grow */
  min-height: 72px;
  display: flex;
  flex-direction: column;
  justify-content: center;
  gap: 4px;
}

.corner-label {
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 0.5px;
  text-transform: uppercase;
  color: #9ca3af;
}

/* --- CUSTOM SELECT STYLES (ALIGNMENT FIX) --- */
:deep(.custom-select .ant-select-selector),
:deep(.model-hero-select .ant-select-selector) {
  background-color: #fff !important;
  border: 1px solid #e5e7eb !important;
  border-radius: 8px !important;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.02) !important;
  height: 36px !important;
  /* Force Flex alignment */
  display: flex !important;
  align-items: center !important;
  padding: 0 12px !important;
  transition: all 0.2s ease;
}

:deep(.custom-select .ant-select-selector .ant-select-selection-item),
:deep(.model-hero-select .ant-select-selector .ant-select-selection-item) {
  display: flex !important;
  align-items: center !important;
  /* Reset absolute positioning often found in AntD */
  position: static !important;
  line-height: normal !important;
  transform: none !important;
  margin: 0 !important;
  top: auto !important;
  bottom: auto !important;
}


:deep(.model-hero-select .ant-select-selector) {
  border-color: transparent !important;
  box-shadow: none !important;
  background-color: transparent !important;
  padding-left: 0 !important;
  font-size: 16px;
  font-weight: 600;
  height: 40px !important;
}

:deep(.model-hero-select:hover .ant-select-selector) {
  background-color: #f9fafb !important;
}

:deep(.ant-select-selector:hover) {
  border-color: #d1d5db !important;
}

:deep(.ant-select-focused .ant-select-selector) {
  border-color: #000 !important;
  box-shadow: 0 0 0 2px rgba(0, 0, 0, 0.05) !important;
}

/* Dropdown Menu Styling */
:global(.custom-dropdown .ant-select-dropdown) {
  border-radius: 12px !important;
  padding: 6px !important;
  box-shadow: 0 10px 15px -3px rgba(0, 0, 0, 0.1) !important;
  border: 1px solid #f3f4f6;
}

.option-content {
  display: flex;
  align-items: center;
  gap: 10px;
}

.option-icon {
  width: 20px;
  height: 20px;
  object-fit: contain;
}

/* VS Divider */
.vs-divider {
  width: 1px;
  background: #f3f4f6;
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 10;
}

.swap-btn {
  position: absolute;
  top: 50%;
  background: #fff;
  border: 1px solid #e5e7eb;
  width: 32px;
  height: 32px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #6b7280;
  cursor: pointer;
  transition: all 0.2s;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.05);
}

.swap-btn:hover {
  color: #000;
  transform: scale(1.1);
  box-shadow: 0 4px 8px rgba(0, 0, 0, 0.1);
}

/* Chat Area */
.chat-area {
  flex: 1;
  padding: 32px;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
}

.placeholder-state {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  opacity: 0.1;
}

.placeholder-icon {
  font-size: 48px;
}

.message-bubble {
  background: #fff;
  font-size: 16px;
  line-height: 1.7;
  color: #374151;
}

.typing-cursor {
  display: inline-block;
  width: 8px;
  height: 18px;
  background: #000;
  margin-left: 4px;
  border-radius: 1px;
  animation: blink 1s infinite;
}

@keyframes blink {
  50% {
    opacity: 0;
  }
}

/* Voting Floating Bar */
.voting-bar {
  position: absolute;
  bottom: 40px;
  left: 50%;
  transform: translateX(-50%);
  background: #fff;
  padding: 8px 16px;
  border-radius: 100px;
  box-shadow: 0 10px 25px -5px rgba(0, 0, 0, 0.1), 0 8px 10px -6px rgba(0, 0, 0, 0.1);
  border: 1px solid #e5e7eb;
  animation: floatUp 0.4s cubic-bezier(0.16, 1, 0.3, 1);
  z-index: 50;
}

@keyframes floatUp {
  from {
    transform: translate(-50%, 20px);
    opacity: 0;
  }

  to {
    transform: translate(-50%, 0);
    opacity: 1;
  }
}

.vote-inner {
  display: flex;
  align-items: center;
  gap: 16px;
}

.vote-label {
  font-size: 13px;
  font-weight: 600;
  color: #6b7280;
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.vote-actions {
  display: flex;
  gap: 8px;
}

.v-btn {
  padding: 8px 16px;
  border-radius: 20px;
  border: 1px solid transparent;
  font-weight: 600;
  font-size: 13px;
  cursor: pointer;
  transition: all 0.2s;
}

.v-a {
  background: #fee2e2;
  color: #991b1b;
}

.v-a:hover {
  background: #fecaca;
}

.v-b {
  background: #dbeafe;
  color: #1e40af;
}

.v-b:hover {
  background: #bfdbfe;
}

.v-tie {
  background: #f3f4f6;
  color: #374151;
}

.v-tie:hover {
  background: #e5e7eb;
}

/* Leaderboard */
.rank-badge {
  display: inline-block;
  width: 24px;
  height: 24px;
  background: #f3f4f6;
  border-radius: 6px;
  text-align: center;
  line-height: 24px;
  font-weight: 600;
  font-size: 12px;
  color: #666;
}

.rank-1 {
  background: #fffbeb;
  color: #b45309;
}

.rank-2 {
  background: #f8fafc;
  color: #475569;
}

.rank-3 {
  background: #fff7ed;
  color: #c2410c;
}

.lb-model-cell {
  display: flex;
  align-items: center;
  gap: 10px;
}

.lb-icon {
  width: 20px;
  height: 20px;
  object-fit: contain;
}

@keyframes fadeIn {
  from {
    opacity: 0;
    transform: translateY(-10px);
  }

  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.model-trigger-btn {
  display: flex;
  align-items: center;
  gap: 8px;
  background: #fff;
  border: 1px solid #e5e7eb;
  padding: 8px 12px;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s;
  height: 40px;
}

.model-trigger-btn:hover {
  border-color: #000;
}

.trigger-text {
  flex: 1;
  font-weight: 500;
  font-size: 14px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.trigger-placeholder {
  flex: 1;
  color: #9ca3af;
  font-size: 14px;
}

.trigger-arrow {
  font-size: 10px;
  color: #9ca3af;
}

/* Reasoning Block (Deep Thinking) */
.reasoning-block {
  margin-bottom: 12px;
  padding: 12px 16px;
  background: #f9fafb;
  border: 1px solid #e5e7eb;
  border-left: 3px solid #f59e0b;
  border-radius: 8px;
}

.reasoning-block .reasoning-header {
  font-size: 12px;
  font-weight: 600;
  color: #6b7280;
  margin-bottom: 8px;
}

.reasoning-block .reasoning-text {
  font-size: 13px;
  color: #6b7280;
  font-style: italic;
  line-height: 1.5;
  white-space: pre-wrap;
  max-height: 150px;
  overflow-y: auto;
}
</style>
