<script setup lang="ts">
import { ref } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { login, register, getCaptcha, type LoginData, type RegisterData, type CaptchaResult } from '../api/user'
import { message } from 'ant-design-vue'
import { ReloadOutlined } from '@ant-design/icons-vue'

const router = useRouter()
const route = useRoute()

const activeTab = ref('login')
const loading = ref(false)

// 验证码相关
const captchaRequired = ref(false)
const captchaLoading = ref(false)
const captchaData = ref<CaptchaResult | null>(null)

const loginForm = ref<LoginData>({
  username: '',
  password: '',
  captchaKey: '',
  captchaCode: ''
})

const registerForm = ref<RegisterData>({
  username: '',
  email: '',
  password: ''
})

const confirmPassword = ref('')

// 获取验证码
const fetchCaptcha = async () => {
  captchaLoading.value = true
  try {
    const res = await getCaptcha()
    if (res.code === 200) {
      captchaData.value = res.data
      loginForm.value.captchaKey = res.data.captchaKey
      loginForm.value.captchaCode = ''
    }
  } catch (error) {
    message.error('获取验证码失败')
  } finally {
    captchaLoading.value = false
  }
}

// 登录
const handleLogin = async () => {
  if (!loginForm.value.username || !loginForm.value.password) {
    message.warning('请输入用户名和密码')
    return
  }

  // 如果需要验证码但没有输入
  if (captchaRequired.value && !loginForm.value.captchaCode) {
    message.warning('请输入验证码')
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
      // 检查是否需要验证码
      if (res.code === 428) {
        captchaRequired.value = true
        await fetchCaptcha()
      }
    }
  } catch (error: any) {
    const errorCode = error.response?.data?.code
    const errorMessage = error.response?.data?.message || '登录失败，请检查后端是否启动'
    
    message.error(errorMessage)
    
    // 如果返回 428，表示需要验证码
    if (errorCode === 428) {
      captchaRequired.value = true
      await fetchCaptcha()
    } else if (captchaRequired.value) {
      // 已经需要验证码的情况下登录失败，刷新验证码
      await fetchCaptcha()
    }
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
            
            <!-- 验证码区域 -->
            <a-form-item v-if="captchaRequired" label="验证码" required>
              <div class="captcha-row">
                <a-input 
                  v-model:value="loginForm.captchaCode" 
                  placeholder="请输入验证码" 
                  size="large"
                  class="captcha-input"
                  @pressEnter="handleLogin"
                />
                <div class="captcha-image-wrapper" @click="fetchCaptcha">
                  <img 
                    v-if="captchaData?.captchaImage" 
                    :src="captchaData.captchaImage" 
                    alt="验证码" 
                    class="captcha-image"
                  />
                  <div v-else class="captcha-placeholder">
                    <a-spin v-if="captchaLoading" size="small" />
                    <span v-else>点击获取</span>
                  </div>
                  <div class="captcha-refresh" title="点击刷新">
                    <ReloadOutlined />
                  </div>
                </div>
              </div>
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

/* 验证码样式 */
.captcha-row {
  display: flex;
  gap: 12px;
  align-items: center;
}

.captcha-input {
  flex: 1;
}

.captcha-image-wrapper {
  position: relative;
  width: 120px;
  height: 40px;
  border-radius: 6px;
  overflow: hidden;
  cursor: pointer;
  background: #2a2a4e;
  border: 1px solid rgba(255, 255, 255, 0.1);
  display: flex;
  align-items: center;
  justify-content: center;
}

.captcha-image {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.captcha-placeholder {
  color: rgba(255, 255, 255, 0.5);
  font-size: 12px;
}

.captcha-refresh {
  position: absolute;
  top: 2px;
  right: 2px;
  width: 18px;
  height: 18px;
  background: rgba(0, 0, 0, 0.5);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-size: 10px;
  opacity: 0;
  transition: opacity 0.2s;
}

.captcha-image-wrapper:hover .captcha-refresh {
  opacity: 1;
}
</style>
