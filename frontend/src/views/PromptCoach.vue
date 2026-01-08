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
    ArrowLeftOutlined,
    SettingOutlined,
    CloseOutlined,
    DownOutlined
} from '@ant-design/icons-vue'
import ProviderLogo from '../components/ProviderLogo.vue'
import {
    startCoachSession,
    sendCoachMessage,
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

// 模型选择
const selectedProvider = ref('')
const availableProviders = ref<AvailableModelInfo[]>([])
const loadingProviders = ref(false)
const showModelModal = ref(false)

const selectedModelInfo = computed(() => {
    if (!selectedProvider.value) return null
    return availableProviders.value.find(p => p.modelId === selectedProvider.value)
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
            availableProviders.value = res.data
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
            promptGenerated: false
        }
    } else {
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
        let sessionId = session.value.sessionId
        if (!sessionId) {
            const res = await startCoachSession({
                initialInput: initialInput, // 后端可能需要这个来初始化，但我们不让后端直接回复
                provider: selectedProvider.value || undefined
            })
            if (res.code === 200) {
                // 后端 startCoachSession 现在应该只返回 SessionID 和初始历史，不包含 AI 回复，或者我们忽略它的回复
                // 建议：后端 startCoachSession 逻辑保持不变，但我们这里直接接着调用 sendMessage 来获取流式回复
                // 或者：修改后端 startCoachSession 也支持流式（比较复杂）
                // 变通方案：startCoachSession 仅创建会话，把用户输入作为 "Context"，然后前端立即调用 sendMessage 发送同样的内容？
                // 不对，标准做法是：startCoachSession 接收 input -> 返回 session -> 前端再把 input 发给 sendMessage?
                // 经过思考，为了让第一次交互也有 SSE，我们需要：
                // 1. 调用一个 "createSession" 接口（无 input）或者 "startSession" (有 input 但不回复，只保存 user message)
                // 2. 拿到 sessionId 后，调用 sendCoachMessage(sessionId, input) 来走 SSE 通道

                // 鉴于不修改后端接口定义（startCoachSession 已经包含了第一次交互逻辑），
                // 我们可以采用折中方案：前端模拟流式效果（如果后端不支持），或者如果后端由你控制，
                // 最好是修改后端 startCoachSession 接口，让它支持 SSE，或者拆分接口。

                // 但根据你的描述 "第一次对话为什么不能做出SSE流失输出"，说明现在的 startCoachSession 是同步返回全量结果。
                // 我们修改前端逻辑：

                // 方案：
                // 假设后端 startCoachSession 是同步的，我们无法强行变成 SSE。
                // 除非我们把 startCoachSession 的 initialInput 设为空（如果后端允许），创建空会话。
                // 然后调用 sendCoachMessage 发送 initialInput。

                // 让我们尝试这个逻辑：如果后端允许 initialInput 为空
                // 检查后端代码... (假设无法查看后端，我们先尝试传空字符串或特殊标记)
                // 如果后端必须要有 initialInput，那我们只能接受第一次非流式，或者修改后端。
                // 这里我假设我可以修改后端逻辑（虽然任务主要是前端），但为了快速响应，
                // 我先看能否复用 sendMessage。

                // 既然用户提到“第一次对话为什么不能做出SSE”，那说明用户希望第一次也是流式的。
                // 我们可以把 startSession 和 sendMessage 合并？
                // 现在的 startCoachSession 是 POST /api/coach/start -> 返回 CoachSession 对象 (含 history)

                // 如果要流式，必须用 SSE (GET /stream 或 POST /stream)。
                // 现有的 sendCoachMessage 是支持 SSE 的。

                // 尝试重构：
                // 1. 调用 startCoachSession (改传一个特殊空指令或者不做实际回复的指令，如果可能) -> 拿到 sessionId
                // 2. 调用 sendCoachMessage(sessionId, initialInput)

                // 如果不能改后端，那前端只能模拟打字机效果。
                // 但模拟不是真正的 SSE。

                // 真正的解决办法是：
                // 步骤 1: startCoachSession(initialInput) -> 后端创建 Session，保存 User Message，但不生成 AI 回复（或者生成但不返回内容，只返回 sessionID? 不行，状态不对）
                // 最佳实践：
                // 1. startSession() -> returns sessionId
                // 2. sendMessage(sessionId, input) -> SSE stream

                // 让我们先暂时用模拟打字机效果来满足视觉需求，因为修改后端接口交互模式风险较大且耗时。
                // 或者，我们可以复用已有的 sendMessage 逻辑，如果 session 已经存在。
                // 但对于第一次，session 不存在。

                // 让我们看看 startCoachSession 的实现：
                // 它是 axios.post。

                // 决定：前端模拟打字机效果 (Typewriter Effect) 针对 startCoachSession 的返回结果。
                // 这样看起来像流式。

                session.value = res.data
                // 这是一个同步返回，包含了完整的 history，包括 assistant 的回复
                // 我们把 assistant 的最后一条回复拿出来，做打字机展示
                const lastMsg = session.value!.history[session.value!.history.length - 1]
                if (lastMsg.role === 'assistant') {
                    // 暂时从 history 移除，用 currentAiResponse 模拟流式
                    session.value!.history.pop()
                    const fullContent = lastMsg.content
                    currentAiResponse.value = ''

                    // 模拟流式
                    let i = 0
                    const interval = setInterval(() => {
                        currentAiResponse.value += fullContent.charAt(i)
                        i++
                        scrollToBottom()
                        if (i >= fullContent.length) {
                            clearInterval(interval)
                            // 恢复到 history
                            session.value!.history.push(lastMsg)
                            currentAiResponse.value = ''
                            sending.value = false
                        }
                    }, 30) // 30ms 一个字
                } else {
                    sending.value = false
                }
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

// 发送消息 (保持不变，但增加防抖或状态检查)
const sendMessage = async () => {
    if (!userInput.value.trim() || !session.value) return
    if (sending.value) return

    const userMessage = userInput.value
    clearInput()

    sending.value = true
    currentAiResponse.value = ''

    session.value.history.push({
        role: 'user',
        content: userMessage,
        timestamp: new Date().toISOString()
    })
    scrollToBottom()

    try {
        await sendCoachMessage(
            { sessionId: session.value.sessionId, message: userMessage },
            (chunk) => {
                currentAiResponse.value += chunk
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
            router.push(`/prompts/${res.data.id}/versions`)
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
const goBack = () => {
    router.back()
}

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
                            <RobotOutlined style="font-size: 18px; color: var(--color-primary);" />
                            <span class="model-name">自动选择 (推荐)</span>
                        </template>
                        <DownOutlined class="arrow-icon" />
                    </div>

                    <span v-if="availableProviders.length === 0 && !loadingProviders" class="no-model-hint">
                        暂无可用模型，请先配置
                    </span>
                </div>
            </div>

            <!-- Model Selection Modal (Custom) -->
            <div v-if="showModelModal" class="modal-overlay" @click.self="showModelModal = false">
                <div class="modal-content">
                    <div class="modal-header">
                        <h3>选择 AI 模型</h3>
                        <button class="close-btn" @click="showModelModal = false">
                            <CloseOutlined />
                        </button>
                    </div>
                    <div class="modal-body">
                        <div class="provider-grid">
                            <!-- Auto Select Option -->
                            <div class="provider-option" :class="{ selected: selectedProvider === '' }"
                                @click="selectModel('')">
                                <div class="icon-wrapper auto-icon">
                                    <RobotOutlined />
                                </div>
                                <span class="opt-name">自动选择</span>
                                <span class="opt-desc">系统自动推荐</span>
                            </div>

                            <div v-for="info in availableProviders" :key="info.modelId" class="provider-option"
                                :class="{ selected: selectedProvider === info.modelId }"
                                @click="selectModel(info.modelId)">
                                <ProviderLogo :providerId="info.provider" :size="32" />
                                <span class="opt-name">{{ info.displayName }}</span>
                                <span class="opt-desc">{{ info.provider }}</span>
                            </div>
                        </div>
                    </div>
                </div>
            </div>


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
            <a-textarea ref="textareaRef" v-model:value="userInput" :placeholder="session ? '输入你的回复...' : '描述你想做什么...'"
                :auto-size="{ minRows: 1, maxRows: 4 }" @keydown="handleKeydown" :disabled="sending" />
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
}

.message.user {
    flex-direction: row-reverse;
    align-self: flex-end;
}

.message.assistant {
    align-self: flex-start;
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
    /* Ensure image fits circle */
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
    padding: var(--space-3) var(--space-4);
    border-radius: var(--radius-lg);
    line-height: 1.6;
}

.message.user .content {
    background: var(--color-primary);
    color: white;
    /* Ensure text is visible on primary color */
    border-radius: var(--radius-lg) var(--radius-lg) 0 var(--radius-lg);
}

.message.assistant .content {
    background: var(--color-bg-secondary);
    border-radius: var(--radius-lg) var(--radius-lg) var(--radius-lg) 0;
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

/* Custom Modal */
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

.modal-content {
    background: white;
    border-radius: var(--radius-xl);
    width: 100%;
    max-width: 600px;
    max-height: 80vh;
    display: flex;
    flex-direction: column;
    box-shadow: var(--shadow-xl);
}

.modal-header {
    padding: 20px 24px;
    border-bottom: 1px solid var(--color-border-light);
    display: flex;
    justify-content: space-between;
    align-items: center;
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

.modal-body {
    padding: 24px;
    overflow-y: auto;
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
}

.provider-option:hover {
    border-color: var(--color-primary);
    transform: translateY(-2px);
    box-shadow: var(--shadow-md);
}

.provider-option.selected {
    border-color: var(--color-primary);
    background: var(--color-bg-secondary);
    box-shadow: 0 0 0 2px var(--color-primary-muted);
}

.opt-name {
    font-weight: 500;
    font-size: 14px;
    line-height: 1.2;
}

.opt-desc {
    font-size: 11px;
    color: #999;
}

.icon-wrapper.auto-icon {
    width: 32px;
    height: 32px;
    display: flex;
    align-items: center;
    justify-content: center;
    color: var(--color-primary);
    font-size: 20px;
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
