<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { getWorkspaces, createWorkspace, updateWorkspace, deleteWorkspace, type Workspace } from '../api/workspace'
import { ElMessage, ElMessageBox } from 'element-plus'
import WorkspaceMemberManager from './WorkspaceMemberManager.vue'

const props = defineProps<{
  modelValue: number
}>()

const emit = defineEmits<{
  (e: 'update:modelValue', value: number): void
  (e: 'change', workspace: Workspace): void
}>()

const loading = ref(false)
const workspaces = ref<Workspace[]>([])
const showCreateDialog = ref(false)
const showEditDialog = ref(false)
const creating = ref(false)
const editing = ref(false)
const newWorkspace = ref({ name: '', description: '' })
const editingWorkspace = ref<Workspace | null>(null)
const editForm = ref({ name: '', description: '' })
const showMemberDialog = ref(false)
const memberWorkspaceId = ref(0)

// 打开成员管理对话框
const openMemberDialog = (ws: Workspace, event: Event) => {
  event.stopPropagation()
  memberWorkspaceId.value = ws.id
  showMemberDialog.value = true
}

// 加载工作空间列表
const loadWorkspaces = async () => {
  loading.value = true
  try {
    const res = await getWorkspaces()
    if (res.code === 200) {
      workspaces.value = res.data
      // 如果当前没有选中，默认选中第一个
      if (workspaces.value.length > 0 && !props.modelValue) {
        emit('update:modelValue', workspaces.value[0].id)
        emit('change', workspaces.value[0])
      }
    }
  } catch (error: any) {
    console.error('加载工作空间失败:', error)
  } finally {
    loading.value = false
  }
}

// 选择工作空间
const selectWorkspace = (id: number) => {
  emit('update:modelValue', id)
  const workspace = workspaces.value.find(w => w.id === id)
  if (workspace) {
    emit('change', workspace)
  }
}

// 创建工作空间
const handleCreate = async () => {
  if (!newWorkspace.value.name) {
    ElMessage.warning('请输入工作空间名称')
    return
  }

  creating.value = true
  try {
    const res = await createWorkspace(newWorkspace.value)
    if (res.code === 200) {
      ElMessage.success('创建成功')
      showCreateDialog.value = false
      newWorkspace.value = { name: '', description: '' }
      await loadWorkspaces()
      selectWorkspace(res.data.id)
    } else {
      ElMessage.error(res.message || '创建失败')
    }
  } catch (error: any) {
    ElMessage.error(error.response?.data?.message || '创建失败')
  } finally {
    creating.value = false
  }
}

// 打开编辑对话框
const openEditDialog = (ws: Workspace, event: Event) => {
  event.stopPropagation()
  editingWorkspace.value = ws
  editForm.value = { name: ws.name, description: ws.description || '' }
  showEditDialog.value = true
}

// 编辑工作空间
const handleEdit = async () => {
  if (!editForm.value.name) {
    ElMessage.warning('请输入工作空间名称')
    return
  }
  if (!editingWorkspace.value) return

  editing.value = true
  try {
    const res = await updateWorkspace(editingWorkspace.value.id, editForm.value)
    if (res.code === 200) {
      ElMessage.success('更新成功')
      showEditDialog.value = false
      await loadWorkspaces()
    } else {
      ElMessage.error(res.message || '更新失败')
    }
  } catch (error: any) {
    ElMessage.error(error.response?.data?.message || '更新失败')
  } finally {
    editing.value = false
  }
}

// 删除工作空间
const handleDelete = async (ws: Workspace, event: Event) => {
  event.stopPropagation()
  
  try {
    await ElMessageBox.confirm(
      `确定要删除工作空间 "${ws.name}" 吗？此操作不可恢复！`,
      '确认删除',
      { type: 'warning' }
    )
    
    const res = await deleteWorkspace(ws.id)
    if (res.code === 200) {
      ElMessage.success('删除成功')
      await loadWorkspaces()
      // 如果删除的是当前选中的，切换到第一个
      if (ws.id === props.modelValue && workspaces.value.length > 0) {
        selectWorkspace(workspaces.value[0].id)
      }
    } else {
      ElMessage.error(res.message || '删除失败')
    }
  } catch (error: any) {
    if (error !== 'cancel') {
      ElMessage.error(error.response?.data?.message || '删除失败')
    }
  }
}

// 获取当前选中的工作空间名称
const currentWorkspaceName = () => {
  const ws = workspaces.value.find(w => w.id === props.modelValue)
  return ws?.name || '选择工作空间'
}

onMounted(() => {
  loadWorkspaces()
})
</script>

<template>
  <div class="workspace-selector">
    <div class="selector-trigger" @click.stop>
      <el-dropdown trigger="click" @command="selectWorkspace">
        <span class="current-workspace">
          <span class="ws-icon">📁</span>
          <span class="ws-name">{{ currentWorkspaceName() }}</span>
          <span class="ws-arrow">▾</span>
        </span>
        <template #dropdown>
          <el-dropdown-menu>
            <el-dropdown-item 
              v-for="ws in workspaces" 
              :key="ws.id" 
              :command="ws.id"
              :class="{ 'is-active': ws.id === modelValue }"
            >
              <div class="ws-item">
                <span class="dropdown-ws-name">{{ ws.name }}</span>
                <span v-if="ws.id === modelValue" class="check-icon">✓</span>
                <div class="ws-actions">
                  <button class="ws-action-btn" @click="openMemberDialog(ws, $event)" title="成员管理">👥</button>
                  <button class="ws-action-btn" @click="openEditDialog(ws, $event)" title="编辑">✏️</button>
                  <button class="ws-action-btn danger" @click="handleDelete(ws, $event)" title="删除">🗑️</button>
                </div>
              </div>
            </el-dropdown-item>
            <el-dropdown-item divided @click="showCreateDialog = true">
              <span class="add-icon">+</span>
              <span>新建工作空间</span>
            </el-dropdown-item>
          </el-dropdown-menu>
        </template>
      </el-dropdown>
    </div>

    <!-- 创建工作空间对话框 -->
    <div v-if="showCreateDialog" class="dialog-overlay" @click.self="showCreateDialog = false">
      <div class="dialog">
        <h3>新建工作空间</h3>
        <form @submit.prevent="handleCreate">
          <div class="form-group">
            <label>名称 *</label>
            <input v-model="newWorkspace.name" type="text" placeholder="输入工作空间名称" />
          </div>
          <div class="form-group">
            <label>描述</label>
            <input v-model="newWorkspace.description" type="text" placeholder="简短描述（可选）" />
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

    <!-- 编辑工作空间对话框 -->
    <div v-if="showEditDialog" class="dialog-overlay" @click.self="showEditDialog = false">
      <div class="dialog">
        <h3>编辑工作空间</h3>
        <form @submit.prevent="handleEdit">
          <div class="form-group">
            <label>名称 *</label>
            <input v-model="editForm.name" type="text" placeholder="输入工作空间名称" />
          </div>
          <div class="form-group">
            <label>描述</label>
            <input v-model="editForm.description" type="text" placeholder="简短描述（可选）" />
          </div>
          <div class="dialog-actions">
            <button type="button" class="cancel-btn" @click="showEditDialog = false">取消</button>
            <button type="submit" class="submit-btn" :disabled="editing">
              {{ editing ? '保存中...' : '保存' }}
            </button>
          </div>
        </form>
      </div>
    </div>

    <!-- 成员管理对话框 -->
    <WorkspaceMemberManager 
      v-model:visible="showMemberDialog" 
      :workspace-id="memberWorkspaceId" 
    />
  </div>
</template>

<style scoped>
.workspace-selector {
  position: relative;
}

.current-workspace {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 12px;
  background: rgba(255, 255, 255, 0.05);
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s;
}

.current-workspace:hover {
  border-color: rgba(255, 255, 255, 0.2);
  background: rgba(255, 255, 255, 0.08);
}

.ws-icon {
  font-size: 16px;
}

.ws-name {
  font-size: 14px;
  max-width: 150px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.ws-arrow {
  font-size: 10px;
  color: #888;
}

.ws-item {
  display: flex;
  align-items: center;
  width: 100%;
  gap: 8px;
}

.dropdown-ws-name {
  flex: 1;
}

.ws-actions {
  display: flex;
  gap: 4px;
  opacity: 0;
  transition: opacity 0.2s;
}

.ws-item:hover .ws-actions {
  opacity: 1;
}

.ws-action-btn {
  padding: 2px 6px;
  background: transparent;
  border: none;
  cursor: pointer;
  font-size: 12px;
  border-radius: 4px;
  transition: background 0.2s;
}

.ws-action-btn:hover {
  background: rgba(255, 255, 255, 0.1);
}

.ws-action-btn.danger:hover {
  background: rgba(239, 68, 68, 0.2);
}

.is-active {
  background: rgba(94, 106, 210, 0.2) !important;
}

.check-icon {
  color: #5e6ad2;
  margin-left: 4px;
}

.add-icon {
  margin-right: 6px;
  color: #5e6ad2;
}

/* Dialog Styles */
.dialog-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.7);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
}

.dialog {
  width: 400px;
  padding: 24px;
  background: #1a1a2e;
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 12px;
}

.dialog h3 {
  margin-bottom: 20px;
  font-size: 18px;
  color: #fff;
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

.form-group input {
  width: 100%;
  padding: 12px;
  background: rgba(0, 0, 0, 0.3);
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 8px;
  color: #fff;
  font-size: 14px;
  outline: none;
}

.form-group input:focus {
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
