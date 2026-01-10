<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { message, Modal } from 'ant-design-vue'
import {
    PlusOutlined,
    EditOutlined,
    DeleteOutlined,
    SyncOutlined,
    GlobalOutlined,
    CheckOutlined,
    CloseOutlined,
    AppstoreOutlined,
    CloudSyncOutlined
} from '@ant-design/icons-vue'
import {
    getModelProviders,
    createModelProvider,
    updateModelProvider,
    deleteModelProvider,
    toggleModelProvider,
    getAvailableModels,
    createAvailableModel,
    updateAvailableModel,
    deleteAvailableModel,
    toggleAvailableModel,
    syncFromLobeChat,
    getSyncStats,
    type AdminModelProvider,
    type AdminAvailableModel,
    type SyncStats
} from '../../api/admin'

// 状态
const loading = ref(false)
const syncing = ref(false)
const activeView = ref<'providers' | 'models'>('providers')
const syncStats = ref<SyncStats | null>(null)

// 数据列表
const providers = ref<AdminModelProvider[]>([])
const models = ref<AdminAvailableModel[]>([])

// 筛选
const selectedProviderId = ref<string>('')

// 模态框状态
const isProviderModalVisible = ref(false)
const isModelModalVisible = ref(false)
const modalMode = ref<'create' | 'edit'>('create')

// 表单数据
const providerForm = ref<Partial<AdminModelProvider>>({
    id: '',
    name: '',
    defaultBaseUrl: '',
    description: '',
    modelsUrl: '',
    sdkType: 'openai',
    enabled: 1,
    sortOrder: 0
})

const modelForm = ref<Partial<AdminAvailableModel>>({
    providerId: '',
    modelId: '',
    displayName: '',
    description: '',
    contextWindow: 4096,
    supportsVision: 0,
    supportsFunctionCall: 0,
    enabled: 1,
    sortOrder: 0
})

// 加载数据
const loadData = async () => {
    loading.value = true
    try {
        // 并行加载
        const [providersRes, statsRes] = await Promise.all([
            getModelProviders(),
            getSyncStats()
        ])

        if (providersRes.code === 200) {
            providers.value = providersRes.data
        }
        if (statsRes.code === 200) {
            syncStats.value = statsRes.data
        }

        // 如果在模型视图，加载模型
        if (activeView.value === 'models') {
            await loadModels()
        }
    } catch (error) {
        console.error('加载数据失败:', error)
        message.error('加载数据失败')
    } finally {
        loading.value = false
    }
}

const loadModels = async () => {
    loading.value = true
    try {
        const res = await getAvailableModels(selectedProviderId.value || undefined)
        if (res.code === 200) {
            models.value = res.data
        }
    } catch (error) {
        console.error('加载模型列表失败:', error)
    } finally {
        loading.value = false
    }
}

// 切换视图
const switchView = (view: 'providers' | 'models') => {
    activeView.value = view
    if (view === 'models' && models.value.length === 0) {
        loadModels()
    }
}

// 同步操作
const handleSync = async () => {
    syncing.value = true
    try {
        const res = await syncFromLobeChat()
        if (res.code === 200) {
            const result = res.data
            message.success(`同步完成: 成功 ${result.successCount}, 失败 ${result.failCount}`)
            loadData() // 重新加载数据
        }
    } catch (error) {
        console.error('同步失败:', error)
        message.error('同步失败')
    } finally {
        syncing.value = false
    }
}

// Provider 操作
const showCreateProviderModal = () => {
    modalMode.value = 'create'
    providerForm.value = {
        id: '',
        name: '',
        defaultBaseUrl: '',
        description: '',
        modelsUrl: '',
        sdkType: 'openai',
        enabled: 1,
        sortOrder: 0
    }
    isProviderModalVisible.value = true
}

const showEditProviderModal = (provider: AdminModelProvider) => {
    modalMode.value = 'edit'
    providerForm.value = { ...provider }
    isProviderModalVisible.value = true
}

const handleProviderSubmit = async () => {
    try {
        if (modalMode.value === 'create') {
            await createModelProvider(providerForm.value)
            message.success('创建成功')
        } else {
            await updateModelProvider(providerForm.value.id!, providerForm.value)
            message.success('更新成功')
        }
        isProviderModalVisible.value = false
        loadData()
    } catch (error) {
        console.error('保存失败:', error)
    }
}

const handleToggleProvider = async (provider: AdminModelProvider) => {
    try {
        await toggleModelProvider(provider.id)
        message.success(`${provider.enabled ? '禁用' : '启用'}成功`)
        loadData()
    } catch (error) {
        console.error('切换状态失败:', error)
    }
}

const handleDeleteProvider = async (provider: AdminModelProvider) => {
    Modal.confirm({
        title: '确认删除厂商？',
        content: `删除厂商 "${provider.name}" 将同时删除其下的所有模型，且不可恢复！`,
        okText: '确认删除',
        okType: 'danger',
        cancelText: '取消',
        onOk: async () => {
            try {
                await deleteModelProvider(provider.id)
                message.success('删除成功')
                loadData()
            } catch (error) {
                console.error('删除失败:', error)
            }
        }
    })
}

// Model 操作
const showCreateModelModal = () => {
    modalMode.value = 'create'
    modelForm.value = {
        providerId: selectedProviderId.value || (providers.value.length > 0 ? providers.value[0].id : ''),
        modelId: '',
        displayName: '',
        description: '',
        contextWindow: 4096,
        supportsVision: 0,
        supportsFunctionCall: 0,
        enabled: 1,
        sortOrder: 0
    }
    isModelModalVisible.value = true
}

const showEditModelModal = (model: AdminAvailableModel) => {
    modalMode.value = 'edit'
    modelForm.value = { ...model }
    isModelModalVisible.value = true
}

const handleModelSubmit = async () => {
    try {
        if (modalMode.value === 'create') {
            await createAvailableModel(modelForm.value)
            message.success('创建成功')
        } else {
            await updateAvailableModel(modelForm.value.id!, modelForm.value)
            message.success('更新成功')
        }
        isModelModalVisible.value = false
        loadModels()
    } catch (error) {
        console.error('保存失败:', error)
    }
}

const handleToggleModel = async (model: AdminAvailableModel) => {
    try {
        await toggleAvailableModel(model.id)
        message.success(`${model.enabled ? '禁用' : '启用'}成功`)
        // 乐观更新
        model.enabled = model.enabled ? 0 : 1
    } catch (error) {
        console.error('切换状态失败:', error)
        loadModels()
    }
}

const handleDeleteModel = async (model: AdminAvailableModel) => {
    Modal.confirm({
        title: '确认删除模型？',
        content: `确定要删除模型 "${model.displayName}" 吗？`,
        okText: '确认删除',
        okType: 'danger',
        cancelText: '取消',
        onOk: async () => {
            try {
                await deleteAvailableModel(model.id)
                message.success('删除成功')
                loadModels()
            } catch (error) {
                console.error('删除失败:', error)
            }
        }
    })
}

// 获取厂商图标
const getProviderIcon = (id: string) => {
    const iconMap: Record<string, string> = {
        'openai': 'openai.svg',
        'anthropic': 'anthropic.svg',
        'google': 'google-color.svg',
        'deepseek': 'deepseek-color.svg',
        'zhipu': 'zhipu-color.svg',
        'qwen': 'qwen-color.svg',
        'moonshot': 'moonshot.svg',
        'hunyuan': 'hunyuan-color.svg',
        'cloudflare': 'cloudflare-color.svg',
        'github': 'githubcopilot.svg',
        'azure': 'azureai-color.svg', // Fixed filename from microsoft-color.svg
        'bedrock': 'bedrock-color.svg',
        'baichuan': 'baichuan-color.svg',
        'minimax': 'minimax-color.svg',
        'stepfun': 'stepfun-color.svg',
        'spark': 'spark-color.svg',
        'sensenova': 'sensenova-color.svg',
        'mistral': 'mistral-color.svg',
        'perplexity': 'perplexity-color.svg',
        'groq': 'groq.svg',
        'cohere': 'cohere-color.svg',
        'novita': 'novita-color.svg',
        'togetherai': 'together-color.svg',
        'ollama': 'ollama.svg',
        'openrouter': 'openrouter.svg',
        'yi': 'yi-color.svg' // Assuming yi-color.svg exists, though not in list. User didn't upload it? Checking list again. No 'yi-color.svg'. Using logo.svg fallback implicitly.
    }
    const filename = iconMap[id] || 'logo.svg' // default fallback
    return new URL(`../../assets/${filename}`, import.meta.url).href
}

const formatDate = (dateStr: string) => {
    if (!dateStr) return '-'
    const date = new Date(dateStr)
    return date.toLocaleDateString('zh-CN', {
        month: '2-digit',
        day: '2-digit',
        hour: '2-digit',
        minute: '2-digit'
    })
}

const getProviderName = (id: string) => {
    const p = providers.value.find(p => p.id === id)
    return p ? p.name : id
}

onMounted(() => {
    loadData()
})
</script>

<template>
    <div class="model-management">
        <!-- 顶部操作栏 -->
        <div class="actions-bar">
            <div class="view-switcher">
                <a-radio-group v-model:value="activeView" button-style="solid" @change="(e: any) => switchView(e.target.value)">
                    <a-radio-button value="providers">
                        <GlobalOutlined /> 厂商管理
                    </a-radio-button>
                    <a-radio-button value="models">
                        <AppstoreOutlined /> 模型列表
                    </a-radio-button>
                </a-radio-group>
            </div>

            <div class="sync-actions">
                <div class="sync-stats" v-if="syncStats">
                    <span title="启用/总厂商">厂商: <strong>{{ syncStats.enabledProviders }}</strong>/{{ syncStats.totalProviders }}</span>
                    <a-divider type="vertical" />
                    <span title="启用/总模型">模型: <strong>{{ syncStats.enabledModels }}</strong>/{{ syncStats.totalModels }}</span>
                </div>
                <a-button type="primary" :loading="syncing" @click="handleSync">
                    <template #icon><CloudSyncOutlined /></template>
                    从 Lobe Chat 同步
                </a-button>
            </div>
        </div>

        <!-- 厂商列表 -->
        <div v-if="activeView === 'providers'" class="view-content">
            <div class="toolbar">
                <h3>所有厂商</h3>
                <a-button type="primary" @click="showCreateProviderModal">
                    <template #icon><PlusOutlined /></template>
                    新增厂商
                </a-button>
            </div>

            <a-table :dataSource="providers" :rowKey="'id'" :loading="loading" :pagination="false" class="provider-table" :scroll="{ x: 1000 }">
                <a-table-column title="图标" width="70px" align="center">
                    <template #default="{ record }">
                        <div class="provider-icon-wrapper">
                             <img :src="getProviderIcon(record.id)" :alt="record.name" class="provider-icon-img" 
                                  @error="(e: any) => e.target.src = getProviderIcon('default')" />
                        </div>
                    </template>
                </a-table-column>
                <a-table-column title="ID" dataIndex="id" width="100px">
                     <template #default="{ text }"><span class="code-text">{{ text }}</span></template>
                </a-table-column>
                <a-table-column title="名称" dataIndex="name" width="150px" >
                    <template #default="{ text }"><strong>{{ text }}</strong></template>
                </a-table-column>
                <a-table-column title="Base URL" dataIndex="defaultBaseUrl" ellipsis />
                <a-table-column title="SDK" dataIndex="sdkType" width="100px">
                    <template #default="{ text }">
                        <a-tag :color="text === 'openai' ? 'blue' : (text === 'anthropic' ? 'orange' : 'green')">{{ text }}</a-tag>
                    </template>
                </a-table-column>
                <a-table-column title="状态" width="80px" align="center">
                    <template #default="{ record }">
                        <a-switch :checked="record.enabled === 1" @change="() => handleToggleProvider(record)" />
                    </template>
                </a-table-column>
                <a-table-column title="最后同步" width="130px" align="center">
                    <template #default="{ record }">
                        <span class="date-text">{{ formatDate(record.syncedAt) }}</span>
                    </template>
                </a-table-column>
                <a-table-column title="操作" width="120px" align="center" fixed="right">
                    <template #default="{ record }">
                        <a-space size="small">
                            <a-button type="link" size="small" @click="showEditProviderModal(record)">编辑</a-button>
                            <a-button type="link" size="small" danger @click="handleDeleteProvider(record)">删除</a-button>
                        </a-space>
                    </template>
                </a-table-column>
            </a-table>
        </div>

        <!-- 模型列表 -->
        <div v-if="activeView === 'models'" class="view-content">
            <div class="toolbar">
                <div class="filters">
                    <a-select v-model:value="selectedProviderId" style="width: 200px" placeholder="筛选厂商" allowClear @change="loadModels">
                        <a-select-option v-for="p in providers" :key="p.id" :value="p.id">
                            {{ p.name }}
                        </a-select-option>
                    </a-select>
                </div>
                <a-button type="primary" @click="showCreateModelModal">
                    <template #icon><PlusOutlined /></template>
                    新增模型
                </a-button>
            </div>

            <a-table :dataSource="models" :rowKey="'id'" :loading="loading" :pagination="{ pageSize: 20 }">
                <a-table-column title="厂商" width="120px">
                    <template #default="{ record }">
                        {{ getProviderName(record.providerId) }}
                    </template>
                </a-table-column>
                <a-table-column title="显示名称" dataIndex="displayName" width="200px" />
                <a-table-column title="模型ID" dataIndex="modelId" width="200px" />
                <a-table-column title="上下文" dataIndex="contextWindow" width="100px">
                     <template #default="{ text }">{{ text ? (text / 1024).toFixed(0) + 'k' : '-' }}</template>
                </a-table-column>
                <a-table-column title="能力" width="150px">
                    <template #default="{ record }">
                        <a-space size="small">
                            <a-tag v-if="record.supportsVision" color="purple">Vision</a-tag>
                            <a-tag v-if="record.supportsFunctionCall" color="cyan">FC</a-tag>
                        </a-space>
                    </template>
                </a-table-column>
                <a-table-column title="来源" dataIndex="source" width="80px">
                     <template #default="{ text }">
                        <a-tag :color="text === 'sync' ? 'blue' : 'green'">{{ text === 'sync' ? '同步' : '手动' }}</a-tag>
                    </template>
                </a-table-column>
                <a-table-column title="状态" width="100px">
                    <template #default="{ record }">
                        <a-switch :checked="record.enabled === 1" @change="() => handleToggleModel(record)" />
                    </template>
                </a-table-column>
                <a-table-column title="操作" width="120px">
                    <template #default="{ record }">
                        <a-space>
                            <a-button type="link" size="small" @click="showEditModelModal(record)">编辑</a-button>
                            <a-button type="link" size="small" danger @click="handleDeleteModel(record)">删除</a-button>
                        </a-space>
                    </template>
                </a-table-column>
            </a-table>
        </div>

        <!-- 厂商编辑模态框 -->
        <a-modal v-model:visible="isProviderModalVisible" :title="modalMode === 'create' ? '新增厂商' : '编辑厂商'" @ok="handleProviderSubmit">
            <a-form layout="vertical">
                <a-form-item label="ID (唯一标识)" required>
                    <a-input v-model:value="providerForm.id" :disabled="modalMode === 'edit'" placeholder="如 openai" />
                </a-form-item>
                <a-form-item label="显示名称" required>
                    <a-input v-model:value="providerForm.name" placeholder="如 OpenAI" />
                </a-form-item>
                <a-form-item label="默认 Base URL">
                    <a-input v-model:value="providerForm.defaultBaseUrl" placeholder="https://api.openai.com/v1" />
                </a-form-item>
                <a-form-item label="SDK 类型">
                    <a-select v-model:value="providerForm.sdkType">
                        <a-select-option value="openai">OpenAI 兼容</a-select-option>
                        <a-select-option value="anthropic">Anthropic</a-select-option>
                        <a-select-option value="google">Google Gemini</a-select-option>
                    </a-select>
                </a-form-item>
                <a-form-item label="排序权重">
                    <a-input-number v-model:value="providerForm.sortOrder" :min="0" />
                </a-form-item>
            </a-form>
        </a-modal>

        <!-- 模型编辑模态框 -->
        <a-modal v-model:visible="isModelModalVisible" :title="modalMode === 'create' ? '新增模型' : '编辑模型'" @ok="handleModelSubmit">
            <a-form layout="vertical">
                <a-form-item label="所属厂商" required>
                    <a-select v-model:value="modelForm.providerId" :disabled="modalMode === 'edit'">
                        <a-select-option v-for="p in providers" :key="p.id" :value="p.id">{{ p.name }}</a-select-option>
                    </a-select>
                </a-form-item>
                <a-form-item label="模型 ID (API调用使用)" required>
                    <a-input v-model:value="modelForm.modelId" placeholder="如 gpt-4o" />
                </a-form-item>
                <a-form-item label="显示名称" required>
                    <a-input v-model:value="modelForm.displayName" placeholder="如 GPT-4 Turbo" />
                </a-form-item>
                <a-form-item label="上下文长度 (Tokens)">
                    <a-input-number v-model:value="modelForm.contextWindow" :min="1024" step="1024" style="width: 100%" />
                </a-form-item>
                <a-form-item label="能力支持">
                    <a-checkbox v-model:checked="modelForm.supportsVision" :value="1">支持视觉</a-checkbox>
                    <a-checkbox v-model:checked="modelForm.supportsFunctionCall" :value="1">支持函数调用</a-checkbox>
                </a-form-item>
                <a-form-item label="排序权重">
                    <a-input-number v-model:value="modelForm.sortOrder" :min="0" />
                </a-form-item>
                <a-form-item label="描述">
                    <a-textarea v-model:value="modelForm.description" />
                </a-form-item>
            </a-form>
        </a-modal>
    </div>
</template>

<style scoped>
.model-management {
    padding: 0;
}

.actions-bar {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 24px;
    padding: 16px;
    background: #f8f9fa;
    border-radius: 8px;
    border: 1px solid #eee;
}

.sync-actions {
    display: flex;
    align-items: center;
    gap: 16px;
}

.sync-stats {
    font-size: 13px;
    color: #666;
    display: flex;
    align-items: center;
    gap: 8px;
}

.divider {
    color: #ddd;
}

.toolbar {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 16px;
}

.provider-icon {
    width: 32px;
    height: 32px;
    background: #e6f7ff;
    color: #1890ff;
    border-radius: 6px;
    display: flex;
    align-items: center;
    justify-content: center;
    font-weight: bold;
    font-size: 16px;
}

.provider-icon-wrapper {
    width: 40px;
    height: 40px;
    border-radius: 8px;
    background: #f5f5f5;
    display: flex;
    align-items: center;
    justify-content: center;
    padding: 6px;
    margin: 0 auto;
}

.provider-icon-img {
    width: 100%;
    height: 100%;
    object-fit: contain;
}

.code-text {
    font-family: 'Monaco', 'Menlo', 'Ubuntu Mono', 'Consolas', 'source-code-pro', monospace;
    color: #666;
    background: #f9f9f9;
    padding: 2px 4px;
    border-radius: 4px;
    font-size: 13px;
}

.date-text {
    color: #999;
    font-size: 13px;
}

.provider-table :deep(.ant-table-row) {
    height: 64px;
}

.provider-table :deep(.ant-table-cell) {
    vertical-align: middle;
}
</style>
