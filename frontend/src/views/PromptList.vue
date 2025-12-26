<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { getPrompts, createPrompt, deletePrompt, type Prompt } from '../api/prompt'
import { ElMessage, ElMessageBox } from 'element-plus'

const router = useRouter()
const loading = ref(false)
const prompts = ref<Prompt[]>([])
const showCreateDialog = ref(false)
const creating = ref(false)

const newPrompt = ref({
  name: '',
  description: '',
  content: ''
})

// 加载 Prompt 列表
const loadPrompts = async () => {
  loading.value = true
  try {
    const res = await getPrompts(1)
    if (res.code === 200) {
      prompts.value = res.data
    } else {
      ElMessage.error(res.message || '加载失败')
    }
  } catch (error: any) {
    ElMessage.error(error.response?.data?.message || '加载失败，请检查后端是否启动')
  } finally {
    loading.value = false
  }
}

// 创建 Prompt
const handleCreate = async () => {
  if (!newPrompt.value.name || !newPrompt.value.content) {
    ElMessage.warning('请填写名称和内容')
    return
  }

  creating.value = true
  try {
    const res = await createPrompt({
      name: newPrompt.value.name,
      description: newPrompt.value.description,
      content: newPrompt.value.content,
      workspaceId: 1
    })
    if (res.code === 200) {
      ElMessage.success('创建成功')
      showCreateDialog.value = false
      newPrompt.value = { name: '', description: '', content: '' }
      loadPrompts()
    } else {
      ElMessage.error(res.message || '创建失败')
    }
  } catch (error: any) {
    ElMessage.error(error.response?.data?.message || '创建失败')
  } finally {
    creating.value = false
  }
}

// 删除 Prompt
const handleDelete = async (prompt: Prompt) => {
  try {
    await ElMessageBox.confirm(`确定要删除 "${prompt.name}" 吗？`, '确认删除', {
      type: 'warning'
    })
    
    const res = await deletePrompt(prompt.id)
    if (res.code === 200) {
      ElMessage.success('删除成功')
      loadPrompts()
    } else {
      ElMessage.error(res.message || '删除失败')
    }
  } catch (error: any) {
    if (error !== 'cancel') {
      ElMessage.error(error.response?.data?.message || '删除失败')
    }
  }
}

// 查看版本历史
const viewVersions = (prompt: Prompt) => {
  router.push(`/prompts/${prompt.id}/versions`)
}

// 退出登录
const handleLogout = () => {
  localStorage.removeItem('token')
  localStorage.removeItem('user')
  router.push('/login')
}

// 获取当前用户
const currentUser = ref<any>(null)
try {
  const userStr = localStorage.getItem('user')
  if (userStr) {
    currentUser.value = JSON.parse(userStr)
  }
} catch {}

onMounted(() => {
  loadPrompts()
})
</script>

<template>
  <div class="page-container">
    <!-- Header -->
    <header class="header">
      <div class="header-left">
        <span class="logo-icon">⬡</span>
        <span class="logo-text">Prompt-Forge</span>
      </div>
      <div class="header-right">
        <span class="username">{{ currentUser?.username }}</span>
        <button class="logout-btn" @click="handleLogout">退出</button>
      </div>
    </header>

    <!-- Main Content -->
    <main class="main-content">
      <div class="page-header">
        <div>
          <h1 class="page-title">Prompt 库</h1>
          <p class="page-desc">管理和版本控制您的 Prompt 模板</p>
        </div>
        <button class="create-btn" @click="showCreateDialog = true">
          + 新建 Prompt
        </button>
      </div>

      <!-- Loading -->
      <div v-if="loading" class="loading">加载中...</div>

      <!-- Empty State -->
      <div v-else-if="prompts.length === 0" class="empty-state">
        <p>暂无 Prompt</p>
        <button class="create-btn" @click="showCreateDialog = true">创建第一个</button>
      </div>

      <!-- Prompt List -->
      <div v-else class="prompt-grid">
        <div 
          v-for="prompt in prompts" 
          :key="prompt.id" 
          class="prompt-card"
        >
          <div class="card-header">
            <span class="prompt-name">{{ prompt.name }}</span>
            <span class="version-badge">v{{ prompt.latestVersionNumber || 1 }}</span>
          </div>
          <p class="prompt-desc">{{ prompt.description || '暂无描述' }}</p>
          <div class="card-footer">
            <span class="date">{{ new Date(prompt.createdAt).toLocaleDateString() }}</span>
            <div class="actions">
              <button class="action-btn" @click="viewVersions(prompt)" title="版本历史">
                📜
              </button>
              <button class="action-btn danger" @click="handleDelete(prompt)" title="删除">
                🗑️
              </button>
            </div>
          </div>
        </div>
      </div>
    </main>

    <!-- Create Dialog -->
    <div v-if="showCreateDialog" class="dialog-overlay" @click.self="showCreateDialog = false">
      <div class="dialog">
        <h3>新建 Prompt</h3>
        <form @submit.prevent="handleCreate">
          <div class="form-group">
            <label>名称 *</label>
            <input v-model="newPrompt.name" type="text" placeholder="输入 Prompt 名称" />
          </div>
          <div class="form-group">
            <label>描述</label>
            <input v-model="newPrompt.description" type="text" placeholder="简短描述" />
          </div>
          <div class="form-group">
            <label>内容 *</label>
            <textarea 
              v-model="newPrompt.content" 
              rows="6" 
              placeholder="输入 Prompt 内容，支持 {{variable}} 变量"
            ></textarea>
          </div>
          <div class="dialog-actions">
            <button type="button" class="cancel-btn" @click="showCreateDialog = false">取消</button>
            <button type="submit" class="submit-btn" :disabled="creating">
              {{ creating ? '创建中...' : '创建' }}
            </button>
          </div>
        </form>
      </div>
    </div>
  </div>
</template>

<style scoped>
.page-container {
  min-height: 100vh;
  background: linear-gradient(135deg, #0a0a0f 0%, #1a1a2e 100%);
  color: #fff;
}

.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 32px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.1);
}

.header-left {
  display: flex;
  align-items: center;
  gap: 8px;
}

.logo-icon {
  font-size: 24px;
  color: #5e6ad2;
}

.logo-text {
  font-size: 18px;
  font-weight: 600;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 16px;
}

.username {
  color: #888;
}

.logout-btn {
  padding: 8px 16px;
  background: transparent;
  border: 1px solid rgba(255, 255, 255, 0.2);
  border-radius: 6px;
  color: #888;
  cursor: pointer;
  transition: all 0.2s;
}

.logout-btn:hover {
  border-color: #5e6ad2;
  color: #5e6ad2;
}

.main-content {
  max-width: 1200px;
  margin: 0 auto;
  padding: 32px;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 32px;
}

.page-title {
  font-size: 28px;
  font-weight: 600;
  margin-bottom: 8px;
}

.page-desc {
  color: #888;
}

.create-btn {
  padding: 12px 24px;
  background: #5e6ad2;
  border: none;
  border-radius: 8px;
  color: #fff;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: background 0.2s;
}

.create-btn:hover {
  background: #4c5bb5;
}

.loading, .empty-state {
  text-align: center;
  padding: 60px 20px;
  color: #888;
}

.prompt-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  gap: 20px;
}

.prompt-card {
  padding: 20px;
  background: rgba(255, 255, 255, 0.05);
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 12px;
  transition: all 0.2s;
}

.prompt-card:hover {
  border-color: rgba(255, 255, 255, 0.2);
  transform: translateY(-2px);
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.prompt-name {
  font-size: 16px;
  font-weight: 600;
}

.version-badge {
  padding: 4px 8px;
  background: #5e6ad2;
  border-radius: 4px;
  font-size: 12px;
}

.prompt-desc {
  color: #888;
  font-size: 14px;
  margin-bottom: 16px;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.card-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.date {
  font-size: 12px;
  color: #666;
}

.actions {
  display: flex;
  gap: 8px;
}

.action-btn {
  padding: 6px 10px;
  background: rgba(255, 255, 255, 0.1);
  border: none;
  border-radius: 6px;
  cursor: pointer;
  font-size: 14px;
  transition: background 0.2s;
}

.action-btn:hover {
  background: rgba(255, 255, 255, 0.2);
}

.action-btn.danger:hover {
  background: rgba(239, 68, 68, 0.3);
}

/* Dialog */
.dialog-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.7);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 100;
}

.dialog {
  width: 500px;
  padding: 24px;
  background: #1a1a2e;
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 12px;
}

.dialog h3 {
  margin-bottom: 20px;
  font-size: 18px;
}

.form-group {
  margin-bottom: 16px;
}

.form-group label {
  display: block;
  margin-bottom: 6px;
  font-size: 14px;
  color: #888;
}

.form-group input,
.form-group textarea {
  width: 100%;
  padding: 12px;
  background: rgba(0, 0, 0, 0.3);
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 8px;
  color: #fff;
  font-size: 14px;
  outline: none;
}

.form-group input:focus,
.form-group textarea:focus {
  border-color: #5e6ad2;
}

.dialog-actions {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  margin-top: 20px;
}

.cancel-btn {
  padding: 10px 20px;
  background: transparent;
  border: 1px solid rgba(255, 255, 255, 0.2);
  border-radius: 8px;
  color: #888;
  cursor: pointer;
}

.submit-btn {
  padding: 10px 20px;
  background: #5e6ad2;
  border: none;
  border-radius: 8px;
  color: #fff;
  cursor: pointer;
}

.submit-btn:disabled {
  opacity: 0.6;
}
</style>
