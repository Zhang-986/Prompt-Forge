<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
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
import { ArrowLeftOutlined, SettingOutlined, BulbOutlined, PlusOutlined, DeleteOutlined, StopOutlined, PlayCircleOutlined } from '@ant-design/icons-vue'

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

// 获取提供商图标 - 返回空字符串，使用组件渲染图标
const getProviderIcon = (_providerId: string) => {
  return '' // 不再使用 emoji
}

// 选择提供商时填充默认值
const onProviderSelect = (providerId: string) => {
  const provider = getProviderInfo(providerId)
  if (provider) {
    addForm.value.baseUrl = provider.defaultBaseUrl
    addForm.value.modelName = provider.defaultModel
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
    message.warning('请填写必填项')
    return
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
  const hide = message.loading('正在获取模型列表...', 0)
  try {
    const res = await refreshModelConfig(config.id)
    if (res.code === 200) {
      message.success(`成功获取 ${res.data.length} 个模型`)
      // 更新本地数据
      config.availableModels = JSON.stringify(res.data)
    } else {
      message.error(res.message || '获取失败')
    }
  } catch (e) {
    message.error('获取失败，请检查 Base URL 和 API Key')
  } finally {
    hide()
  }
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
    <!-- Header Removed -->

    <main class="main-content">
      <!-- Minimalist Info Block -->
      <div class="info-card">
        <div class="info-icon">
          <BulbOutlined />
        </div>
        <div class="info-content">
          <h3>配置说明</h3>
          <p>在这里配置您的 AI 模型 API Key。配置后将在竞技场中使用您的配置调用模型。</p>
          <p>未配置的提供商将尝试使用系统默认配置（如果可用）。</p>
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
          <p>暂无配置，点击上方按钮添加您的第一个模型配置</p>
        </div>

        <div v-else class="config-list">
          <div v-for="config in configs" :key="config.id" class="config-item-row"
            :class="{ disabled: !config.enabled }">

            <div class="row-left">
              <div class="provider-icon-wrapper">
                {{ getProviderIcon(config.provider) || '🤖' }}
              </div>
              <div class="row-info">
                <span class="provider-name">{{ getProviderInfo(config.provider)?.name || config.provider }}</span>
                <span class="model-name">
                  {{ config.modelName || getProviderInfo(config.provider)?.defaultModel }}
                </span>
              </div>
            </div>

            <div class="row-right">
              <div class="status-badge" :class="{ enabled: config.enabled, disabled: !config.enabled }">
                {{ config.enabled ? '已启用' : '已禁用' }}
              </div>

              <div class="row-actions">
                <button class="icon-btn" @click="handleToggle(config)" :title="config.enabled ? '禁用' : '启用'">
                  <component :is="config.enabled ? 'StopOutlined' : 'PlayCircleOutlined'" />
                </button>
                <button class="icon-btn edit" @click="openEditDialog(config)" title="编辑">
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
            <select v-model="addForm.provider" @change="onProviderSelect(addForm.provider)">
              <option value="" disabled>请选择提供商</option>
              <option v-for="p in availableProviders" :key="p.id" :value="p.id">
                {{ getProviderIcon(p.id) }} {{ p.name }}
              </option>
            </select>
          </div>
          <div class="form-item">
            <label>API Key <span class="required">*</span></label>
            <input v-model="addForm.apiKey" type="password" placeholder="请输入 API Key" />
          </div>
          <div class="form-item">
            <label>Base URL</label>
            <input v-model="addForm.baseUrl" type="text" placeholder="留空使用默认值" />
          </div>
          <div class="form-item">
            <label>模型选择</label>
            <select v-model="addForm.modelName" :disabled="!addForm.provider">
              <option value="">使用默认模型</option>
              <option v-for="m in currentProviderModels" :key="m.id" :value="m.id">
                {{ m.name }} - {{ m.description }}
              </option>
            </select>
          </div>
        </div>
        <div class="dialog-footer">
          <button class="btn cancel" @click="showAddDialog = false">取消</button>
          <button class="btn primary" @click="handleAdd">确认添加</button>
        </div>
      </div>
    </div>

    <!-- 编辑对话框 -->
    <div v-if="showEditDialog" class="dialog-overlay" @click.self="showEditDialog = false">
      <div class="dialog">
        <div class="dialog-header">
          <h3>编辑配置 - {{ getProviderInfo(editingConfig?.provider || '')?.name }}</h3>
          <button class="close-btn" @click="showEditDialog = false">×</button>
        </div>
        <div class="dialog-body">
          <div class="form-item">
            <label>API Key</label>
            <input v-model="editForm.apiKey" type="password" placeholder="不修改请留空" />
          </div>
          <div class="form-item">
            <label>Base URL</label>
            <input v-model="editForm.baseUrl" type="text" placeholder="留空使用默认值" />
          </div>
          <div class="form-item">
            <label>模型选择</label>
            <select v-model="editForm.modelName">
              <option value="">使用默认模型</option>
              <option v-for="m in editProviderModels" :key="m.id" :value="m.id">
                {{ m.name }} - {{ m.description }}
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
  </div>
</template>

<style scoped>
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

.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: var(--space-4) var(--space-8);
  border-bottom: 1px solid var(--color-border);
  background: var(--color-bg-primary);
  position: sticky;
  top: 0;
  z-index: 100;
}

.main-content {
  max-width: 800px;
  /* Reduced max-width for better reading flow in list view */
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
  padding: var(--space-2) var(--space-4);
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
  gap: var(--space-2);
}

.add-btn:hover:not(:disabled) {
  background: #333;
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
  box-shadow: var(--shadow-sm);
}

.config-item-row.disabled {
  opacity: 0.6;
  background: var(--color-bg-secondary);
}

.row-left {
  display: flex;
  align-items: center;
  gap: var(--space-4);
}

.provider-icon-wrapper {
  width: 40px;
  height: 40px;
  background: var(--color-bg-secondary);
  border-radius: var(--radius-md);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: var(--text-xl);
}

.row-info {
  display: flex;
  flex-direction: column;
}

.provider-name {
  font-weight: 600;
  font-size: var(--text-base);
  color: var(--color-text-primary);
}

.model-name {
  font-size: var(--text-sm);
  color: var(--color-text-secondary);
  font-family: var(--font-mono);
  margin-top: 2px;
}

.row-right {
  display: flex;
  align-items: center;
  gap: var(--space-4);
}

.status-badge {
  padding: 4px 10px;
  border-radius: var(--radius-full);
  font-size: 12px;
  font-weight: 500;
}

.status-badge.enabled {
  background: #000;
  color: #fff;
}

.status-badge.disabled {
  background: var(--color-bg-tertiary);
  color: var(--color-text-secondary);
}

.row-actions {
  display: flex;
  gap: var(--space-3);
}

.icon-btn {
  width: 32px;
  height: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
  border: none;
  background: transparent;
  color: var(--color-text-tertiary);
  border-radius: var(--radius-md);
  cursor: pointer;
  transition: all var(--transition-fast);
}

.icon-btn:hover {
  background: var(--color-bg-secondary);
  color: var(--color-text-primary);
}

.icon-btn.delete:hover {
  background: #fee2e2;
  color: #dc2626;
}

/* Empty State */
.empty-state {
  text-align: center;
  padding: var(--space-10);
  background: var(--color-bg-secondary);
  border-radius: var(--radius-lg);
  color: var(--color-text-tertiary);
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
  backdrop-filter: blur(2px);
}

.dialog {
  background: #fff;
  border-radius: var(--radius-xl);
  width: 480px;
  max-width: 90vw;
  box-shadow: var(--shadow-xl);
  overflow: hidden;
}

.dialog-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: var(--space-5);
  border-bottom: 1px solid var(--color-border);
  background: var(--color-bg-primary);
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
}

.close-btn:hover {
  background: var(--color-bg-secondary);
  color: var(--color-text-primary);
}

.dialog-body {
  padding: var(--space-6);
}

.form-item {
  margin-bottom: var(--space-5);
}

.form-item label {
  display: block;
  margin-bottom: var(--space-2);
  font-size: 14px;
  font-weight: 500;
  color: var(--color-text-primary);
}

.form-item input,
.form-item select {
  width: 100%;
  padding: 12px;
  background: #fff;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  font-size: 14px;
  outline: none;
  transition: border 0.2s;
}

.form-item input:focus,
.form-item select:focus {
  border-color: #000;
}

.dialog-footer {
  padding: var(--space-5);
  border-top: 1px solid var(--color-border);
  display: flex;
  justify-content: flex-end;
  gap: var(--space-3);
  background: var(--color-bg-primary);
}

.btn {
  padding: 10px 20px;
  border-radius: var(--radius-md);
  font-size: 14px;
  cursor: pointer;
  font-weight: 500;
  transition: all var(--transition-fast);
}

.btn.cancel {
  background: transparent;
  border: 1px solid var(--color-border);
  color: var(--color-text-primary);
}

.btn.cancel:hover {
  background: var(--color-bg-secondary);
}

.btn.primary {
  background: #000;
  border: 1px solid #000;
  color: #fff;
}

.btn.primary:hover {
  background: #333;
  border-color: #333;
  transform: translateY(-1px);
}
</style>
