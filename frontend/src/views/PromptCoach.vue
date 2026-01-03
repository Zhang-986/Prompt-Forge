<script setup lang="ts">
import { ref, nextTick, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import { SendOutlined, RobotOutlined, UserOutlined, CheckOutlined, ArrowLeftOutlined } from '@ant-design/icons-vue'
import { 
    startCoachSession, 
    sendCoachMessage, 
    getCoachSession, 
    type CoachSession
} from '../api/promptCoach'
import { getAvailableModels } from '../api/arena'

const router = useRouter()
// route 备用于后续功能

// 状态
const loading = ref(false)
const sending = ref(false)
const session = ref<CoachSession | null>(null)
const userInput = ref('')
const currentAiResponse = ref('')
const chatContainer = ref<HTMLElement | null>(null)

// 模型选择 - 从用户配置动态加载
const selectedProvider = ref('')
const availableProviders = ref<string[]>([])
const loadingProviders = ref(false)

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

// 页面加载时获取模型列表
onMounted(() => {
    loadProviders()
})

// 开始对话
const startChat = async () => {
    if (!userInput.value.trim()) {
        message.warning('请输入你的想法')
        return
    }

    loading.value = true
    try {
        const res = await startCoachSession({
            initialInput: userInput.value,
            provider: selectedProvider.value || undefined
        })

        if (res.code === 200) {
            session.value = res.data
            userInput.value = ''
            scrollToBottom()
        } else {
            message.error(res.message || '启动失败')
        }
    } catch (error: any) {
        message.error(error.response?.data?.message || '启动失败')
    } finally {
        loading.value = false
    }
}

// 发送消息
const sendMessage = async () => {
    if (!userInput.value.trim() || !session.value) return
    if (sending.value) return

    const userMessage = userInput.value
    userInput.value = ''
    sending.value = true
    currentAiResponse.value = ''

    // 先添加用户消息到界面
    session.value.history.push({
        role: 'user',
        content: userMessage,
        timestamp: new Date().toISOString()
    })
    scrollToBottom()

    try {
        await sendCoachMessage(
            { sessionId: session.value.sessionId, message: userMessage },
            // onChunk
            (chunk) => {
                currentAiResponse.value += chunk
                scrollToBottom()
            },
            // onComplete
            async () => {
                // 流式完成后，添加 AI 回复到历史
                session.value!.history.push({
                    role: 'assistant',
                    content: currentAiResponse.value,
                    timestamp: new Date().toISOString()
                })
                currentAiResponse.value = ''
                sending.value = false

                // 刷新会话状态
                const res = await getCoachSession(session.value!.sessionId)
                if (res.code === 200) {
                    session.value = res.data
                }
                scrollToBottom()
            },
            // onError
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
        <!-- 头部 -->
        <div class="coach-header">
            <a-button type="text" @click="goBack" class="back-btn">
                <ArrowLeftOutlined /> 返回
            </a-button>
            <div class="header-title">
                <RobotOutlined />
                <span>Prompt 教练</span>
            </div>
            <div class="header-right">
                <span v-if="session" class="phase-badge">{{ session.phaseDescription }}</span>
            </div>
        </div>

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
                    <span>选择 AI 模型：</span>
                    <a-select 
                        v-model:value="selectedProvider" 
                        placeholder="自动选择" 
                        style="width: 180px"
                        :loading="loadingProviders"
                    >
                        <a-select-option value="">自动选择</a-select-option>
                        <a-select-option v-for="provider in availableProviders" :key="provider" :value="provider">
                            {{ provider }}
                        </a-select-option>
                    </a-select>
                    <span v-if="availableProviders.length === 0 && !loadingProviders" class="no-model-hint">
                        请先配置模型
                    </span>
                </div>
            </div>

            <!-- 对话消息 -->
            <div v-if="session" class="messages">
                <div 
                    v-for="(turn, index) in session.history" 
                    :key="index"
                    :class="['message', turn.role]"
                >
                    <div class="avatar">
                        <UserOutlined v-if="turn.role === 'user'" />
                        <RobotOutlined v-else />
                    </div>
                    <div class="content">
                        <pre>{{ turn.content }}</pre>
                    </div>
                </div>

                <!-- 正在生成的回复 -->
                <div v-if="currentAiResponse" class="message assistant">
                    <div class="avatar">
                        <RobotOutlined />
                    </div>
                    <div class="content typing">
                        <pre>{{ currentAiResponse }}</pre>
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
        <a-modal 
            v-model:open="showSaveDialog" 
            title="保存 Prompt" 
            @ok="doSave"
            :confirmLoading="saving"
            okText="保存"
        >
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
            <a-textarea 
                v-model:value="userInput" 
                :placeholder="session ? '输入你的回复...' : '描述你想做什么...'"
                :auto-size="{ minRows: 1, maxRows: 4 }"
                @keydown="handleKeydown"
                :disabled="sending"
            />
            <a-button 
                type="primary" 
                :loading="loading || sending"
                @click="session ? sendMessage() : startChat()"
            >
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
    background: linear-gradient(135deg, #0f0f1a 0%, #1a1a2e 100%);
    color: #fff;
}

.coach-header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: 16px 24px;
    background: rgba(26, 26, 46, 0.9);
    border-bottom: 1px solid rgba(255, 255, 255, 0.1);
}

.back-btn {
    color: rgba(255, 255, 255, 0.7);
}

.header-title {
    display: flex;
    align-items: center;
    gap: 8px;
    font-size: 18px;
    font-weight: 600;
}

.header-title .anticon {
    font-size: 24px;
    color: #5e6ad2;
}

.phase-badge {
    padding: 4px 12px;
    background: rgba(94, 106, 210, 0.2);
    border: 1px solid rgba(94, 106, 210, 0.4);
    border-radius: 16px;
    font-size: 12px;
    color: #5e6ad2;
}

.chat-area {
    flex: 1;
    overflow-y: auto;
    padding: 24px;
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
    font-size: 64px;
    color: #5e6ad2;
    margin-bottom: 24px;
}

.welcome-section h2 {
    font-size: 28px;
    margin-bottom: 8px;
}

.welcome-section p {
    color: rgba(255, 255, 255, 0.6);
    margin-bottom: 8px;
}

.welcome-section .hint {
    font-size: 14px;
    color: rgba(255, 255, 255, 0.4);
}

.provider-select {
    margin-top: 24px;
    display: flex;
    align-items: center;
    gap: 12px;
}

/* 消息样式 */
.messages {
    display: flex;
    flex-direction: column;
    gap: 16px;
}

.message {
    display: flex;
    gap: 12px;
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
    width: 36px;
    height: 36px;
    border-radius: 50%;
    display: flex;
    align-items: center;
    justify-content: center;
    flex-shrink: 0;
}

.message.user .avatar {
    background: #5e6ad2;
}

.message.assistant .avatar {
    background: rgba(255, 255, 255, 0.1);
}

.content {
    padding: 12px 16px;
    border-radius: 12px;
    line-height: 1.6;
}

.message.user .content {
    background: #5e6ad2;
    border-radius: 12px 12px 0 12px;
}

.message.assistant .content {
    background: rgba(255, 255, 255, 0.1);
    border-radius: 12px 12px 12px 0;
}

.content pre {
    margin: 0;
    white-space: pre-wrap;
    font-family: inherit;
}

.typing .cursor {
    animation: blink 1s infinite;
}

@keyframes blink {
    0%, 50% { opacity: 1; }
    51%, 100% { opacity: 0; }
}

/* 生成的 Prompt */
.generated-prompt {
    margin-top: 24px;
    padding: 16px;
    background: rgba(94, 106, 210, 0.1);
    border: 1px solid rgba(94, 106, 210, 0.3);
    border-radius: 12px;
}

.prompt-header {
    display: flex;
    align-items: center;
    gap: 8px;
    color: #5e6ad2;
    font-weight: 600;
    margin-bottom: 12px;
}

.prompt-content {
    background: rgba(0, 0, 0, 0.3);
    padding: 16px;
    border-radius: 8px;
    white-space: pre-wrap;
    font-family: monospace;
    max-height: 300px;
    overflow-y: auto;
}

.confirm-btn {
    margin-top: 16px;
}

/* 输入区域 */
.input-area {
    display: flex;
    gap: 12px;
    padding: 16px 24px;
    background: rgba(26, 26, 46, 0.9);
    border-top: 1px solid rgba(255, 255, 255, 0.1);
}

.input-area :deep(.ant-input) {
    background: rgba(255, 255, 255, 0.1);
    border: 1px solid rgba(255, 255, 255, 0.1);
    color: #fff;
    resize: none;
}

.input-area :deep(.ant-input:focus) {
    border-color: #5e6ad2;
}

.input-area :deep(.ant-btn) {
    height: auto;
    padding: 8px 16px;
}

.no-model-hint {
    color: #ff7875;
    font-size: 12px;
}
</style>
