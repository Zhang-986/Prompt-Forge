<script setup lang="ts">
import { ref, onMounted, watch } from 'vue'
import { getWorkspaceMembers, addWorkspaceMember, removeWorkspaceMember, updateMemberRole, type WorkspaceMember } from '../api/workspace'
import { searchUser } from '../api/user'
import { message, Modal } from 'ant-design-vue'
import { DeleteOutlined } from '@ant-design/icons-vue'

const props = defineProps<{
  workspaceId: number
  visible: boolean
}>()

const emit = defineEmits<{
  (e: 'update:visible', value: boolean): void
  (e: 'close'): void
}>()

const loading = ref(false)
const members = ref<WorkspaceMember[]>([])
const searchLoading = ref(false)
const searchUsername = ref('')
const searchResult = ref<any>(null)
const selectedRole = ref<'ADMIN' | 'MEMBER' | 'VIEWER'>('MEMBER')

// 加载成员列表
const loadMembers = async () => {
  if (!props.workspaceId) return
  
  loading.value = true
  try {
    const res = await getWorkspaceMembers(props.workspaceId)
    if (res.code === 200) {
      members.value = res.data
    }
  } catch (error: any) {
    console.error('加载成员失败:', error)
  } finally {
    loading.value = false
  }
}

// 搜索用户
const handleSearch = async () => {
  if (!searchUsername.value.trim()) {
    message.warning('请输入用户名')
    return
  }
  
  searchLoading.value = true
  searchResult.value = null
  try {
    const res = await searchUser(searchUsername.value.trim())
    if (res.code === 200) {
      // 检查是否已经是成员
      const exists = members.value.some(m => m.userId === res.data.id)
      if (exists) {
        message.warning('该用户已经是成员')
        return
      }
      searchResult.value = res.data
    } else {
      message.error(res.message || '未找到用户')
    }
  } catch (error: any) {
    message.error(error.response?.data?.message || '搜索失败')
  } finally {
    searchLoading.value = false
  }
}

// 邀请成员
const handleInvite = async () => {
  if (!searchResult.value) return
  
  try {
    const res = await addWorkspaceMember(props.workspaceId, {
      userId: searchResult.value.id,
      role: selectedRole.value
    })
    if (res.code === 200) {
      message.success('邀请成功')
      searchUsername.value = ''
      searchResult.value = null
      await loadMembers()
    } else {
      message.error(res.message || '邀请失败')
    }
  } catch (error: any) {
    message.error(error.response?.data?.message || '邀请失败')
  }
}

// 移除成员
const handleRemove = (member: WorkspaceMember) => {
  Modal.confirm({
    title: '确认移除',
    content: `确定要移除成员 "${member.username}" 吗？`,
    okType: 'danger',
    onOk: async () => {
      const res = await removeWorkspaceMember(props.workspaceId, member.userId)
      if (res.code === 200) {
        message.success('移除成功')
        await loadMembers()
      } else {
        message.error(res.message || '移除失败')
      }
    }
  })
}

// 修改成员角色
const handleRoleChange = async (member: WorkspaceMember, newRole: 'ADMIN' | 'MEMBER' | 'VIEWER') => {
  if (member.role === newRole) return
  
  try {
    const res = await updateMemberRole(props.workspaceId, member.userId, newRole)
    if (res.code === 200) {
      message.success('角色更新成功')
      await loadMembers()
    } else {
      message.error(res.message || '更新失败')
    }
  } catch (error: any) {
    message.error(error.response?.data?.message || '更新失败')
  }
}

// 关闭对话框
const handleClose = () => {
  emit('update:visible', false)
  emit('close')
}

// getRoleColor 用于显示角色颜色

// 获取角色颜色
const getRoleColor = (role: string) => {
  const colorMap: Record<string, string> = {
    'ADMIN': '#e74c3c',
    'MEMBER': '#3498db',
    'VIEWER': '#95a5a6'
  }
  return colorMap[role] || '#95a5a6'
}

watch(() => props.visible, (val) => {
  if (val) {
    loadMembers()
  }
})

onMounted(() => {
  if (props.visible) {
    loadMembers()
  }
})
</script>

<template>
  <div v-if="visible" class="dialog-overlay" @click.self="handleClose">
    <div class="dialog member-dialog">
      <div class="dialog-header">
        <h3>成员管理</h3>
        <button class="close-btn" @click="handleClose">×</button>
      </div>
      
      <!-- 邀请成员 -->
      <div class="invite-section">
        <h4>邀请新成员</h4>
        <div class="invite-form">
          <input 
            v-model="searchUsername" 
            type="text" 
            placeholder="输入用户名搜索" 
            @keyup.enter="handleSearch"
          />
          <button class="search-btn" @click="handleSearch" :disabled="searchLoading">
            {{ searchLoading ? '搜索中...' : '搜索' }}
          </button>
        </div>
        
        <!-- 搜索结果 -->
        <div v-if="searchResult" class="search-result">
          <div class="user-info">
            <span class="username">{{ searchResult.username }}</span>
            <span class="email">{{ searchResult.email }}</span>
          </div>
          <select v-model="selectedRole" class="role-select">
            <option value="ADMIN">管理员</option>
            <option value="MEMBER">成员</option>
            <option value="VIEWER">观察者</option>
          </select>
          <button class="invite-btn" @click="handleInvite">邀请</button>
        </div>
      </div>
      
      <!-- 成员列表 -->
      <div class="members-section">
        <h4>当前成员 ({{ members.length }})</h4>
        <div v-if="loading" class="loading">加载中...</div>
        <div v-else class="member-list">
          <div v-for="member in members" :key="member.id" class="member-item">
            <div class="member-info">
              <span class="member-name">{{ member.username }}</span>
              <select 
                class="role-select-inline" 
                :value="member.role"
                @change="handleRoleChange(member, ($event.target as HTMLSelectElement).value as any)"
                :style="{ color: getRoleColor(member.role) }"
              >
                <option value="ADMIN">管理员</option>
                <option value="MEMBER">成员</option>
                <option value="VIEWER">观察者</option>
              </select>
            </div>
            <button 
              v-if="member.role !== 'ADMIN'" 
              class="remove-btn" 
              @click="handleRemove(member)"
              title="移除成员"
            >
              <DeleteOutlined />
            </button>
          </div>
          <div v-if="members.length === 0" class="empty">暂无成员</div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.dialog-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
}

.member-dialog {
  width: 500px;
  max-height: 80vh;
  overflow-y: auto;
  padding: var(--space-6);
  background: var(--color-bg-primary);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
}

.dialog-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: var(--space-6);
}

.dialog-header h3 {
  font-size: var(--text-lg);
  color: var(--color-text-primary);
  margin: 0;
}

.close-btn {
  background: transparent;
  border: none;
  color: var(--color-text-tertiary);
  font-size: var(--text-2xl);
  cursor: pointer;
  padding: 0 var(--space-2);
}

.close-btn:hover {
  color: var(--color-text-primary);
}

.invite-section, .members-section {
  margin-bottom: var(--space-6);
}

.invite-section h4, .members-section h4 {
  font-size: var(--text-sm);
  color: var(--color-text-secondary);
  margin-bottom: var(--space-3);
}

.invite-form {
  display: flex;
  gap: var(--space-2);
}

.invite-form input {
  flex: 1;
  padding: var(--space-3);
  background: var(--color-bg-primary);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  color: var(--color-text-primary);
  font-size: var(--text-sm);
  outline: none;
}

.invite-form input:focus {
  border-color: var(--color-primary);
}

.search-btn {
  padding: var(--space-3) var(--space-4);
  background: var(--color-primary);
  border: none;
  border-radius: var(--radius-md);
  color: #fff;
  cursor: pointer;
  white-space: nowrap;
}

.search-btn:disabled {
  opacity: 0.6;
}

.search-result {
  display: flex;
  align-items: center;
  gap: var(--space-3);
  margin-top: var(--space-3);
  padding: var(--space-3);
  background: var(--color-bg-secondary);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
}

.user-info {
  flex: 1;
}

.user-info .username {
  font-weight: 500;
  color: var(--color-text-primary);
}

.user-info .email {
  font-size: var(--text-xs);
  color: var(--color-text-tertiary);
  margin-left: var(--space-2);
}

.role-select {
  padding: var(--space-2) var(--space-3);
  background: var(--color-bg-primary);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-sm);
  color: var(--color-text-primary);
  font-size: var(--text-xs);
}

.invite-btn {
  padding: var(--space-2) var(--space-4);
  background: var(--color-primary);
  border: none;
  border-radius: var(--radius-sm);
  color: #fff;
  cursor: pointer;
}

.member-list {
  display: flex;
  flex-direction: column;
  gap: var(--space-2);
}

.member-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: var(--space-3);
  background: var(--color-bg-secondary);
  border-radius: var(--radius-md);
}

.member-info {
  display: flex;
  align-items: center;
  gap: var(--space-3);
}

.member-name {
  font-weight: 500;
  color: var(--color-text-primary);
}

.member-role {
  font-size: var(--text-xs);
  padding: var(--space-1) var(--space-2);
  background: var(--color-bg-tertiary);
  border-radius: var(--radius-sm);
}

.role-select-inline {
  padding: var(--space-1) var(--space-2);
  background: var(--color-bg-primary);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-sm);
  font-size: var(--text-xs);
  cursor: pointer;
  transition: all var(--transition-fast);
}

.role-select-inline:hover {
  border-color: var(--color-primary);
}

.role-select-inline option {
  background: var(--color-bg-primary);
  color: var(--color-text-primary);
}

.remove-btn {
  padding: var(--space-1) var(--space-2);
  background: transparent;
  border: none;
  color: var(--color-danger);
  cursor: pointer;
  opacity: 0.6;
  transition: opacity var(--transition-fast);
}

.remove-btn:hover {
  opacity: 1;
}

.loading, .empty {
  text-align: center;
  padding: var(--space-6);
  color: var(--color-text-tertiary);
}
</style>
