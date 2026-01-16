<script setup lang="ts">
import { ref, computed, watch, defineProps, defineEmits } from 'vue'
import { CloseOutlined } from '@ant-design/icons-vue'
import ProviderLogo from './ProviderLogo.vue'
import type { AvailableModelInfo } from '../api/arena'

const props = defineProps<{
  open: boolean
  models: AvailableModelInfo[]
  selectedModelId?: string
}>()

const emit = defineEmits<{
  (e: 'update:open', value: boolean): void
  (e: 'select', modelId: string): void
}>()

const activeProvider = ref<string>('')

// Computed: Unique Providers
const sortedUniqueProviders = computed(() => {
  const providerIds = new Set(props.models.map(p => p.provider))
  return Array.from(providerIds).map(id => {
    // Attempt to find a "nicer" name if possible, currently using ID
    return {
      id,
      name: getProviderDisplayName(id)
    }
  }).sort((a, b) => {
    const weights: Record<string, number> = { 
        openai: 100, 
        google: 90, 
        github: 85,
        aliyun: 80, // Tencent Hunyuan usually fits here or similar
        deepseek: 75,
        zhipu: 70,
        moonshot: 65,
        cloudflare: 60
    }
    const wa = weights[a.id] || 0
    const wb = weights[b.id] || 0
    return wb - wa
  })
})

const getProviderDisplayName = (providerId: string) => {
    const map: Record<string, string> = {
        openai: 'OpenAI GPT',
        google: 'Google Gemini',
        github: 'GitHub Models',
        aliyun: '通义千问', // Aliyun
        hunyuan: '腾讯混元',
        deepseek: 'DeepSeek',
        zhipu: '智谱 GLM',
        moonshot: 'Moonshot Kimi',
        cloudflare: 'Cloudflare Workers AI',
        claude: 'Anthropic Claude'
    }
    return map[providerId.toLowerCase()] || providerId
}

// Filtered Models
const filteredModels = computed(() => {
    if (!activeProvider.value) return []
    return props.models.filter(m => m.provider === activeProvider.value)
})

// Initialize active provider when opening
watch(() => props.open, (val) => {
    if (val) {
        if (props.selectedModelId) {
             const m = props.models.find(x => x.modelId === props.selectedModelId)
             if (m) {
                 activeProvider.value = m.provider
             } else if (sortedUniqueProviders.value.length > 0) {
                 activeProvider.value = sortedUniqueProviders.value[0].id
             }
        } else {
             if (sortedUniqueProviders.value.length > 0) {
                 activeProvider.value = sortedUniqueProviders.value[0].id
             }
        }
    }
})

const selectModel = (modelId: string) => {
    emit('select', modelId)
    emit('update:open', false)
}

const close = () => {
    emit('update:open', false)
}
</script>

<template>
  <div v-if="open" class="modal-overlay" @click.self="close">
    <div class="modal-content model-selector-dialog">
      <div class="modal-header">
        <h3>选择 AI 模型</h3>
        <button class="close-btn" @click="close">
          <CloseOutlined />
        </button>
      </div>
      
      <div class="modal-body-layout">
        <!-- Sidebar -->
        <div class="category-sidebar">
           <div v-for="p in sortedUniqueProviders" :key="p.id" 
                class="category-item" 
                :class="{ active: activeProvider === p.id }"
                @click="activeProvider = p.id">
                <ProviderLogo :providerId="p.id" :size="20" />
                <span class="category-name">{{ p.name }}</span>
           </div>
        </div>

        <!-- Content -->
        <div class="model-content-area">
           <div v-if="filteredModels.length > 0" class="provider-grid">
               <div v-for="m in filteredModels" :key="m.modelId"
                    class="provider-option"
                    :class="{ selected: selectedModelId === m.modelId }"
                    @click="selectModel(m.modelId)">
                    <ProviderLogo :providerId="m.provider" :size="32" />
                    <span class="opt-name">{{ m.displayName }}</span>
                    <span class="opt-desc">{{ m.provider }}</span>
               </div>
           </div>
           <div v-else class="empty-state">
               该厂商暂无模型
           </div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.modal-overlay {
    position: fixed;
    top: 0; left: 0; right: 0; bottom: 0;
    background: rgba(0,0,0,0.5);
    z-index: 2000;
    display: flex;
    align-items: center;
    justify-content: center;
    padding: 20px;
    backdrop-filter: blur(4px);
}

.model-selector-dialog {
    width: 800px;
    max-width: 90vw;
    height: 600px;
    max-height: 85vh;
    background: white;
    border-radius: 16px;
    box-shadow: 0 20px 25px -5px rgba(0, 0, 0, 0.1), 0 10px 10px -5px rgba(0, 0, 0, 0.04);
    display: flex;
    flex-direction: column;
    overflow: hidden;
}

.modal-header {
    padding: 16px 24px;
    border-bottom: 1px solid #e5e7eb;
    display: flex;
    justify-content: space-between;
    align-items: center;
}

.modal-header h3 {
    margin: 0;
    font-size: 18px;
    font-weight: 600;
    color: #111827;
}

.close-btn {
    background: none;
    border: none;
    cursor: pointer;
    font-size: 18px;
    color: #9ca3af;
    padding: 4px;
    border-radius: 4px;
    transition: all 0.2s;
    display: flex;
    align-items: center; justify-content: center;
}
.close-btn:hover {
    background: #f3f4f6;
    color: #374151;
}

.modal-body-layout {
    display: flex;
    flex: 1;
    overflow: hidden;
}

.category-sidebar {
    width: 220px;
    background: #f9fafb;
    border-right: 1px solid #e5e7eb;
    overflow-y: auto;
    padding: 12px;
    display: flex;
    flex-direction: column;
    gap: 4px;
}

.category-item {
    display: flex;
    align-items: center;
    gap: 12px;
    padding: 10px 12px;
    border-radius: 8px;
    cursor: pointer;
    transition: all 0.2s;
    color: #4b5563;
}
.category-item:hover {
    background: rgba(0,0,0,0.03);
    color: #111827;
}
.category-item.active {
    background: #fff;
    color: #000;
    box-shadow: 0 1px 2px rgba(0,0,0,0.05);
    font-weight: 500;
}
.category-name {
    font-size: 14px;
}

.model-content-area {
    flex: 1;
    padding: 24px;
    overflow-y: auto;
    background: #fff;
}

.provider-grid {
    display: grid;
    grid-template-columns: repeat(auto-fill, minmax(150px, 1fr));
    gap: 16px;
}

.provider-option {
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    padding: 20px 12px;
    border: 1px solid #e5e7eb;
    border-radius: 12px;
    cursor: pointer;
    transition: all 0.2s;
    background: #fff;
    gap: 12px;
    text-align: center;
    min-height: 140px;
}

.provider-option:hover {
    border-color: #000;
    transform: translateY(-2px);
    box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.1);
}

.provider-option.selected {
    border-color: #000;
    background: #f4f4f5;
    box-shadow: 0 0 0 1px #000;
}

.opt-name {
    font-weight: 600;
    font-size: 14px;
    color: #111827;
    line-height: 1.3;
}
.opt-desc {
    font-size: 12px;
    color: #9ca3af;
}

.empty-state {
    color: #9ca3af;
    text-align: center;
    margin-top: 40px;
}
</style>
