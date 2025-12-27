<script setup lang="ts">
import { ref } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { login, register, type LoginData, type RegisterData } from '../api/user'
import { message } from 'ant-design-vue'

const router = useRouter()
const route = useRoute()

const activeTab = ref('login')
const loading = ref(false)

const loginForm = ref<LoginData>({
  username: '',
  password: ''
})

const registerForm = ref<RegisterData>({
  username: '',
  email: '',
  password: ''
})

const confirmPassword = ref('')

// 登录
const handleLogin = async () => {
  if (!loginForm.value.username || !loginForm.value.password) {
    message.warning('请输入用户名和密码')
    return
  }

  loading.value = true
  try {
    const res = await login(loginForm.value)
    if (res.code === 200) {
      localStorage.setItem('token', res.data.token)
      localStorage.setItem('user', JSON.stringify(res.data.user))
      message.success('登录成功')

      // 跳转到之前的页面或首页
      const redirect = route.query.redirect as string || '/prompts'
      router.push(redirect)
    } else {
      message.error(res.message || '登录失败')
    }
  } catch (error: any) {
    message.error(error.response?.data?.message || '登录失败，请检查后端是否启动')
  } finally {
    loading.value = false
  }
}

// 注册
const handleRegister = async () => {
  if (!registerForm.value.username || !registerForm.value.email || !registerForm.value.password) {
    message.warning('请填写完整信息')
    return
  }
  if (registerForm.value.password !== confirmPassword.value) {
    message.warning('两次密码不一致')
    return
  }

  loading.value = true
  try {
    const res = await register(registerForm.value)
    if (res.code === 200) {
      localStorage.setItem('token', res.data.token)
      localStorage.setItem('user', JSON.stringify(res.data.user))
      message.success('注册成功')
      router.push('/prompts')
    } else {
      message.error(res.message || '注册失败')
    }
  } catch (error: any) {
    message.error(error.response?.data?.message || '注册失败，请检查后端是否启动')
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="login-container">
    <div class="login-card">
      <div class="logo">
        <img src="/vite.svg" alt="Logo" class="logo-icon" />
        <span class="logo-text">Prompt-Forge</span>
      </div>

      <a-tabs v-model:activeKey="activeTab" centered>
        <a-tab-pane key="login" tab="登录">
          <a-form layout="vertical">
            <a-form-item label="用户名" required>
              <a-input v-model:value="loginForm.username" placeholder="请输入用户名" size="large" />
            </a-form-item>
            <a-form-item label="密码" required>
              <a-input-password v-model:value="loginForm.password" placeholder="请输入密码" size="large" />
            </a-form-item>
            <a-form-item>
              <a-button type="primary" @click="handleLogin" :loading="loading" block size="large">
                登录
              </a-button>
            </a-form-item>
          </a-form>
        </a-tab-pane>

        <a-tab-pane key="register" tab="注册">
          <a-form layout="vertical">
            <a-form-item label="用户名" required>
              <a-input v-model:value="registerForm.username" placeholder="请输入用户名" size="large" />
            </a-form-item>
            <a-form-item label="邮箱" required>
              <a-input v-model:value="registerForm.email" type="email" placeholder="请输入邮箱" size="large" />
            </a-form-item>
            <a-form-item label="密码" required>
              <a-input-password v-model:value="registerForm.password" placeholder="请输入密码" size="large" />
            </a-form-item>
            <a-form-item label="确认密码" required>
              <a-input-password v-model:value="confirmPassword" placeholder="请再次输入密码" size="large" />
            </a-form-item>
            <a-form-item>
              <a-button type="primary" @click="handleRegister" :loading="loading" block size="large">
                注册
              </a-button>
            </a-form-item>
          </a-form>
        </a-tab-pane>
      </a-tabs>
    </div>
  </div>
</template>

<style scoped>
.login-container {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #0f0f1a 0%, #1a1a2e 100%);
}

.login-card {
  width: 420px;
  padding: 40px;
  background: #1a1a2e;
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 16px;
  backdrop-filter: blur(10px);
}

.logo {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
  margin-bottom: 32px;
}

.logo-icon {
  width: 36px;
  height: 36px;
}

.logo-text {
  font-size: 22px;
  font-weight: 600;
  color: #fff;
}
</style>
