<script setup lang="ts">
import { ref, onMounted, computed, h, watch } from 'vue'
import { useRouter } from 'vue-router'
import {
  getProviders,
  getModelConfigs,
  createConfig,
  updateConfig,
  deleteConfig,
  toggleConfig,
  type ProviderInfo,
  type ModelConfig,
  type CreateConfigRequest
} from '../api/modelConfig'
import { message, Modal } from 'ant-design-vue'
import { ArrowLeftOutlined, SettingOutlined, BulbOutlined, PlusOutlined, DeleteOutlined, StopOutlined, PlayCircleOutlined, CheckCircleFilled } from '@ant-design/icons-vue'

// Import Config Assets
import iconOpenAI from '@/assets/openai.svg'
import iconGoogle from '@/assets/google-color.svg'
import iconClaude from '@/assets/claude-color.svg'
import iconDeepSeek from '@/assets/deepseek-color.svg'
import iconQwen from '@/assets/qwen-color.svg'
import iconZhipu from '@/assets/zhipu-color.svg'
import iconHunyuan from '@/assets/hunyuan-color.svg'
import iconCloudflare from '@/assets/cloudflare-color.svg'
import iconGithub from '@/assets/githubcopilot.svg'
import iconMoonshot from '@/assets/moonshot.svg'
import iconAzure from '@/assets/azureai-color.svg'
import iconBedrock from '@/assets/bedrock-color.svg'
import iconBaichuan from '@/assets/baichuan-color.svg'
import iconMinimax from '@/assets/minimax-color.svg'
import iconStepfun from '@/assets/stepfun-color.svg'
import iconSpark from '@/assets/spark-color.svg'
import iconSensenova from '@/assets/sensenova-color.svg'
import iconMistral from '@/assets/mistral-color.svg'
import iconPerplexity from '@/assets/perplexity-color.svg'
import iconGroq from '@/assets/groq.svg'
import iconCohere from '@/assets/cohere-color.svg'
import iconNovita from '@/assets/novita-color.svg'
import iconTogether from '@/assets/together-color.svg'
import iconOllama from '@/assets/ollama.svg'
import iconOpenRouter from '@/assets/openrouter.svg'

const router = useRouter()

// 状态
const loading = ref(false)
const providers = ref<ProviderInfo[]>([])
const configs = ref<ModelConfig[]>([])
const showAddDialog = ref(false)
const showEditDialog = ref(false)
const editingConfig = ref<ModelConfig | null>(null)

// 表单数据
const addForm = ref<CreateConfigRequest>({
  provider: '',
  apiKey: '',
  baseUrl: '',
  modelName: ''
})

const editForm = ref({
  apiKey: '',
  baseUrl: '',
  modelName: '',
  enabled: true
})

// Provider Logos Map
const logoMap: Record<string, string> = {
  openai: iconOpenAI,
  google: iconGoogle,
  anthropic: iconClaude,
  deepseek: iconDeepSeek,
  aliyun: iconQwen, // 兼容旧数据
  qwen: iconQwen,   // 新数据
  zhipu: iconZhipu,
  hunyuan: iconHunyuan,
  cloudflare: iconCloudflare,
  github: iconGithub,
  moonshot: iconMoonshot,
  azure: iconAzure,
  bedrock: iconBedrock,
  baichuan: iconBaichuan,
  minimax: iconMinimax,
  stepfun: iconStepfun,
  spark: iconSpark,
  sensenova: iconSensenova,
  mistral: iconMistral,
  perplexity: iconPerplexity,
  groq: iconGroq,
  cohere: iconCohere,
  novita: iconNovita,
  togetherai: iconTogether,
  ollama: iconOllama,
  openrouter: iconOpenRouter
}

// Provider Logos Component
const ProviderLogo = (props: { providerId: string, size?: number }) => {
  const size = props.size || 24
  const style = { width: `${size}px`, height: `${size}px`, objectFit: 'contain' as const }
  const pid = props.providerId?.toLowerCase()

  if (logoMap[pid]) {
    return h('img', { src: logoMap[pid], alt: pid, style })
  }

  // Fallback for providers without assets
  switch (pid) {
    default:
      // Default robot icon
      return h('svg', { viewBox: "0 0 24 24", fill: "none", stroke: "currentColor", 'stroke-width': 2, style }, [
        h('rect', { x: 3, y: 11, width: 18, height: 10, rx: 2 }),
        h('circle', { cx: 12, cy: 16, r: 2 }),
        h('path', { d: "M8.5 11V7a3.5 3.5 0 0 1 7 0v4" })
      ])
  }
}

// 计算已配置的提供商
const configuredProviders = computed(() => {
  return new Set(configs.value.map(c => c.provider))
})

// 可添加的提供商
const availableProviders = computed(() => {
  return providers.value.filter(p => !configuredProviders.value.has(p.id))
})

// 当前选中提供商的可用模型
const currentProviderModels = computed(() => {
  const provider = providers.value.find(p => p.id === addForm.value.provider)
  return provider?.models || []
})

// 编辑时当前提供商的可用模型
const editProviderModels = computed(() => {
  const provider = providers.value.find(p => p.id === editingConfig.value?.provider)
  const defaultModels = provider?.models || []

  // 如果有自动获取的模型，合并显示
  if (editingConfig.value?.availableModels) {
    try {
      const fetchedModels: string[] = JSON.parse(editingConfig.value.availableModels)
      const fetchedOptions = fetchedModels.map(id => ({
        id,
        name: id,
        description: '自动获取'
      }))

      // 合并去重 (优先使用 defaultModels 的详细信息)
      const map = new Map<string, any>()
      defaultModels.forEach(m => map.set(m.id, m))
      fetchedOptions.forEach(m => {
        if (!map.has(m.id)) {
          map.set(m.id, m)
        }
      })
      return Array.from(map.values())
    } catch (e) {
      console.error('解析 availableModels 失败', e)
    }
  }
  return defaultModels
})

// Preferences Logic
// Preferences Logic
const defaultOptimizeModel = ref<string | null>(localStorage.getItem('PF_DEFAULT_OPTIMIZE_MODEL') || null)
const showPrefDialog = ref(false)

// Compute all available models across all enabled configurations
const availableModelsForPref = computed(() => {
  const options: { label: string, value: string, provider: string, modelName: string, providerName: string }[] = []

  configs.value.filter(c => c.enabled).forEach(config => {
    const provider = providers.value.find(p => p.id === config.provider)
    const providerName = provider?.name || config.provider

    // Use default provider models
    if (provider?.models) {
      provider.models.forEach(m => {
        options.push({
          label: `${providerName} - ${m.name}`,
          value: `${config.provider}:${m.id}`, // Format: "openai:gpt-4"
          provider: config.provider,
          modelName: m.name,
          providerName: providerName
        })
      })
    }
  })
  return options
})

// Categories Logic
const activePrefProvider = ref<string>('')

const uniqueProvidersForPref = computed(() => {
  const providerIds = new Set(availableModelsForPref.value.map(m => m.provider))
  // Convert set to array and get names
  return Array.from(providerIds).map(id => {
    const p = providers.value.find(pr => pr.id === id)
    return {
      id,
      name: p ? p.name : id
    }
  }).sort((a, b) => {
    // Optional: Sort logic, e.g. openai first
    const weights: Record<string, number> = { openai: 100, anthropic: 90, google: 80 }
    const wa = weights[a.id] || 0
    const wb = weights[b.id] || 0
    return wb - wa
  })
})

const activePrefModels = computed(() => {
  if (!activePrefProvider.value && uniqueProvidersForPref.value.length > 0) {
    // If no provider selected, select first one
    return availableModelsForPref.value.filter(m => m.provider === uniqueProvidersForPref.value[0].id)
  }
  return availableModelsForPref.value.filter(m => m.provider === activePrefProvider.value)
})

// Auto-select first provider when dialog opens
watch(showPrefDialog, (val) => {
  if (val && uniqueProvidersForPref.value.length > 0 && !activePrefProvider.value) {
    activePrefProvider.value = uniqueProvidersForPref.value[0].id
  }
})

const selectedModelInfoForPref = computed(() => {
  if (!defaultOptimizeModel.value) return null
  return availableModelsForPref.value.find(m => m.value === defaultOptimizeModel.value)
})

const selectPreference = (value: string) => {
  defaultOptimizeModel.value = value
  localStorage.setItem('PF_DEFAULT_OPTIMIZE_MODEL', value)
  message.success('已保存默认优化模型')
  showPrefDialog.value = false
}

const clearPreferences = () => {
  defaultOptimizeModel.value = null
  localStorage.removeItem('PF_DEFAULT_OPTIMIZE_MODEL')
  message.info('已清除默认优化模型')
}

// 加载数据
const loadData = async () => {
  loading.value = true
  try {
    const [providersRes, configsRes] = await Promise.all([
      getProviders(),
      getModelConfigs()
    ])
    if (providersRes.code === 200) {
      providers.value = providersRes.data
    }
    if (configsRes.code === 200) {
      configs.value = configsRes.data
    }
  } catch (error) {
    console.error('加载数据失败:', error)
  } finally {
    loading.value = false
  }
}

// 获取提供商信息
const getProviderInfo = (providerId: string) => {
  return providers.value.find(p => p.id === providerId)
}

// 选择提供商时填充默认值
const onProviderSelect = (providerId: string) => {
  const provider = getProviderInfo(providerId)
  if (provider) {
    addForm.value.baseUrl = provider.defaultBaseUrl
    addForm.value.modelName = provider.defaultModel // 默认选中默认模型
  }
}

// 打开添加对话框
const openAddDialog = () => {
  addForm.value = {
    provider: '',
    apiKey: '',
    baseUrl: '',
    modelName: ''
  }
  showAddDialog.value = true
}

// 添加配置
const handleAdd = async () => {
  if (!addForm.value.provider || !addForm.value.apiKey) {
    message.warning('请填写提供商和 API Key')
    return
  }

  // 如果没有选择模型，使用默认的 (已经在 onProviderSelect 中设置了，但防万一)
  if (!addForm.value.modelName) {
    const provider = getProviderInfo(addForm.value.provider)
    if (provider) {
      addForm.value.modelName = provider.defaultModel
    }
  }

  try {
    const res = await createConfig(addForm.value)
    if (res.code === 200) {
      message.success('添加成功')
      showAddDialog.value = false
      loadData()
    } else {
      message.error(res.message || '添加失败')
    }
  } catch (error) {
    message.error('添加失败')
  }
}

// 打开编辑对话框
const openEditDialog = (config: ModelConfig) => {
  editingConfig.value = config
  editForm.value = {
    apiKey: '',  // 不显示原有的 API Key
    baseUrl: config.baseUrl || '',
    modelName: config.modelName || '',
    enabled: config.enabled
  }
  showEditDialog.value = true
}

// 更新配置
const handleUpdate = async () => {
  if (!editingConfig.value) return

  try {
    const res = await updateConfig(editingConfig.value.id, editForm.value)
    if (res.code === 200) {
      message.success('更新成功')
      showEditDialog.value = false
      loadData()
    } else {
      message.error(res.message || '更新失败')
    }
  } catch (error) {
    message.error('更新失败')
  }
}

// 切换启用状态
const handleToggle = async (config: ModelConfig) => {
  try {
    const res = await toggleConfig(config.id)
    if (res.code === 200) {
      message.success(res.data.enabled ? '已启用' : '已禁用')
      loadData()
    }
  } catch (error) {
    message.error('操作失败')
  }
}

// 刷新可用模型
const handleRefresh = async (config: ModelConfig) => {
  // 此功能已废弃，现在由后端自动处理模型列表
  message.info('系统会自动列出所有支持的模型')
}

// 删除配置
const handleDelete = (config: ModelConfig) => {
  Modal.confirm({
    title: '删除确认',
    content: `确定要删除 ${getProviderInfo(config.provider)?.name} 的配置吗？`,
    okType: 'danger',
    onOk: async () => {
      const res = await deleteConfig(config.id)
      if (res.code === 200) {
        message.success('删除成功')
        loadData()
      } else {
        message.error('删除失败')
      }
    }
  })
}

// 返回
const goBack = () => {
  router.push('/prompts')
}

onMounted(() => {
  loadData()
})
</script>

<template>
  <div class="settings-container">
    <main class="main-content">
      <!-- Minimalist Info Block -->
      <div class="info-card">
        <div class="info-icon">
          <BulbOutlined />
        </div>
        <div class="info-content">
          <h3>配置说明</h3>
          <p>在这里配置您的 AI 模型 API Key。配置后即可在竞技场、Prompt 教练和 AI 优化功能中调用对应厂商的所有模型。</p>
        </div>
      </div>

      <!-- Preferences Section (Global Settings) -->
      <div class="preferences-section">
        <div class="section-header">
          <h3>偏好设置</h3>
        </div>

        <div class="config-list">
          <!-- Default Optimize Model Card -->
          <div class="config-item-row">
            <div class="row-left">
              <div class="provider-icon-wrapper"
                :style="selectedModelInfoForPref ? {} : { background: '#f5f5f5', color: '#ccc' }">
                <ProviderLogo v-if="selectedModelInfoForPref" :providerId="selectedModelInfoForPref.provider"
                  :size="24" />
                <SettingOutlined v-else />
              </div>
              <div class="row-info">
                <span class="provider-name">默认 AI 优化模型</span>
                <span class="model-status" v-if="selectedModelInfoForPref" style="color: #4b5563;">
                  当前选择: <strong>{{ selectedModelInfoForPref.label }}</strong>
                </span>
                <span class="model-status disabled" v-else>
                  未设置 (每次询问)
                </span>
              </div>
            </div>
            <div class="row-right">
              <button class="add-btn" @click="showPrefDialog = true"
                style="background: #fff; color: #000; border: 1px solid #e5e7eb;">
                <SettingOutlined /> 配置
              </button>
              <button v-if="defaultOptimizeModel" class="icon-btn delete" @click="clearPreferences" title="清除设置">
                <DeleteOutlined />
              </button>
            </div>
          </div>
        </div>
      </div>

      <!-- Config List Section -->
      <div class="config-section">
        <div class="section-header">
          <h3>已配置的模型</h3>
          <button class="add-btn" @click="openAddDialog" :disabled="availableProviders.length === 0">
            <PlusOutlined /> 添加配置
          </button>
        </div>

        <div v-if="loading" class="loading-state">
          <a-skeleton active :paragraph="{ rows: 3 }" />
        </div>

        <div v-else-if="configs.length === 0" class="empty-state">
          <div class="empty-icon">
            <SettingOutlined />
          </div>
          <p>暂无配置，点击上方按钮添加您的第一个模型配置</p>
        </div>

        <div v-else class="config-list">
          <div v-for="config in configs" :key="config.id" class="config-item-row"
            :class="{ disabled: !config.enabled }">

            <div class="row-left">
              <div class="provider-icon-wrapper">
                <ProviderLogo :providerId="config.provider" :size="24" />
              </div>
              <div class="row-info">
                <span class="provider-name">{{ getProviderInfo(config.provider)?.name || config.provider }}</span>
                <!-- 隐藏具体模型名，因为现在支持选任意模型，这里显示状态即可 -->
                <span class="model-status" v-if="config.enabled">
                  <CheckCircleFilled class="status-icon" /> 已准备就绪
                </span>
                <span class="model-status disabled" v-else>
                  <StopOutlined class="status-icon" /> 已暂停使用
                </span>
              </div>
            </div>

            <div class="row-right">
              <div class="row-actions">
                <button class="icon-btn" @click="handleToggle(config)" :title="config.enabled ? '暂停' : '启用'">
                  <component :is="config.enabled ? 'StopOutlined' : 'PlayCircleOutlined'" />
                </button>
                <button class="icon-btn edit" @click="openEditDialog(config)" title="编辑高级配置">
                  <SettingOutlined />
                </button>
                <button class="icon-btn delete" @click="handleDelete(config)" title="删除">
                  <DeleteOutlined />
                </button>
              </div>
            </div>

          </div>
        </div>
      </div>
    </main>

    <!-- 添加对话框 -->
    <div v-if="showAddDialog" class="dialog-overlay" @click.self="showAddDialog = false">
      <div class="dialog">
        <div class="dialog-header">
          <h3>添加模型配置</h3>
          <button class="close-btn" @click="showAddDialog = false">×</button>
        </div>
        <div class="dialog-body">
          <div class="form-item">
            <label>选择提供商 <span class="required">*</span></label>
            <div class="provider-grid">
              <div v-for="p in availableProviders" :key="p.id" class="provider-option"
                :class="{ selected: addForm.provider === p.id }"
                @click="addForm.provider = p.id; onProviderSelect(p.id)">
                <ProviderLogo :providerId="p.id" :size="24" />
                <span>{{ p.name }}</span>
              </div>
            </div>
          </div>

          <template v-if="addForm.provider">
            <div class="form-item">
              <label>API Key <span class="required">*</span></label>
              <input v-model="addForm.apiKey" type="password" placeholder="请输入 API Key" class="dialog-input" />
            </div>

            <!-- 高级设置折叠 -->
            <div class="advanced-settings-toggle">
              <a-collapse ghost>
                <a-collapse-panel key="1" header="高级设置 (Base URL / 代理)">
                  <div class="form-item">
                    <label>Base URL</label>
                    <input v-model="addForm.baseUrl" type="text" placeholder="https://..." class="dialog-input" />
                    <div class="field-hint">如果您使用代理或中转服务，请在此输入。</div>
                  </div>
                  <div class="form-item">
                    <label>默认模型 (可选)</label>
                    <select v-model="addForm.modelName" class="dialog-input">
                      <option v-for="m in currentProviderModels" :key="m.id" :value="m.id">
                        {{ m.name }}
                      </option>
                    </select>
                  </div>
                </a-collapse-panel>
              </a-collapse>
            </div>
          </template>
        </div>
        <div class="dialog-footer">
          <button class="btn cancel" @click="showAddDialog = false">取消</button>
          <button class="btn primary" @click="handleAdd" :disabled="!addForm.provider || !addForm.apiKey">确认添加</button>
        </div>
      </div>
    </div>

    <!-- 编辑对话框 -->
    <div v-if="showEditDialog" class="dialog-overlay" @click.self="showEditDialog = false">
      <div class="dialog">
        <div class="dialog-header">
          <h3>配置管理 - {{ getProviderInfo(editingConfig?.provider || '')?.name }}</h3>
          <button class="close-btn" @click="showEditDialog = false">×</button>
        </div>
        <div class="dialog-body">
          <div class="form-item">
            <label>更新 API Key</label>
            <input v-model="editForm.apiKey" type="password" placeholder="如果要修改 Key，请在此输入新的 Key" class="dialog-input" />
          </div>
          <div class="form-item">
            <label>Base URL (代理地址)</label>
            <input v-model="editForm.baseUrl" type="text" placeholder="留空使用默认官方地址" class="dialog-input" />
          </div>

          <!-- 隐藏具体模型选择，因为现在更推荐动态选择，这里只作为底层默认值 -->
          <div class="form-item" style="display: none;">
            <select v-model="editForm.modelName" class="dialog-input">
              <option v-for="m in editProviderModels" :key="m.id" :value="m.id">
                {{ m.name }}
              </option>
            </select>
          </div>


          <div class="form-item switch-row">
            <span class="switch-label">启用此配置</span>
            <a-switch v-model:checked="editForm.enabled" />
          </div>
        </div>
        <div class="dialog-footer">
          <button class="btn cancel" @click="showEditDialog = false">取消</button>
          <button class="btn primary" @click="handleUpdate">保存修改</button>
        </div>
      </div>
    </div>



    <!-- Preference Selection Modal (Sidebar Layout) -->
    <div v-if="showPrefDialog" class="dialog-overlay" @click.self="showPrefDialog = false">
      <div class="dialog model-selector-dialog">
        <div class="dialog-header">
          <h3>选择默认 AI 优化模型</h3>
          <button class="close-btn" @click="showPrefDialog = false">×</button>
        </div>
        <div class="dialog-body-layout">
          <!-- Left Sidebar: Provider Categories -->
          <div class="category-sidebar">
            <div 
              v-for="provider in uniqueProvidersForPref" 
              :key="provider.id"
              class="category-item"
              :class="{ active: activePrefProvider === provider.id }"
              @click="activePrefProvider = provider.id"
            >
               <ProviderLogo :providerId="provider.id" :size="20" />
               <span class="category-name">{{ provider.name }}</span>
            </div>
          </div>

          <!-- Right Content: Model Grid -->
          <div class="model-content-area">
             <div v-if="activePrefModels.length > 0" class="provider-grid">
              <div v-for="m in activePrefModels" :key="m.value" class="provider-option"
                :class="{ selected: defaultOptimizeModel === m.value }" @click="selectPreference(m.value)">
                <ProviderLogo :providerId="m.provider" :size="32" />
                <span style="font-weight: 500;">{{ m.modelName }}</span>
                <span style="font-size: 11px; color: #999;">{{ m.providerName }}</span>
              </div>
            </div>
            <div v-else class="empty-state" style="padding: 20px;">
               <p>该厂商暂无可用模型</p>
            </div>
          </div>
        </div>
      </div>
    </div>

  </div>
</template>

<style scoped>
/* Dialog & Layout */
.model-selector-dialog {
  width: 800px; /* Wider for sidebar layout */
  max-width: 90vw;
  height: 600px;
  max-height: 85vh;
  display: flex;
  flex-direction: column;
}

.dialog-body-layout {
  display: flex;
  flex: 1;
  overflow: hidden; /* Contain scroll internally */
}

/* Sidebar */
.category-sidebar {
  width: 200px;
  background: #f9fafb;
  border-right: 1px solid var(--color-border);
  overflow-y: auto;
  padding: 12px;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.category-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 12px;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s;
  color: var(--color-text-secondary);
}

.category-item:hover {
  background: rgba(0,0,0,0.05);
  color: var(--color-text-primary);
}

.category-item.active {
  background: #fff;
  color: var(--color-primary);
  box-shadow: 0 1px 3px rgba(0,0,0,0.05);
  font-weight: 500;
}

.category-name {
  font-size: 14px;
}

/* Content Area */
.model-content-area {
  flex: 1;
  padding: 20px;
  overflow-y: auto;
  background: #fff;
}

/* Existing Styles */
.settings-container {
  min-height: 100vh;
  background: var(--color-bg-primary);
  color: var(--color-text-primary);
  padding-bottom: var(--space-8);
}

.loading-state {
  padding: var(--space-8);
  background: #fff;
  border-radius: var(--radius-lg);
  border: 1px solid var(--color-border);
}

.main-content {
  max-width: 800px;
  margin: 0 auto;
  padding: var(--space-8);
}

/* Minimalist Info Block */
.info-card {
  background: var(--color-bg-secondary);
  border-radius: var(--radius-lg);
  padding: var(--space-5);
  margin-bottom: var(--space-8);
  display: flex;
  gap: var(--space-3);
  align-items: flex-start;
}

.info-icon {
  font-size: var(--text-lg);
  color: var(--color-text-primary);
  margin-top: 2px;
}

.info-content h3 {
  margin: 0 0 var(--space-1) 0;
  font-size: var(--text-base);
  font-weight: 600;
  color: var(--color-text-primary);
}

.info-content p {
  margin: 0;
  color: var(--color-text-secondary);
  font-size: var(--text-sm);
  line-height: 1.5;
}

/* Section Header */
.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: var(--space-5);
}

.section-header h3 {
  margin: 0;
  font-size: var(--text-lg);
  font-weight: 600;
  color: var(--color-text-primary);
}

.add-btn {
  padding: 8px 16px;
  background: #000;
  border: none;
  border-radius: var(--radius-md);
  color: #fff;
  cursor: pointer;
  transition: all var(--transition-fast);
  font-size: var(--text-sm);
  font-weight: 500;
  display: flex;
  align-items: center;
  gap: 8px;
}

.add-btn:hover:not(:disabled) {
  background: #333;
}

.add-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

/* Config List (Replaces Grid) */
.config-list {
  display: flex;
  flex-direction: column;
  gap: var(--space-4);
}

.config-item-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: var(--space-5);
  background: #fff;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  transition: all var(--transition-fast);
}

.config-item-row:hover {
  border-color: var(--color-text-tertiary);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.05);
}

.config-item-row.disabled {
  opacity: 0.8;
  background: #fafafa;
}

.config-item-row.disabled .provider-name {
  color: var(--color-text-secondary);
}

.row-left {
  display: flex;
  align-items: center;
  gap: var(--space-4);
}

.provider-icon-wrapper {
  width: 44px;
  height: 44px;
  background: var(--color-bg-secondary);
  border-radius: var(--radius-md);
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--color-text-primary);
}

.row-info {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.provider-name {
  font-weight: 600;
  font-size: 15px;
  color: var(--color-text-primary);
}

.model-status {
  font-size: 12px;
  color: #10B981;
  /* Green */
  display: flex;
  align-items: center;
  gap: 4px;
}

.model-status.disabled {
  color: var(--color-text-secondary);
}

.row-right {
  display: flex;
  align-items: center;
  gap: var(--space-4);
}

.row-actions {
  display: flex;
  gap: 8px;
}

.icon-btn {
  width: 36px;
  height: 36px;
  display: flex;
  align-items: center;
  justify-content: center;
  border: 1px solid transparent;
  background: transparent;
  color: var(--color-text-tertiary);
  border-radius: var(--radius-md);
  cursor: pointer;
  transition: all var(--transition-fast);
}

.icon-btn:hover {
  background: var(--color-bg-secondary);
  color: var(--color-text-primary);
  border-color: var(--color-border);
}

.icon-btn.delete:hover {
  background: #fee2e2;
  color: #dc2626;
  border-color: #fecaca;
}

/* Empty State */
.empty-state {
  text-align: center;
  padding: 60px 20px;
  background: var(--color-bg-secondary);
  border-radius: var(--radius-lg);
  color: var(--color-text-tertiary);
  display: flex;
  flex-direction: column;
  align-items: center;
}

.empty-icon {
  font-size: 48px;
  margin-bottom: 16px;
  opacity: 0.5;
}

/* Dialog Styles */
.dialog-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.4);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
  backdrop-filter: blur(4px);
}

.dialog {
  background: #fff;
  border-radius: var(--radius-xl);
  width: 500px;
  max-width: 90vw;
  box-shadow: 0 20px 25px -5px rgba(0, 0, 0, 0.1), 0 10px 10px -5px rgba(0, 0, 0, 0.04);
  overflow: hidden;
  max-height: 90vh;
  display: flex;
  flex-direction: column;
}

.dialog-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 24px;
  border-bottom: 1px solid var(--color-border);
  background: #fff;
}

.dialog-header h3 {
  margin: 0;
  font-size: 18px;
  font-weight: 600;
}

.close-btn {
  width: 32px;
  height: 32px;
  border: none;
  background: transparent;
  color: var(--color-text-tertiary);
  font-size: 20px;
  cursor: pointer;
  border-radius: var(--radius-md);
  display: flex;
  align-items: center;
  justify-content: center;
  line-height: 1;
}

.close-btn:hover {
  background: var(--color-bg-secondary);
  color: var(--color-text-primary);
}

.dialog-body {
  padding: 24px;
  overflow-y: auto;
}

.form-item {
  margin-bottom: 20px;
}

.form-item label {
  display: block;
  margin-bottom: 8px;
  font-size: 14px;
  font-weight: 600;
  color: var(--color-text-primary);
}

.form-item label .required {
  color: #ef4444;
  margin-left: 2px;
}

.dialog-input {
  width: 100%;
  padding: 10px 12px;
  background: #fff;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  font-size: 14px;
  outline: none;
  transition: all 0.2s;
}

.dialog-input:focus {
  border-color: #000;
  box-shadow: 0 0 0 2px rgba(0, 0, 0, 0.05);
}

.field-hint {
  margin-top: 6px;
  font-size: 12px;
  color: var(--color-text-secondary);
}

/* Provider Grid */
.provider-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(100px, 1fr));
  gap: 12px;
}

.provider-option {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 12px 8px;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  cursor: pointer;
  transition: all 0.2s;
}

.provider-option:hover {
  background: var(--color-bg-secondary);
  border-color: var(--color-text-tertiary);
}

.provider-option.selected {
  border-color: #000;
  background: #fdfdfd;
  box-shadow: 0 0 0 1px #000 inset;
}

.provider-option span {
  font-size: 12px;
  text-align: center;
  line-height: 1.2;
  color: var(--color-text-secondary);
}

.provider-option.selected span {
  color: #000;
  font-weight: 500;
}


.dialog-footer {
  padding: 16px 24px;
  border-top: 1px solid var(--color-border);
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  background: #f9fafe;
}

.btn {
  padding: 8px 20px;
  border-radius: var(--radius-md);
  font-size: 14px;
  cursor: pointer;
  font-weight: 500;
  transition: all 0.2s;
  border: 1px solid transparent;
}

.btn.cancel {
  background: white;
  border-color: var(--color-border);
  color: var(--color-text-primary);
}

.btn.cancel:hover {
  background: var(--color-bg-secondary);
  border-color: var(--color-text-tertiary);
}

.btn.primary {
  background: #000;
  color: #fff;
}

.btn.primary:hover:not(:disabled) {
  background: #333;
  transform: translateY(-1px);
}

.btn.primary:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

/* Switch Styles */
.switch-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.switch-label {
  font-size: 14px;
  font-weight: 500;
}
</style>
