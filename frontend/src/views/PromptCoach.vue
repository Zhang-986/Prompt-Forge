<script setup lang="ts">
import { ref, nextTick, onMounted, computed } from 'vue'
import { marked } from 'marked'
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import {
    SendOutlined,
    RobotOutlined,
    UserOutlined,
    CheckOutlined,
    SettingOutlined,
    DownOutlined,
    InfoCircleOutlined
} from '@ant-design/icons-vue'
import ProviderLogo from '../components/ProviderLogo.vue'
import ModelSelectorModal from '../components/ModelSelectorModal.vue'
import {
    startAgentSession,
    sendAgentMessage,
    getCoachSession,
    type CoachSession
} from '../api/promptCoach'
import { getAvailableModels, type AvailableModelInfo } from '../api/arena'

const router = useRouter()
// route 备用于后续功能

// 状态
const loading = ref(false)
const sending = ref(false)
const session = ref<CoachSession | null>(null)
const userInput = ref('')
const currentAiResponse = ref('')
const chatContainer = ref<HTMLElement | null>(null)
const textareaRef = ref<any>(null)  // Ant Design Vue textarea ref

// 模型选择 - Sidebar Logic
const selectedProvider = ref('')
const availableProviders = ref<AvailableModelInfo[]>([])
const loadingProviders = ref(false)
const showModelModal = ref(false)




const selectedModelInfo = computed(() => {
    if (!selectedProvider.value) return null
    return availableProviders.value.find(p => p.modelId === selectedProvider.value)
})

const isSelectedModelUnsupported = computed(() => {
    if (!selectedModelInfo.value) return false
    const unsupported = ['google', 'anthropic', 'claude', 'cloudflare']
    return unsupported.includes(selectedModelInfo.value.provider.toLowerCase())
})

const selectModel = (modelId: string) => {
    selectedProvider.value = modelId
    showModelModal.value = false
}

// 加载用户配置的模型
const loadProviders = async () => {
    loadingProviders.value = true
    try {
        const res = await getAvailableModels()
        if (res.code === 200 && res.data.length > 0) {
            // Graceful Degradation: Allow all models
            availableProviders.value = res.data

            // Auto Select first available model
            if (availableProviders.value.length > 0) {
                const first = availableProviders.value[0]
                if (first) selectedProvider.value = first.modelId
            }
        }
    } catch (error) {
        console.error('加载模型列表失败', error)
    } finally {
        loadingProviders.value = false
    }
}


const userAvatar = ref('')

// 页面加载时获取模型列表和用户信息
onMounted(() => {
    loadProviders()
    const userStr = localStorage.getItem('user')
    if (userStr) {
        try {
            const u = JSON.parse(userStr)
            userAvatar.value = u.avatar || ''
        } catch (e) {
            console.error('Failed to parse user info', e)
        }
    }
})



// 开始对话
const startChat = async () => {
    if (!userInput.value.trim()) {
        message.warning('请输入你的想法')
        return
    }

    const initialInput = userInput.value
    // 立即清空输入框
    clearInput()

    // 显示用户消息
    if (!session.value) {
        // 创建临时会话结构用于显示
        session.value = {
            sessionId: '',
            history: [{
                role: 'user',
                content: initialInput,
                timestamp: new Date().toISOString()
            }],
            currentPhase: 'clarification',
            phaseDescription: '',
            turnCount: 0,
            extractedInfo: {},
            generatedPrompt: null,
            promptGenerated: false
        }
    } else if (session.value) {
        session.value.history.push({
            role: 'user',
            content: initialInput,
            timestamp: new Date().toISOString()
        })
    }

    sending.value = true
    currentAiResponse.value = ''
    scrollToBottom()

    try {
        // 1. 创建会话 (如果还没有)
        let sessionId = session.value?.sessionId
        if (!sessionId) {
            // ⚡ 性能优化：快速创建会话（< 100ms）
            const res = await startAgentSession({
                initialInput: initialInput,
                provider: selectedProvider.value || undefined
            })
            if (res.code === 200) {
                session.value = res.data
                sessionId = res.data.sessionId
                
                // ⚡ 立即获取首次 AI 回复（真正的 SSE 流式）
                thoughtProcess.value = []
                await sendAgentMessage(
                    { sessionId: sessionId, message: null as any }, // null 表示首次对话
                    (chunk) => {
                        handleSSEChunk(chunk)
                        scrollToBottom()
                    },
                    () => {
                        // 完成后保存到 history
                        if (currentAiResponse.value) {
                            session.value!.history.push({
                                role: 'assistant',
                                content: currentAiResponse.value,
                                timestamp: new Date().toISOString()
                            })
                        }
                        currentAiResponse.value = ''
                        sending.value = false
                    },
                    (error) => {
                        message.error('获取回复失败: ' + error.message)
                        currentAiResponse.value = ''
                        sending.value = false
                    }
                )
            } else {
                message.error(res.message || '启动失败')
                userInput.value = initialInput
                sending.value = false
            }
        }
    } catch (error: any) {
        message.error(error.response?.data?.message || '启动失败')
        userInput.value = initialInput
        sending.value = false
    }
}

const thoughtProcess = ref<{ type: string; content: string; name?: string; params?: string; preview?: string }[]>([])

// 处理 SSE 事件流
const handleSSEChunk = (chunk: string) => {
    // 检查是否是后端发送的事件标记（格式：__SSE_EVENT__:TYPE:data）
    if (chunk.startsWith('__SSE_EVENT__:')) {
        const parts = chunk.split(':')
        const eventType = parts[1]
        const data = parts.slice(2).join(':')
        
        switch (eventType) {
            case 'THOUGHT':
                thoughtProcess.value.push({ type: 'thought', content: data })
                break
            case 'TOOL_START':
                try {
                    const toolInfo = JSON.parse(data)
                    thoughtProcess.value.push({ 
                        type: 'tool_start', 
                        content: toolInfo.display || toolInfo.name,
                        name: toolInfo.name,
                        params: toolInfo.params
                    })
                } catch (e) {
                    console.error('解析 TOOL_START 失败:', e)
                }
                break
            case 'TOOL_END':
                try {
                    const toolResult = JSON.parse(data)
                    thoughtProcess.value.push({ 
                        type: 'tool_end', 
                        content: toolResult.display || '执行完成',
                        preview: toolResult.preview || `结果长度: ${toolResult.length} 字符`
                    })
                } catch (e) {
                    console.error('解析 TOOL_END 失败:', e)
                }
                break
        }
    } 
    // 检查是否是前端包装的事件（格式：__EVENT__:TYPE:data，来自旧的SSE解析）
    else if (chunk.startsWith('__EVENT__:')) {
        const parts = chunk.split(':')
        const eventType = parts[1]
        const data = parts.slice(2).join(':')
        
        switch (eventType) {
            case 'THOUGHT':
                thoughtProcess.value.push({ type: 'thought', content: data })
                break
            case 'TOOL_START':
                try {
                    const toolInfo = JSON.parse(data)
                    thoughtProcess.value.push({ 
                        type: 'tool_start', 
                        content: toolInfo.display || toolInfo.name,
                        name: toolInfo.name,
                        params: toolInfo.params
                    })
                } catch (e) {
                    console.error('解析 TOOL_START 失败:', e)
                }
                break
            case 'TOOL_END':
                try {
                    const toolResult = JSON.parse(data)
                    thoughtProcess.value.push({ 
                        type: 'tool_end', 
                        content: toolResult.display || '执行完成',
                        preview: toolResult.preview || `结果长度: ${toolResult.length} 字符`
                    })
                } catch (e) {
                    console.error('解析 TOOL_END 失败:', e)
                }
                break
        }
    } 
    else {
        // 普通文本内容，追加到 AI 回复
        currentAiResponse.value += chunk
    }
}

// 发送消息 (保持不变，但增加防抖或状态检查)
const sendMessage = async () => {
    if (!userInput.value.trim() || !session.value) return
    if (sending.value) return

    const userMessage = userInput.value
    clearInput()

    sending.value = true
    currentAiResponse.value = ''
    thoughtProcess.value = []

    session.value.history.push({
        role: 'user',
        content: userMessage,
        timestamp: new Date().toISOString()
    })
    scrollToBottom()

    try {
        await sendAgentMessage(
            {
                sessionId: session.value.sessionId,
                message: userMessage
            },
            (chunk) => {
                handleSSEChunk(chunk)
                scrollToBottom()
            },
            async () => {
                session.value!.history.push({
                    role: 'assistant',
                    content: currentAiResponse.value,
                    timestamp: new Date().toISOString()
                })
                currentAiResponse.value = ''
                sending.value = false

                // 后台刷新状态
                getCoachSession(session.value!.sessionId).then(res => {
                    if (res.code === 200) session.value = res.data
                })
            },
            (error) => {
                message.error('发送失败: ' + error.message)
                sending.value = false
            }
        )
    } catch (error: any) {
        message.error('发送失败')
        sending.value = false
    }
}

// 确认保存 - 弹窗相关
const showSaveDialog = ref(false)
const savePromptName = ref('')
const savePromptDesc = ref('')
const saving = ref(false)

// 确认保存
const handleConfirm = async () => {
    if (!session.value?.generatedPrompt) {
        message.warning('尚未生成最终 Prompt')
        return
    }

    // 打开保存弹窗，让用户输入名称
    savePromptName.value = ''
    savePromptDesc.value = ''
    showSaveDialog.value = true
}

// 执行保存
const doSave = async () => {
    if (!savePromptName.value.trim()) {
        message.warning('请输入 Prompt 名称')
        return
    }

    // 获取当前工作空间 ID（从 localStorage）
    const workspaceId = localStorage.getItem('currentWorkspaceId')
    if (!workspaceId) {
        message.error('请先在主页选择或创建一个工作空间')
        return
    }

    saving.value = true
    try {
        // 导入 createPrompt
        const { createPrompt } = await import('../api/prompt')

        const res = await createPrompt({
            name: savePromptName.value.trim(),
            description: savePromptDesc.value.trim() || '由 Prompt 教练生成',
            content: session.value!.generatedPrompt!,
            workspaceId: parseInt(workspaceId)
        })

        if (res.code === 200) {
            message.success('Prompt 已创建成功！')
            showSaveDialog.value = false
            router.push(`/app/prompts/${res.data.id}/versions`)
        } else {
            message.error(res.message || '保存失败')
        }
    } catch (error: any) {
        message.error(error.response?.data?.message || '保存失败')
    } finally {
        saving.value = false
    }
}

// 滚动到底部
const scrollToBottom = () => {
    nextTick(() => {
        if (chatContainer.value) {
            chatContainer.value.scrollTop = chatContainer.value.scrollHeight
        }
    })
}

// 返回

// 强制清空输入框 (同步执行)
const clearInput = () => {
    // 1. 设置 Vue 响应式变量
    userInput.value = ''

    // 2. 同步直接操作 DOM（不用 nextTick）
    try {
        if (textareaRef.value) {
            // Ant Design Vue 组件可能嵌套了 textarea
            const component = textareaRef.value
            const inputEl = component.$el?.querySelector('textarea')
                || component.$el
                || component
            if (inputEl && inputEl.tagName === 'TEXTAREA') {
                inputEl.value = ''
                // 触发 input 事件，确保 Vue 同步状态
                inputEl.dispatchEvent(new Event('input', { bubbles: true }))
            }
        }
    } catch (e) {
        console.warn('clearInput DOM 操作失败:', e)
    }
}

// Markdown 渲染
const renderMarkdown = (content: string) => {
    // 简单的打字机效果：如果不完整，可能 markdown 解析会有问题，但 marked 通常能处理
    // 为了防止闪烁，可以只渲染已完成的部分，但这里先保持实时渲染
    return marked.parse(content)
}

// 处理回车发送
const handleKeydown = async (e: KeyboardEvent) => {
    if (e.key === 'Enter' && !e.shiftKey) {
        e.preventDefault()
        if (session.value) {
            await sendMessage()
        } else {
            await startChat()
        }
    }
}
</script>

<template>
    <div class="coach-container">
        <!-- Header Removed -->

        <!-- 对话区域 -->

        <!-- 对话区域 -->
        <div class="chat-area" ref="chatContainer">
            <!-- 欢迎信息 -->
            <div v-if="!session" class="welcome-section">
                <div class="welcome-icon">
                    <RobotOutlined />
                </div>
                <h2>Prompt 教练</h2>
                <p>我会通过几轮对话，帮你生成高质量的 Prompt</p>
                <p class="hint">告诉我你想做什么，比如：做个管理系统、写个数据分析脚本...</p>

                <div class="provider-select">
                    <span class="label">选择 AI 模型：</span>

                    <div class="model-trigger" @click="showModelModal = true">
                        <template v-if="selectedModelInfo">
                            <ProviderLogo :providerId="selectedModelInfo.provider" :size="20" />
                            <span class="model-name">{{ selectedModelInfo.displayName }}</span>
                        </template>
                        <template v-else>
                            <span class="model-name" style="color: var(--color-text-tertiary);">请选择模型</span>
                        </template>
                        <DownOutlined class="arrow-icon" />
                    </div>

                    <span v-if="availableProviders.length === 0 && !loadingProviders" class="no-model-hint">
                        暂无可用模型，请先配置
                    </span>
                </div>

                <p v-if="isSelectedModelUnsupported" class="coach-model-hint">
                    <InfoCircleOutlined /> 此模型不支持联网工具，仅提供对话建议
                </p>
                <p v-if="selectedModelInfo && !isSelectedModelUnsupported" class="coach-model-hint success">
                    <CheckOutlined /> AI 可自动调用联网、代码分析等工具
                </p>
            </div>

            <!-- Model Selection Modal (Categorized) -->
            <ModelSelectorModal v-model:open="showModelModal" :models="availableProviders"
                :selectedModelId="selectedProvider" @select="selectModel" />


            <!-- 对话消息 -->
            <div v-if="session" class="messages">
                <div v-for="(turn, index) in session.history" :key="index" :class="['message', turn.role]">
                    <div class="avatar">
                        <template v-if="turn.role === 'user'">
                            <img v-if="userAvatar" :src="userAvatar" class="user-avatar-img" />
                            <UserOutlined v-else />
                        </template>
                        <RobotOutlined v-else />
                    </div>
                    <div class="content markdown-body" v-html="renderMarkdown(turn.content)"></div>
                </div>

                <!-- 思维链展示区 (流式生成时) -->
                <div v-if="sending && thoughtProcess.length > 0" class="thought-chain">
                    <div class="thought-chain-header">
                        <span class="chain-icon">
                            <SettingOutlined :spin="true" />
                        </span>
                        <span>AI 正在思考...</span>
                    </div>
                    <div v-for="(step, idx) in thoughtProcess" :key="idx"
                        :class="['thought-item', step.type]">
                        <template v-if="step.type === 'thought'">
                            <span class="step-icon thought-icon">💭</span>
                            <div class="step-body">
                                <span class="step-content">{{ step.content }}</span>
                            </div>
                        </template>
                        <template v-else-if="step.type === 'tool_start'">
                            <span class="step-icon tool-icon">
                                <SettingOutlined :spin="true" />
                            </span>
                            <div class="step-body">
                                <span class="step-content">正在调用 <strong>{{ step.content }}</strong></span>
                                <span v-if="step.params" class="step-params">{{ step.params }}</span>
                            </div>
                        </template>
                        <template v-else-if="step.type === 'tool_end'">
                            <span class="step-icon done-icon">
                                <CheckOutlined />
                            </span>
                            <div class="step-body">
                                <span class="step-content">{{ step.content }}</span>
                                <span v-if="step.preview" class="step-preview">{{ step.preview }}</span>
                            </div>
                        </template>
                    </div>
                </div>

                <!-- 正在生成的回复 -->
                <div v-if="currentAiResponse" class="message assistant">
                    <div class="avatar">
                        <RobotOutlined />
                    </div>
                    <div class="content typing markdown-body">
                        <div v-html="renderMarkdown(currentAiResponse)"></div>
                        <span class="cursor">▌</span>
                    </div>
                </div>
            </div>

            <!-- 生成的 Prompt -->
            <div v-if="session?.promptGenerated" class="generated-prompt">
                <div class="prompt-header">
                    <CheckOutlined /> 已生成最终 Prompt
                </div>
                <pre class="prompt-content">{{ session.generatedPrompt }}</pre>
                <a-button type="primary" @click="handleConfirm" class="confirm-btn">
                    <CheckOutlined /> 确认并保存
                </a-button>
            </div>
        </div>

        <!-- 保存弹窗 -->
        <a-modal v-model:open="showSaveDialog" title="保存 Prompt" @ok="doSave" :confirmLoading="saving" okText="保存">
            <a-form layout="vertical">
                <a-form-item label="Prompt 名称" required>
                    <a-input v-model:value="savePromptName" placeholder="给你的 Prompt 起个名字" />
                </a-form-item>
                <a-form-item label="描述">
                    <a-input v-model:value="savePromptDesc" placeholder="简短描述（可选）" />
                </a-form-item>
            </a-form>
        </a-modal>

        <!-- 输入区域 -->
        <div class="input-area">
            <a-textarea ref="textareaRef" v-model:value="userInput"
                :placeholder="session ? '输入你的回复...' : '描述你想做什么，AI 会自动调用合适的工具'" :auto-size="{ minRows: 1, maxRows: 4 }"
                @keydown="handleKeydown" :disabled="sending" />
            <a-button type="primary" :loading="loading || sending" @click="session ? sendMessage() : startChat()">
                <SendOutlined />
            </a-button>
        </div>
    </div>
</template>

<style scoped>
.coach-container {
    display: flex;
    flex-direction: column;
    height: 100vh;
    background: var(--color-bg-primary);
    color: var(--color-text-primary);
}

.coach-header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: var(--space-4) var(--space-6);
    background: var(--color-bg-secondary);
    border-bottom: 1px solid var(--color-border-light);
}

.back-btn {
    color: var(--color-text-secondary);
}

.header-title {
    display: flex;
    align-items: center;
    gap: var(--space-2);
    font-size: var(--text-lg);
    font-weight: 600;
}

.header-title .anticon {
    font-size: var(--text-2xl);
    color: var(--color-primary);
}

.phase-badge {
    padding: var(--space-1) var(--space-3);
    background: var(--color-primary-muted);
    border: 1px solid var(--color-primary);
    border-radius: var(--radius-full);
    font-size: var(--text-xs);
    color: var(--color-primary);
}

.chat-area {
    flex: 1;
    overflow-y: auto;
    padding: var(--space-6);
}

/* 欢迎区域 */
.welcome-section {
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    height: 100%;
    text-align: center;
}

.welcome-icon {
    font-size: 48px;
    color: var(--color-primary);
    margin-bottom: var(--space-6);
}

.welcome-section h2 {
    font-size: var(--text-2xl);
    margin-bottom: var(--space-2);
}

.welcome-section p {
    color: var(--color-text-secondary);
    margin-bottom: var(--space-2);
}

.welcome-section .hint {
    font-size: var(--text-sm);
    color: var(--color-text-tertiary);
}

.provider-select {
    margin-top: var(--space-6);
    display: flex;
    align-items: center;
    gap: var(--space-3);
}

.coach-model-hint {
    margin-top: var(--space-3);
    font-size: var(--text-xs);
    color: var(--color-warning);
    display: flex;
    align-items: center;
    gap: 4px;
}

.coach-model-hint.success {
    color: var(--color-success);
}

/* Slash 命令提示 */
.slash-hint {
    margin-top: var(--space-4);
    font-size: var(--text-sm);
    color: var(--color-text-tertiary);
}

.slash-hint code {
    padding: 2px 6px;
    background: var(--color-bg-tertiary);
    border-radius: var(--radius-sm);
    font-family: monospace;
}

.selected-skill-hint {
    margin-top: var(--space-3);
    font-size: var(--text-sm);
    color: var(--color-primary);
}

.selected-skill-hint a {
    margin-left: var(--space-2);
    color: var(--color-text-tertiary);
    cursor: pointer;
}

.selected-skill-hint a:hover {
    color: var(--color-error);
}

/* Slash 菜单 */
.slash-menu {
    position: absolute;
    bottom: 100%;
    left: 0;
    right: 0;
    margin-bottom: 8px;
    background: var(--color-bg-secondary);
    border: 1px solid var(--color-border-light);
    border-radius: var(--radius-lg);
    box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
    max-height: 240px;
    overflow-y: auto;
    z-index: 100;
}

.slash-menu-header {
    padding: 8px 12px;
    font-size: var(--text-xs);
    color: var(--color-text-tertiary);
    border-bottom: 1px solid var(--color-border-light);
}

.slash-menu-item {
    padding: 10px 12px;
    display: flex;
    gap: 12px;
    cursor: pointer;
    transition: background 0.15s ease;
}

.slash-menu-item:hover {
    background: var(--color-bg-tertiary);
}

.slash-cmd {
    font-family: monospace;
    color: var(--color-primary);
    font-weight: 500;
}

.slash-desc {
    color: var(--color-text-secondary);
    font-size: var(--text-sm);
}

.slash-menu-empty {
    padding: 16px;
    text-align: center;
    color: var(--color-text-tertiary);
    font-size: var(--text-sm);
}

/* 消息样式 */
.messages {
    display: flex;
    flex-direction: column;
    gap: var(--space-4);
}

.message {
    display: flex;
    gap: var(--space-3);
    max-width: 80%;
    width: fit-content;
    /* Critical fix: Don't stretch */
}

.message.user {
    flex-direction: row-reverse;
    align-self: flex-end;
    margin-left: auto;
    /* Push to right */
}

.message.assistant {
    align-self: flex-start;
    margin-right: auto;
}

.avatar {
    width: 32px;
    height: 32px;
    border-radius: 50%;
    display: flex;
    align-items: center;
    justify-content: center;
    flex-shrink: 0;
    overflow: hidden;
}

.user-avatar-img {
    width: 100%;
    height: 100%;
    object-fit: cover;
}

.message.user .avatar {
    background: var(--color-primary);
}

.message.assistant .avatar {
    background: var(--color-bg-tertiary);
}

.content {
    padding: 12px 16px;
    border-radius: 12px;
    line-height: 1.6;
    font-size: 14px;
    -webkit-font-smoothing: antialiased;
    -moz-osx-font-smoothing: grayscale;
    box-shadow: 0 1px 2px rgba(0, 0, 0, 0.05);
    word-break: break-word;
    /* Ensure long text wraps */
    overflow-wrap: break-word;
    min-width: 0;
    /* Flexbox text overflow fix */
}

.message.user .content {
    background: #1677ff !important;
    /* Ant Design Blue - hardcoded safety */
    color: #ffffff !important;
    border-radius: 12px 12px 0 12px;
}

.message.user .content :deep(*) {
    color: #ffffff !important;
}

.message.assistant .content {
    background: #F0F2F5 !important;
    color: #000000 !important;
    border: 1px solid #d9d9d9 !important;
    border-radius: 12px 12px 12px 0;
}

/* 强制覆盖 markdown 内部所有元素的颜色 */
.message.assistant .content :deep(*) {
    color: #000000 !important;
}

.markdown-body {
    background-color: transparent !important;
    font-family: inherit !important;
}

.markdown-body :deep(pre) {
    background: rgba(0, 0, 0, 0.05) !important;
    border-radius: 6px;
}

.content pre {
    margin: 0;
    white-space: pre-wrap;
    font-family: inherit;
}

.markdown-body :deep(p) {
    margin-bottom: var(--space-2);
}

.markdown-body :deep(p:last-child) {
    margin-bottom: 0;
}

.markdown-body :deep(ul),
.markdown-body :deep(ol) {
    margin-bottom: var(--space-2);
    padding-left: var(--space-5);
}

.markdown-body :deep(code) {
    background: var(--color-bg-tertiary);
    padding: 2px 4px;
    border-radius: var(--radius-sm);
    font-family: var(--font-mono);
}

.typing .cursor {
    display: inline-block;
    width: 2px;
    height: 1em;
    background-color: currentColor;
    margin-left: 2px;
    animation: blink 1s infinite;
}

@keyframes blink {

    0%,
    50% {
        opacity: 1;
    }

    51%,
    100% {
        opacity: 0;
    }
}

/* 生成的 Prompt */
.generated-prompt {
    margin-top: var(--space-6);
    padding: var(--space-4);
    background: var(--color-primary-muted);
    border: 1px solid var(--color-primary);
    border-radius: var(--radius-lg);
}

.prompt-header {
    display: flex;
    align-items: center;
    gap: var(--space-2);
    color: var(--color-primary);
    font-weight: 600;
    margin-bottom: var(--space-3);
}

.prompt-content {
    background: var(--color-bg-tertiary);
    padding: var(--space-4);
    border-radius: var(--radius-md);
    white-space: pre-wrap;
    font-family: var(--font-mono);
    max-height: 300px;
    overflow-y: auto;
}

.confirm-btn {
    margin-top: var(--space-4);
}

/* 输入区域 */
.input-area {
    position: relative;
    display: flex;
    gap: var(--space-3);
    padding: var(--space-4) var(--space-6);
    background: var(--color-bg-secondary);
    border-top: 1px solid var(--color-border-light);
}

.input-area :deep(.ant-input) {
    background: var(--color-bg-tertiary);
    border: 1px solid var(--color-border-light);
    color: var(--color-text-primary);
    resize: none;
}

.input-area :deep(.ant-input:focus) {
    border-color: var(--color-primary);
}

.input-area :deep(.ant-btn) {
    height: auto;
    padding: var(--space-2) var(--space-4);
}

/* Thought Chain - Enhanced */
.thought-chain {
    margin-left: 44px;
    margin-bottom: var(--space-4);
    display: flex;
    flex-direction: column;
    gap: 10px;
    font-size: 13px;
    color: var(--color-text-secondary);
    max-width: 85%;
    background: linear-gradient(135deg, rgba(22, 119, 255, 0.03) 0%, rgba(22, 119, 255, 0.08) 100%);
    border-radius: 12px;
    padding: 16px;
    border: 1px solid rgba(22, 119, 255, 0.15);
}

.thought-chain-header {
    display: flex;
    align-items: center;
    gap: 8px;
    font-size: 14px;
    font-weight: 600;
    color: var(--color-primary);
    margin-bottom: 8px;
    padding-bottom: 10px;
    border-bottom: 1px solid var(--color-border-light);
}

.chain-icon {
    font-size: 18px;
}

.thought-item {
    display: flex;
    align-items: flex-start;
    gap: 10px;
    padding: 10px 14px;
    background: var(--color-bg-secondary);
    border-radius: 8px;
    border: 1px solid var(--color-border-light);
    animation: fadeIn 0.3s ease;
    transition: all 0.2s ease;
}

.thought-item.tool_start {
    border-left: 3px solid #faad14;
    background: rgba(250, 173, 20, 0.05);
}

.thought-item.tool_end {
    border-left: 3px solid #52c41a;
    background: rgba(82, 196, 26, 0.05);
}

.step-icon {
    font-size: 14px;
    flex-shrink: 0;
    margin-top: 2px;
    display: flex;
    align-items: center;
    justify-content: center;
    width: 20px;
    height: 20px;
}

.step-icon.thought-icon {
    color: var(--color-text-tertiary);
    font-size: 12px;
    font-weight: 500;
}

.step-icon.tool-icon {
    color: #faad14;
}

.step-icon.done-icon {
    color: #52c41a;
}

.step-body {
    display: flex;
    flex-direction: column;
    gap: 4px;
    flex: 1;
    min-width: 0;
}

.step-content {
    line-height: 1.5;
}

.step-content strong {
    color: var(--color-primary);
}

.step-params {
    font-size: 12px;
    color: var(--color-text-tertiary);
    font-family: var(--font-mono);
    background: var(--color-bg-tertiary);
    padding: 4px 8px;
    border-radius: 4px;
    max-width: 100%;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
}

.step-preview {
    font-size: 12px;
    color: #52c41a;
    font-style: italic;
    padding: 4px 8px;
    background: rgba(82, 196, 26, 0.1);
    border-radius: 4px;
    max-width: 100%;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
}

@keyframes spin {
    from {
        transform: rotate(0deg);
    }

    to {
        transform: rotate(360deg);
    }
}

@keyframes fadeIn {
    from {
        opacity: 0;
        transform: translateY(-5px);
    }

    to {
        opacity: 1;
        transform: translateY(0);
    }
}

/* Custom Modal & Sidebar Layout */
.modal-overlay {
    position: fixed;
    top: 0;
    left: 0;
    right: 0;
    bottom: 0;
    background: rgba(0, 0, 0, 0.5);
    z-index: 1000;
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
    display: flex;
    flex-direction: column;
}

.modal-content {
    background: white;
    border-radius: var(--radius-xl);
    width: 100%;
    /* Default width overridden by model-selector-dialog if applied */
    max-width: 600px;
    display: flex;
    flex-direction: column;
    box-shadow: var(--shadow-xl);
}

/* Ensure model-selector-dialog class overrides max-width/height */
.model-selector-dialog.modal-content {
    max-width: 90vw;
    width: 800px;
    max-height: 85vh;
}

.modal-header {
    padding: 20px 24px;
    border-bottom: 1px solid var(--color-border-light);
    display: flex;
    justify-content: space-between;
    align-items: center;
    flex-shrink: 0;
}

.modal-header h3 {
    margin: 0;
    font-size: 18px;
    font-weight: 600;
}

.close-btn {
    background: none;
    border: none;
    cursor: pointer;
    font-size: 18px;
    color: #999;
    padding: 4px;
    border-radius: 4px;
    transition: all 0.2s;
}

.close-btn:hover {
    background: #f5f5f5;
    color: #333;
}

.modal-body-layout {
    display: flex;
    flex: 1;
    overflow: hidden;
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
    flex-shrink: 0;
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
    background: rgba(0, 0, 0, 0.05);
    color: var(--color-text-primary);
}

.category-item.active {
    background: #fff;
    color: var(--color-primary);
    box-shadow: 0 1px 3px rgba(0, 0, 0, 0.05);
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

.provider-grid {
    display: grid;
    grid-template-columns: repeat(auto-fill, minmax(140px, 1fr));
    gap: 12px;
}

.provider-option {
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    padding: 16px;
    border: 1px solid var(--color-border);
    border-radius: var(--radius-lg);
    cursor: pointer;
    transition: all 0.2s;
    background: #fff;
    gap: 8px;
    text-align: center;
    min-height: 120px;
}

.provider-option:hover {
    border-color: var(--color-primary);
    transform: translateY(-2px);
    box-shadow: var(--shadow-md);
}

.provider-option.selected {
    border-color: var(--color-primary);
    background: var(--color-primary-muted);
    box-shadow: 0 0 0 2px var(--color-primary-muted);
}

.opt-name {
    margin-top: 8px;
    font-weight: 600;
    font-size: 14px;
    color: var(--color-text-primary);
    line-height: 1.2;
}

.opt-desc {
    margin-top: 4px;
    font-size: 12px;
    color: var(--color-text-tertiary);
}

.icon-wrapper.auto-icon {
    width: 32px;
    height: 32px;
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 24px;
    color: var(--color-text-primary);
    border: 1px solid var(--color-border);
    border-radius: 6px;
}


/* Styled Trigger */
.provider-select .label {
    font-size: 14px;
    color: var(--color-text-secondary);
}

.model-trigger {
    display: flex;
    align-items: center;
    gap: 10px;
    padding: 8px 16px;
    background: white;
    border: 1px solid var(--color-border);
    border-radius: var(--radius-lg);
    cursor: pointer;
    transition: all 0.2s;
    min-width: 180px;
}

.model-trigger:hover {
    border-color: var(--color-primary);
    box-shadow: var(--shadow-sm);
}

.model-trigger .model-name {
    flex: 1;
    font-weight: 500;
    font-size: 14px;
}

.model-trigger .arrow-icon {
    font-size: 12px;
    color: #999;
}

.no-model-hint {
    color: var(--color-danger);
    font-size: var(--text-xs);
}
</style>
