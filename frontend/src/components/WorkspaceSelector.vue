<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { getWorkspaces, createWorkspace, updateWorkspace, deleteWorkspace, type Workspace } from '../api/workspace'
import { message, Modal } from 'ant-design-vue'
import { FolderOutlined, DownOutlined, PlusOutlined, EditOutlined, DeleteOutlined, TeamOutlined, CheckOutlined } from '@ant-design/icons-vue'
import WorkspaceMemberManager from './WorkspaceMemberManager.vue'
import WorkspaceOnboarding from './WorkspaceOnboarding.vue'

const props = defineProps<{
  modelValue: number | undefined
}>()

const emit = defineEmits<{
  (e: 'update:modelValue', value: number | undefined): void
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
const dropdownOpen = ref(false)
const showOnboarding = ref(false)  // 全屏引导

// 打开成员管理对话框
const openMemberDialog = (ws: Workspace) => {
  memberWorkspaceId.value = ws.id
  showMemberDialog.value = true
  dropdownOpen.value = false
}

// 加载工作空间列表
const loadWorkspaces = async () => {
  loading.value = true
  try {
    const res = await getWorkspaces()
    if (res.code === 200) {
      workspaces.value = res.data
      
      // 如果没有任何工作空间，显示全屏引导
      if (workspaces.value.length === 0) {
        showOnboarding.value = true
        return
      }
      
      // 如果当前没有选中，默认选中第一个
      const firstWs = workspaces.value[0]
      if (firstWs && !props.modelValue) {
        emit('update:modelValue', firstWs.id)
        emit('change', firstWs)
        // 保存到 localStorage 供其他页面使用
        localStorage.setItem('currentWorkspaceId', String(firstWs.id))
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
    // 保存到 localStorage 供其他页面使用
    localStorage.setItem('currentWorkspaceId', String(id))
  }
  dropdownOpen.value = false
}

// 创建工作空间
const handleCreate = async () => {
  if (!newWorkspace.value.name) {
    message.warning('请输入工作空间名称')
    return
  }

  creating.value = true
  try {
    const res = await createWorkspace(newWorkspace.value)
    if (res.code === 200) {
      message.success('创建成功')
      showCreateDialog.value = false
      newWorkspace.value = { name: '', description: '' }
      await loadWorkspaces()
      selectWorkspace(res.data.id)
    } else {
      message.error(res.message || '创建失败')
    }
  } catch (error: any) {
    message.error(error.response?.data?.message || '创建失败')
  } finally {
    creating.value = false
  }
}

// 打开编辑对话框
const openEditDialog = (ws: Workspace) => {
  editingWorkspace.value = ws
  editForm.value = { name: ws.name, description: ws.description || '' }
  showEditDialog.value = true
  dropdownOpen.value = false
}

// 编辑工作空间
const handleEdit = async () => {
  if (!editForm.value.name) {
    message.warning('请输入工作空间名称')
    return
  }
  if (!editingWorkspace.value) return

  editing.value = true
  try {
    const res = await updateWorkspace(editingWorkspace.value.id, editForm.value)
    if (res.code === 200) {
      message.success('更新成功')
      showEditDialog.value = false
      await loadWorkspaces()
    } else {
      message.error(res.message || '更新失败')
    }
  } catch (error: any) {
    message.error(error.response?.data?.message || '更新失败')
  } finally {
    editing.value = false
  }
}

// 删除工作空间
const handleDelete = (ws: Workspace) => {
  dropdownOpen.value = false
  Modal.confirm({
    title: '确认删除',
    content: `确定要删除工作空间 "${ws.name}" 吗？此操作不可恢复！`,
    okType: 'danger',
    onOk: async () => {
      const res = await deleteWorkspace(ws.id)
      if (res.code === 200) {
        message.success('删除成功')
        await loadWorkspaces()
        const firstWsAfterDelete = workspaces.value[0]
        if (ws.id === props.modelValue && firstWsAfterDelete) {
          selectWorkspace(firstWsAfterDelete.id)
        }
      } else {
        message.error(res.message || '删除失败')
      }
    }
  })
}

// 获取当前选中的工作空间名称
const currentWorkspaceName = computed(() => {
  const ws = workspaces.value.find(w => w.id === props.modelValue)
  return ws?.name || '选择工作空间'
})

// 引导完成处理
const onOnboardingCreated = async (workspaceId: number) => {
  showOnboarding.value = false
  await loadWorkspaces()
  selectWorkspace(workspaceId)
}

onMounted(() => {
  loadWorkspaces()
})
</script>

<template>
  <div class="workspace-selector">
    <a-dropdown v-model:open="dropdownOpen" :trigger="['click']">
      <div class="current-workspace" @click.prevent>
        <FolderOutlined class="ws-icon" />
        <span class="ws-name">{{ currentWorkspaceName }}</span>
        <DownOutlined class="ws-arrow" />
      </div>
      <template #overlay>
        <a-menu class="ws-menu">
          <a-menu-item v-for="ws in workspaces" :key="ws.id" @click="selectWorkspace(ws.id)">
            <div class="ws-item">
              <span class="dropdown-ws-name">{{ ws.name }}</span>
              <CheckOutlined v-if="ws.id === modelValue" class="check-icon" />
              <div class="ws-actions" @click.stop>
                <a-tooltip title="成员管理">
                  <a-button type="text" size="small" @click="openMemberDialog(ws)">
                    <template #icon>
                      <TeamOutlined />
                    </template>
                  </a-button>
                </a-tooltip>
                <a-tooltip title="编辑">
                  <a-button type="text" size="small" @click="openEditDialog(ws)">
                    <template #icon>
                      <EditOutlined />
                    </template>
                  </a-button>
                </a-tooltip>
                <a-tooltip title="删除">
                  <a-button type="text" size="small" danger @click="handleDelete(ws)">
                    <template #icon>
                      <DeleteOutlined />
                    </template>
                  </a-button>
                </a-tooltip>
              </div>
            </div>
          </a-menu-item>
          <a-menu-divider />
          <a-menu-item @click="showCreateDialog = true; dropdownOpen = false">
            <PlusOutlined class="add-icon" />
            新建工作空间
          </a-menu-item>
        </a-menu>
      </template>
    </a-dropdown>

    <!-- 创建工作空间对话框 -->
    <a-modal v-model:open="showCreateDialog" title="新建工作空间" :footer="null" width="400px">
      <a-form layout="vertical">
        <a-form-item label="名称" required>
          <a-input v-model:value="newWorkspace.name" placeholder="输入工作空间名称" />
        </a-form-item>
        <a-form-item label="描述">
          <a-input v-model:value="newWorkspace.description" placeholder="简短描述（可选）" />
        </a-form-item>
        <a-form-item>
          <a-space>
            <a-button @click="showCreateDialog = false">取消</a-button>
            <a-button type="primary" @click="handleCreate" :loading="creating">创建</a-button>
          </a-space>
        </a-form-item>
      </a-form>
    </a-modal>

    <!-- 编辑工作空间对话框 -->
    <a-modal v-model:open="showEditDialog" title="编辑工作空间" @ok="handleEdit" :confirmLoading="editing">
      <a-form layout="vertical">
        <a-form-item label="名称" required>
          <a-input v-model:value="editForm.name" placeholder="输入工作空间名称" />
        </a-form-item>
        <a-form-item label="描述">
          <a-input v-model:value="editForm.description" placeholder="简短描述（可选）" />
        </a-form-item>
      </a-form>
    </a-modal>

    <!-- 成员管理对话框 -->
    <WorkspaceMemberManager v-model:visible="showMemberDialog" :workspace-id="memberWorkspaceId" />

    <!-- 全屏引导组件 -->
    <WorkspaceOnboarding v-if="showOnboarding" @created="onOnboardingCreated" />
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
  color: #fff;
}

.current-workspace:hover {
  border-color: rgba(255, 255, 255, 0.2);
  background: rgba(255, 255, 255, 0.08);
}

.ws-icon {
  font-size: 16px;
  color: #facc15;
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
  color: rgba(255, 255, 255, 0.45);
}

.ws-menu {
  min-width: 260px;
  background: #1a1a2e !important;
  border: 1px solid rgba(255, 255, 255, 0.1);
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
  gap: 2px;
  opacity: 0;
  transition: opacity 0.2s;
}

.ws-item:hover .ws-actions {
  opacity: 1;
}

.check-icon {
  color: #7c3aed;
  margin-left: 4px;
}

.add-icon {
  margin-right: 8px;
  color: #7c3aed;
}
</style>
