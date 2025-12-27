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
import { ElMessage, ElMessageBox } from 'element-plus'

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
  return provider?.models || []
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

// 获取提供商图标
const getProviderIcon = (providerId: string) => {
  const icons: Record<string, string> = {
    'google': '🌐',
    'zhipu': '🧠',
    'deepseek': '🔍',
    'openai': '🤖',
    'claude': '🎭'
  }
  return icons[providerId] || '💬'
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
    ElMessage.warning('请填写必填项')
    return
  }

  try {
    const res = await createConfig(addForm.value)
    if (res.code === 200) {
      ElMessage.success('添加成功')
      showAddDialog.value = false
      loadData()
    } else {
      ElMessage.error(res.message || '添加失败')
    }
  } catch (error) {
    ElMessage.error('添加失败')
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
      ElMessage.success('更新成功')
      showEditDialog.value = false
      loadData()
    } else {
      ElMessage.error(res.message || '更新失败')
    }
  } catch (error) {
    ElMessage.error('更新失败')
  }
}

// 切换启用状态
const handleToggle = async (config: ModelConfig) => {
  try {
    const res = await toggleConfig(config.id)
    if (res.code === 200) {
      ElMessage.success(res.data.enabled ? '已启用' : '已禁用')
      loadData()
    }
  } catch (error) {
    ElMessage.error('操作失败')
  }
}

// 删除配置
const handleDelete = async (config: ModelConfig) => {
  try {
    await ElMessageBox.confirm(
      `确定要删除 ${getProviderInfo(config.provider)?.name} 的配置吗？`,
      '删除确认',
      { type: 'warning' }
    )
    const res = await deleteConfig(config.id)
    if (res.code === 200) {
      ElMessage.success('删除成功')
      loadData()
    }
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('删除失败')
    }
  }
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
    <!-- Header -->
    <header class="header">
      <div class="header-left">
        <button class="back-btn" @click="goBack">← 返回</button>
        <span class="logo-icon">⚙️</span>
        <span class="page-title">模型配置</span>
      </div>
    </header>

    <main class="main-content">
      <!-- 说明卡片 -->
      <div class="info-card">
        <h3>💡 配置说明</h3>
        <p>在这里配置您自己的 AI 模型 API Key，配置后将在竞技场中使用您的配置调用模型。</p>
        <p>如果没有配置某个提供商，系统将使用默认配置（如果可用）。</p>
      </div>

      <!-- 已配置列表 -->
      <div class="config-section">
        <div class="section-header">
          <h3>已配置的模型</h3>
          <button class="add-btn" @click="openAddDialog" :disabled="availableProviders.length === 0">
            ➕ 添加配置
          </button>
        </div>

        <div v-if="loading" class="loading">加载中...</div>

        <div v-else-if="configs.length === 0" class="empty-state">
          <p>暂无配置，点击上方按钮添加您的第一个模型配置</p>
        </div>

        <div v-else class="config-grid">
          <div v-for="config in configs" :key="config.id" class="config-card" :class="{ disabled: !config.enabled }">
            <div class="card-header">
              <div class="provider-info">
                <span class="provider-icon">{{ getProviderIcon(config.provider) }}</span>
                <span class="provider-name">{{ getProviderInfo(config.provider)?.name || config.provider }}</span>
              </div>
              <div class="status-badge" :class="{ enabled: config.enabled, disabled: !config.enabled }">
                {{ config.enabled ? '已启用' : '已禁用' }}
              </div>
            </div>

            <div class="card-body">
              <div class="config-item">
                <label>API Key</label>
                <span class="value masked">••••••••••••{{ config.apiKey.slice(-4) }}</span>
              </div>
              <div class="config-item">
                <label>Base URL</label>
                <span class="value">{{ config.baseUrl || getProviderInfo(config.provider)?.defaultBaseUrl }}</span>
              </div>
              <div class="config-item">
                <label>模型</label>
                <span class="value">{{ config.modelName || getProviderInfo(config.provider)?.defaultModel }}</span>
              </div>
            </div>

            <div class="card-actions">
              <button class="action-btn toggle" @click="handleToggle(config)">
                {{ config.enabled ? '禁用' : '启用' }}
              </button>
              <button class="action-btn edit" @click="openEditDialog(config)">编辑</button>
              <button class="action-btn delete" @click="handleDelete(config)">删除</button>
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
            <label>API Key（留空则不修改）</label>
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
          <div class="form-item checkbox">
            <label>
              <input type="checkbox" v-model="editForm.enabled" />
              启用此配置
            </label>
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
}

/* 深色主题渐变 */
[data-theme="dark"] .settings-container,
:root:not([data-theme="light"]) .settings-container {
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

.logo-icon {
  font-size: 24px;
}

.page-title {
  font-size: 20px;
  font-weight: 600;
  color: var(--color-text-primary);
}

.main-content {
  max-width: 1200px;
  margin: 0 auto;
  padding: 32px;
}

.info-card {
  background: var(--color-primary-light);
  border: 1px solid rgba(94, 106, 210, 0.3);
  border-radius: 12px;
  padding: 20px;
  margin-bottom: 32px;
}

.info-card h3 {
  margin: 0 0 12px 0;
  font-size: 16px;
  color: var(--color-text-primary);
}

.info-card p {
  margin: 0 0 8px 0;
  color: var(--color-text-secondary);
  font-size: 14px;
}

.info-card p:last-child {
  margin-bottom: 0;
}

.config-section {
  background: var(--color-bg-elevated);
  border: 1px solid var(--color-border);
  border-radius: 16px;
  padding: 24px;
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
}

.section-header h3 {
  margin: 0;
  font-size: 18px;
  color: var(--color-text-primary);
}

.add-btn {
  padding: 10px 20px;
  background: var(--color-primary-gradient);
  border: none;
  border-radius: 8px;
  color: #fff;
  cursor: pointer;
  transition: all 0.2s;
}

.add-btn:hover:not(:disabled) {
  transform: translateY(-2px);
  box-shadow: 0 4px 15px rgba(94, 106, 210, 0.4);
}

.add-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.loading, .empty-state {
  text-align: center;
  padding: 40px;
  color: var(--color-text-tertiary);
}

.config-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(350px, 1fr));
  gap: 20px;
}

.config-card {
  background: var(--color-bg-secondary);
  border: 1px solid var(--color-border);
  border-radius: 12px;
  overflow: hidden;
  transition: all 0.2s;
}

.config-card:hover {
  border-color: rgba(94, 106, 210, 0.5);
}

.config-card.disabled {
  opacity: 0.6;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 20px;
  background: var(--color-bg-tertiary);
  border-bottom: 1px solid var(--color-border);
}

.provider-info {
  display: flex;
  align-items: center;
  gap: 10px;
}

.provider-icon {
  font-size: 24px;
}

.provider-name {
  font-weight: 600;
  font-size: 16px;
  color: var(--color-text-primary);
}

.status-badge {
  padding: 4px 10px;
  border-radius: 12px;
  font-size: 12px;
}

.status-badge.enabled {
  background: rgba(39, 174, 96, 0.2);
  color: #27ae60;
}

.status-badge.disabled {
  background: rgba(231, 76, 60, 0.2);
  color: #e74c3c;
}

.card-body {
  padding: 20px;
}

.config-item {
  margin-bottom: 12px;
}

.config-item:last-child {
  margin-bottom: 0;
}

.config-item label {
  display: block;
  font-size: 12px;
  color: var(--color-text-tertiary);
  margin-bottom: 4px;
}

.config-item .value {
  font-size: 14px;
  color: var(--color-text-secondary);
  word-break: break-all;
}

.config-item .value.masked {
  font-family: monospace;
  color: var(--color-primary);
}

.card-actions {
  display: flex;
  gap: 8px;
  padding: 16px 20px;
  border-top: 1px solid var(--color-border);
}

.action-btn {
  flex: 1;
  padding: 8px;
  border: 1px solid var(--color-border);
  border-radius: 6px;
  background: transparent;
  color: var(--color-text-tertiary);
  cursor: pointer;
  font-size: 13px;
  transition: all 0.2s;
}

.action-btn:hover {
  border-color: var(--color-primary);
  color: var(--color-primary);
}

.action-btn.delete:hover {
  border-color: var(--color-danger);
  color: var(--color-danger);
}

/* 对话框样式 */
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

.dialog {
  background: var(--color-bg-elevated);
  border: 1px solid var(--color-border);
  border-radius: 16px;
  width: 450px;
  max-width: 90vw;
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
  font-size: 18px;
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

.close-btn:hover {
  color: var(--color-text-primary);
}

.dialog-body {
  padding: 20px;
}

.form-item {
  margin-bottom: 16px;
}

.form-item:last-child {
  margin-bottom: 0;
}

.form-item label {
  display: block;
  margin-bottom: 8px;
  font-size: 14px;
  color: var(--color-text-secondary);
}

.form-item .required {
  color: var(--color-danger);
}

.form-item input,
.form-item select {
  width: 100%;
  padding: 12px;
  background: var(--color-bg-secondary);
  border: 1px solid var(--color-border);
  border-radius: 8px;
  color: var(--color-text-primary);
  font-size: 14px;
  outline: none;
  box-sizing: border-box;
}

.form-item input:focus,
.form-item select:focus {
  border-color: var(--color-primary);
}

.form-item.checkbox label {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
}

.form-item.checkbox input {
  width: auto;
  accent-color: var(--color-primary);
}

.dialog-footer {
  display: flex;
  gap: 12px;
  padding: 20px;
  border-top: 1px solid var(--color-border);
}

.btn {
  flex: 1;
  padding: 12px;
  border-radius: 8px;
  font-size: 14px;
  cursor: pointer;
  transition: all 0.2s;
}

.btn.cancel {
  background: transparent;
  border: 1px solid var(--color-border);
  color: var(--color-text-tertiary);
}

.btn.cancel:hover {
  border-color: var(--color-primary);
  color: var(--color-primary);
}

.btn.primary {
  background: var(--color-primary-gradient);
  border: none;
  color: #fff;
}

.btn.primary:hover {
  transform: translateY(-1px);
  box-shadow: 0 4px 15px rgba(94, 106, 210, 0.4);
}
</style>
