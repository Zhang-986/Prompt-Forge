<script setup lang="ts">
import { ref } from 'vue'
import { createWorkspace } from '../api/workspace'
import { message } from 'ant-design-vue'
import { FolderOutlined, RocketOutlined, ArrowRightOutlined } from '@ant-design/icons-vue'

const emit = defineEmits<{
  (e: 'created', workspaceId: number): void
}>()

const step = ref(1) // 1: 欢迎, 2: 创建工作空间
const workspaceName = ref('')
const workspaceDesc = ref('')
const creating = ref(false)

const goToCreate = () => {
  step.value = 2
}

const handleCreate = async () => {
  if (!workspaceName.value.trim()) {
    message.warning('请输入工作空间名称')
    return
  }

  creating.value = true
  try {
    const res = await createWorkspace({
      name: workspaceName.value.trim(),
      description: workspaceDesc.value.trim()
    })
    if (res.code === 200) {
      message.success('工作空间创建成功！')
      localStorage.setItem('currentWorkspaceId', String(res.data.id))
      emit('created', res.data.id)
    } else {
      message.error(res.message || '创建失败')
    }
  } catch (error: any) {
    message.error(error.response?.data?.message || '创建失败')
  } finally {
    creating.value = false
  }
}
</script>

<template>
  <div class="onboarding-overlay">
    <div class="onboarding-container">
      <!-- Step 1: 欢迎页 -->
      <div v-if="step === 1" class="step-content welcome-step">
        <div class="logo-section">
          <div class="logo-icon">
            <RocketOutlined />
          </div>
          <h1>欢迎使用 Prompt-Forge</h1>
          <p class="subtitle">让我们开始设置你的工作空间</p>
        </div>
        
        <div class="feature-list">
          <div class="feature-item">
            <span class="feature-icon">1</span>
            <span>创建工作空间来组织你的 Prompts</span>
          </div>
          <div class="feature-item">
            <span class="feature-icon">2</span>
            <span>使用版本控制管理 Prompt 迭代</span>
          </div>
          <div class="feature-item">
            <span class="feature-icon">3</span>
            <span>通过竞技场对比不同 AI 模型效果</span>
          </div>
        </div>

        <a-button type="primary" size="large" class="next-btn" @click="goToCreate">
          开始使用
          <template #icon>
            <ArrowRightOutlined />
          </template>
        </a-button>
      </div>

      <!-- Step 2: 创建工作空间 -->
      <div v-if="step === 2" class="step-content create-step">
        <div class="step-header">
          <div class="step-icon">
            <FolderOutlined />
          </div>
          <h2>创建你的第一个工作空间</h2>
          <p class="step-desc">工作空间用于组织和管理你的 Prompts</p>
        </div>

        <div class="form-section">
          <div class="form-item">
            <label>工作空间名称 <span class="required">*</span></label>
            <a-input 
              v-model:value="workspaceName" 
              placeholder="例如：我的项目、日常工作、学习笔记"
              size="large"
            />
          </div>
          <div class="form-item">
            <label>描述（可选）</label>
            <a-input 
              v-model:value="workspaceDesc" 
              placeholder="简单描述这个工作空间的用途"
              size="large"
            />
          </div>
        </div>

        <a-button 
          type="primary" 
          size="large" 
          class="create-btn" 
          :loading="creating"
          @click="handleCreate"
        >
          创建并开始使用
        </a-button>
      </div>

      <!-- 进度指示器 -->
      <div class="progress-dots">
        <span :class="['dot', { active: step === 1 }]"></span>
        <span :class="['dot', { active: step === 2 }]"></span>
      </div>
    </div>
  </div>
</template>

<style scoped>
.onboarding-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: linear-gradient(135deg, #0f0f1a 0%, #1a1a2e 50%, #16213e 100%);
  z-index: 9999;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
}

.onboarding-container {
  width: 100%;
  max-width: 500px;
  padding: 40px;
  text-align: center;
}

.step-content {
  animation: fadeIn 0.3s ease;
}

@keyframes fadeIn {
  from { opacity: 0; transform: translateY(20px); }
  to { opacity: 1; transform: translateY(0); }
}

/* 欢迎页 */
.logo-section {
  margin-bottom: 48px;
}

.logo-icon {
  width: 80px;
  height: 80px;
  margin: 0 auto 24px;
  background: linear-gradient(135deg, #5e6ad2, #8b5cf6);
  border-radius: 20px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 40px;
}

.logo-section h1 {
  font-size: 32px;
  font-weight: 700;
  margin-bottom: 8px;
  background: linear-gradient(135deg, #fff, #a5b4fc);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
}

.subtitle {
  color: rgba(255, 255, 255, 0.6);
  font-size: 16px;
}

.feature-list {
  margin-bottom: 48px;
  text-align: left;
}

.feature-item {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 16px;
  margin-bottom: 12px;
  background: rgba(255, 255, 255, 0.05);
  border-radius: 12px;
  transition: all 0.2s;
}

.feature-item:hover {
  background: rgba(255, 255, 255, 0.08);
}

.feature-icon {
  width: 32px;
  height: 32px;
  background: linear-gradient(135deg, #5e6ad2, #8b5cf6);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 600;
  flex-shrink: 0;
}

.next-btn {
  width: 100%;
  height: 50px;
  font-size: 16px;
  font-weight: 600;
  border-radius: 12px;
}

/* 创建工作空间页 */
.step-header {
  margin-bottom: 40px;
}

.step-icon {
  width: 64px;
  height: 64px;
  margin: 0 auto 20px;
  background: rgba(250, 204, 21, 0.2);
  border-radius: 16px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 32px;
  color: #facc15;
}

.step-header h2 {
  font-size: 24px;
  font-weight: 600;
  margin-bottom: 8px;
}

.step-desc {
  color: rgba(255, 255, 255, 0.6);
}

.form-section {
  margin-bottom: 32px;
}

.form-item {
  margin-bottom: 20px;
  text-align: left;
}

.form-item label {
  display: block;
  margin-bottom: 8px;
  font-size: 14px;
  color: rgba(255, 255, 255, 0.8);
}

.required {
  color: #ff6b6b;
}

.form-item :deep(.ant-input) {
  background: rgba(255, 255, 255, 0.1);
  border: 1px solid rgba(255, 255, 255, 0.1);
  color: #fff;
  border-radius: 10px;
}

.form-item :deep(.ant-input:focus) {
  border-color: #5e6ad2;
}

.form-item :deep(.ant-input::placeholder) {
  color: rgba(255, 255, 255, 0.4);
}

.create-btn {
  width: 100%;
  height: 50px;
  font-size: 16px;
  font-weight: 600;
  border-radius: 12px;
}

/* 进度指示器 */
.progress-dots {
  display: flex;
  justify-content: center;
  gap: 8px;
  margin-top: 40px;
}

.dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.2);
  transition: all 0.3s;
}

.dot.active {
  width: 24px;
  border-radius: 4px;
  background: #5e6ad2;
}
</style>
