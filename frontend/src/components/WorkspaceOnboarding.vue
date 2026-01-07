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
  background: var(--color-bg-primary);
  z-index: 9999;
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--color-text-primary);
}

.onboarding-container {
  width: 100%;
  max-width: 500px;
  padding: var(--space-10);
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
  margin-bottom: var(--space-12);
}

.logo-icon {
  width: 80px;
  height: 80px;
  margin: 0 auto var(--space-6);
  background: var(--color-primary);
  border-radius: var(--radius-xl);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 40px;
  color: white;
}

.logo-section h1 {
  font-size: var(--text-2xl);
  font-weight: 700;
  margin-bottom: var(--space-2);
  color: var(--color-text-primary);
}

.subtitle {
  color: var(--color-text-secondary);
  font-size: var(--text-base);
}

.feature-list {
  margin-bottom: var(--space-12);
  text-align: left;
}

.feature-item {
  display: flex;
  align-items: center;
  gap: var(--space-4);
  padding: var(--space-4);
  margin-bottom: var(--space-3);
  background: var(--color-bg-secondary);
  border-radius: var(--radius-lg);
  transition: all var(--transition-fast);
}

.feature-item:hover {
  background: var(--color-bg-tertiary);
}

.feature-icon {
  width: 32px;
  height: 32px;
  background: var(--color-primary);
  color: white;
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
  font-size: var(--text-base);
  font-weight: 600;
  border-radius: var(--radius-lg);
}

/* 创建工作空间页 */
.step-header {
  margin-bottom: var(--space-10);
}

.step-icon {
  width: 64px;
  height: 64px;
  margin: 0 auto var(--space-5);
  background: var(--color-bg-secondary);
  border-radius: var(--radius-xl);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 32px;
  color: var(--color-text-secondary);
}

.step-header h2 {
  font-size: var(--text-xl);
  font-weight: 600;
  margin-bottom: var(--space-2);
  color: var(--color-text-primary);
}

.step-desc {
  color: var(--color-text-secondary);
}

.form-section {
  margin-bottom: var(--space-8);
}

.form-item {
  margin-bottom: var(--space-5);
  text-align: left;
}

.form-item label {
  display: block;
  margin-bottom: var(--space-2);
  font-size: var(--text-sm);
  color: var(--color-text-secondary);
}

.required {
  color: var(--color-danger);
}

.form-item :deep(.ant-input) {
  background: var(--color-bg-primary);
  border: 1px solid var(--color-border);
  color: var(--color-text-primary);
  border-radius: var(--radius-md);
}

.form-item :deep(.ant-input:focus) {
  border-color: var(--color-primary);
}

.form-item :deep(.ant-input::placeholder) {
  color: var(--color-text-tertiary);
}

.create-btn {
  width: 100%;
  height: 50px;
  font-size: var(--text-base);
  font-weight: 600;
  border-radius: var(--radius-lg);
}

/* 进度指示器 */
.progress-dots {
  display: flex;
  justify-content: center;
  gap: var(--space-2);
  margin-top: var(--space-10);
}

.dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: var(--color-border);
  transition: all var(--transition-base);
}

.dot.active {
  width: 24px;
  border-radius: var(--radius-sm);
  background: var(--color-primary);
}
</style>
