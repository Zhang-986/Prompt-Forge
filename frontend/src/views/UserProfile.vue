<template>
  <div class="user-profile-page">
    <div class="page-header">
      <a-button type="text" @click="$router.back()" class="back-btn">
        <template #icon><LeftOutlined /></template>
        返回
      </a-button>
      <h1 class="page-title">个人资料</h1>
    </div>

    <div class="profile-container">
      <a-card :bordered="false" class="profile-card">
        <div class="profile-content">
          <!-- 头像部分 -->
          <div class="avatar-section">
            <div class="avatar-wrapper">
              <a-avatar :size="100" :src="userForm.avatar" class="profile-avatar">
                <template #icon v-if="!userForm.avatar">
                  <UserOutlined />
                </template>
              </a-avatar>
              <div class="avatar-upload">
                <a-upload
                  name="file"
                  :show-upload-list="false"
                  :before-upload="beforeUpload"
                  :custom-request="handleUpload"
                  accept="image/*"
                >
                  <a-button type="primary" shape="circle" size="small">
                    <template #icon><CameraOutlined /></template>
                  </a-button>
                </a-upload>
              </div>
            </div>
            <div class="avatar-tip">点击相及图标上传头像 (支持 jpg, png, max 5MB)</div>
          </div>

          <!-- 表单部分 -->
          <a-form
            :model="userForm"
            layout="vertical"
            class="profile-form"
            @finish="handleUpdateProfile"
          >
            <a-form-item label="昵称" name="nickname">
              <a-input v-model:value="userForm.nickname" placeholder="设置一个好听的昵称" :maxlength="50" show-count />
            </a-form-item>

            <a-form-item>
              <a-button type="primary" html-type="submit" :loading="loading" block>
                保存修改
              </a-button>
            </a-form-item>
          </a-form>
        </div>
      </a-card>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { message } from 'ant-design-vue'
import { CameraOutlined, UserOutlined, LeftOutlined } from '@ant-design/icons-vue'
import request from '../api/request'

interface UserInfo {
  id: number
  username: string
  nickname: string
  email: string
  avatar: string
  role: string
}

const loading = ref(false)
const userForm = ref<UserInfo>({
  id: 0,
  username: '',
  nickname: '',
  email: '',
  avatar: '',
  role: ''
})

// 获取用户信息
const fetchUserInfo = async () => {
    try {
        const res = await request.get('/users/me')
        if (res.data) {
            // 确保 nickname 不是 null
            userForm.value = {
                ...res.data,
                nickname: res.data.nickname || ''
            }
            // 更新本地存储的用户信息（如果需要）
            updateLocalStorage(userForm.value)
        }
    } catch (error) {
        message.error('获取用户信息失败')
    }
}

// 上传前校验
const beforeUpload = (file: File) => {
  const isJpgOrPng = file.type === 'image/jpeg' || file.type === 'image/png' || file.type === 'image/webp';
  if (!isJpgOrPng) {
    message.error('只能上传 JPG/PNG/WebP 格式的图片!');
  }
  const isLt5M = file.size / 1024 / 1024 < 5;
  if (!isLt5M) {
    message.error('图片大小必须小于 5MB!');
  }
  return isJpgOrPng && isLt5M;
}

// 自定义上传
const handleUpload = async (options: any) => {
    const { file, onSuccess, onError } = options
    const formData = new FormData()
    formData.append('file', file)

    try {
        const res = await request.post('/users/avatar', formData, {
            headers: { 'Content-Type': 'multipart/form-data' }
        })
        userForm.value.avatar = res.data // 后端返回 URL
        message.success('头像上传成功')
        updateLocalStorage(userForm.value)
        onSuccess(res.data)
    } catch (error) {
        message.error('头像上传失败')
        onError(error)
    }
}

// 更新资料
const handleUpdateProfile = async () => {
    loading.value = true
    try {
        await request.put('/users/profile', {
            nickname: userForm.value.nickname
        })
        message.success('个人资料更新成功')
        updateLocalStorage(userForm.value)
    } catch (error) {
        message.error('更新失败')
    } finally {
        loading.value = false
    }
}

// 更新本地存储和触发事件（如果需要）
const updateLocalStorage = (user: UserInfo) => {
    localStorage.setItem('user', JSON.stringify(user))
    // 可以触发一个自定义事件来通知 Layout 更新头像
    window.dispatchEvent(new Event('user-updated'))
}

onMounted(() => {
    fetchUserInfo()
})
</script>

<style scoped>
.user-profile-page {
  padding: 24px;
  max-width: 800px;
  margin: 0 auto;
}

.page-header {
  margin-bottom: 24px;
  display: flex;
  align-items: center;
  gap: 12px;
}

.back-btn {
  padding: 0;
  color: #6b7280;
}

.back-btn:hover {
  color: #1f2937;
}

.page-title {
  font-size: 24px;
  font-weight: 600;
  color: #1f2937;
  margin: 0;
}

.profile-container {
  display: flex;
  justify-content: center;
}

.profile-card {
  width: 100%;
  max-width: 500px;
  border-radius: 12px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
}

.profile-content {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 32px;
  padding: 20px 0;
}

.avatar-section {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
}

.avatar-wrapper {
  position: relative;
}

.profile-avatar {
  background: #f2f2f2;
  color: #a1a1a1;
  font-size: 36px;
  border: 4px solid white;
  box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.1);
}

.avatar-upload {
  position: absolute;
  bottom: 0;
  right: 0;
  z-index: 2;
}

.avatar-tip {
  font-size: 12px;
  color: #6b7280;
}

.profile-form {
  width: 100%;
}
</style>
