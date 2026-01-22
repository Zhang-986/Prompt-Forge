<script setup lang="ts">
import { ref, onUnmounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { login, register, getCaptcha, sendEmailCode, type LoginData, type RegisterData, type CaptchaResult } from '../api/user'
import { message } from 'ant-design-vue'
import logo from '@/assets/logo.svg'

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
  password: '',
  emailCode: ''
})

const confirmPassword = ref('')

// 邮箱验证码相关
const emailCodeLoading = ref(false)
const emailCodeCooldown = ref(0)
let cooldownTimer: ReturnType<typeof setInterval> | null = null

// 清理定时器
onUnmounted(() => {
  if (cooldownTimer) {
    clearInterval(cooldownTimer)
  }
})

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
      const redirect = route.query.redirect as string || '/app/prompts'
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

// 发送邮箱验证码
const handleSendEmailCode = async () => {
  if (!registerForm.value.email) {
    message.warning('请先输入邮箱')
    return
  }

  // 简单的邮箱格式验证
  const emailRegex = /^[\w.-]+@[\w.-]+\.[a-zA-Z]{2,}$/
  if (!emailRegex.test(registerForm.value.email)) {
    message.warning('请输入正确的邮箱格式')
    return
  }

  emailCodeLoading.value = true
  try {
    const res = await sendEmailCode(registerForm.value.email)
    if (res.code === 200) {
      message.success(res.message || '验证码已发送')
      // 开始倒计时
      emailCodeCooldown.value = res.data?.cooldown || 60
      cooldownTimer = setInterval(() => {
        if (emailCodeCooldown.value > 0) {
          emailCodeCooldown.value--
        } else {
          if (cooldownTimer) {
            clearInterval(cooldownTimer)
            cooldownTimer = null
          }
        }
      }, 1000)
    } else {
      message.error(res.message || '发送失败')
    }
  } catch (error: any) {
    message.error(error.response?.data?.message || '发送失败')
  } finally {
    emailCodeLoading.value = false
  }
}

// 注册
const handleRegister = async () => {
  if (!registerForm.value.username || !registerForm.value.email || !registerForm.value.password) {
    message.warning('请填写完整信息')
    return
  }
  if (!registerForm.value.emailCode) {
    message.warning('请输入邮箱验证码')
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
      router.push('/app/prompts')
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
    <div class="login-box">
      <div class="login-header">
        <img :src="logo" alt="Logo" class="logo-icon" />
        <h1 class="logo-text">Prompt-Forge</h1>
      </div>

      <h2 class="welcome-text">欢迎回来</h2>

      <a-tabs v-model:activeKey="activeTab" centered class="auth-tabs" :tabBarStyle="{ borderBottom: 'none' }">
        <a-tab-pane key="login" tab="登录">
          <a-form layout="vertical" class="auth-form">
            <a-form-item>
              <a-input v-model:value="loginForm.username" placeholder="用户名" size="large" class="minimal-input" />
            </a-form-item>
            <a-form-item>
              <a-input-password v-model:value="loginForm.password" placeholder="密码" size="large"
                class="minimal-input" />
            </a-form-item>

            <!-- 验证码区域 -->
            <a-form-item v-if="captchaRequired">
              <div class="captcha-row">
                <a-input v-model:value="loginForm.captchaCode" placeholder="验证码" size="large"
                  class="minimal-input captcha-input" @pressEnter="handleLogin" />
                <div class="captcha-image-wrapper" @click="fetchCaptcha">
                  <img v-if="captchaData?.captchaImage" :src="captchaData.captchaImage" alt="验证码"
                    class="captcha-image" />
                  <div v-else class="captcha-placeholder">
                    <a-spin v-if="captchaLoading" size="small" />
                    <span v-else>点击获取</span>
                  </div>
                </div>
              </div>
            </a-form-item>

            <a-form-item>
              <a-button type="primary" @click="handleLogin" :loading="loading" block size="large" class="submit-btn">
                继续
              </a-button>
            </a-form-item>
          </a-form>
        </a-tab-pane>

        <a-tab-pane key="register" tab="注册">
          <a-form layout="vertical" class="auth-form">
            <a-form-item>
              <a-input v-model:value="registerForm.username" placeholder="用户名" size="large" class="minimal-input" />
            </a-form-item>
            <a-form-item>
              <a-input v-model:value="registerForm.email" type="email" placeholder="邮箱地址" size="large"
                class="minimal-input" />
            </a-form-item>
            <a-form-item>
              <div class="email-code-row">
                <a-input v-model:value="registerForm.emailCode" placeholder="验证码" size="large"
                  class="minimal-input email-input" />
                <a-button type="link" :loading="emailCodeLoading" :disabled="emailCodeCooldown > 0"
                  @click="handleSendEmailCode" class="send-code-link">
                  {{ emailCodeCooldown > 0 ? `${emailCodeCooldown}s` : '发送验证码' }}
                </a-button>
              </div>
            </a-form-item>
            <a-form-item>
              <a-input-password v-model:value="registerForm.password" placeholder="密码" size="large"
                class="minimal-input" />
            </a-form-item>
            <a-form-item>
              <a-input-password v-model:value="confirmPassword" placeholder="确认密码" size="large" class="minimal-input" />
            </a-form-item>
            <a-form-item>
              <a-button type="primary" @click="handleRegister" :loading="loading" block size="large" class="submit-btn">
                创建账号
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
  background-color: #ffffff;
  color: #000000;
}

.login-box {
  width: 100%;
  max-width: 320px;
  padding: 0 20px;
  display: flex;
  flex-direction: column;
  align-items: center;
}

.login-header {
  margin-bottom: 24px;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
}

.logo-icon {
  width: 32px;
  height: 32px;
}

.logo-text {
  font-size: 20px;
  font-weight: 600;
  color: #000;
}

.welcome-text {
  font-size: 24px;
  font-weight: 600;
  margin-bottom: 32px;
  color: #0d0d0d;
}

.auth-tabs {
  width: 100%;
}

/* Minimalist Input Styles */
.minimal-input {
  border: 1px solid #e5e5e5;
  border-radius: 6px;
  box-shadow: none !important;
  font-size: 14px;
  padding: 12px;
  height: 48px;
}

.minimal-input:focus,
.minimal-input:hover {
  border-color: #000;
}

/* Button Styles */
.submit-btn {
  height: 48px;
  background-color: #000;
  border-color: #000;
  border-radius: 6px;
  font-size: 16px;
  font-weight: 500;
  box-shadow: none;
}

.submit-btn:hover {
  background-color: #333;
  border-color: #333;
}

/* Captcha & Email layout */
.captcha-row,
.email-code-row {
  display: flex;
  gap: 10px;
  position: relative;
}

.captcha-input {
  flex: 1;
}

.captcha-image-wrapper {
  height: 48px;
  width: 100px;
  border: 1px solid #e5e5e5;
  border-radius: 6px;
  overflow: hidden;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f9f9f9;
}

.captcha-image {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.email-input {
  flex: 1;
}

.send-code-link {
  height: 48px;
  color: #000;
  padding: 0 10px;
}

.send-code-link:hover {
  color: #333;
}

/* Override Ant Design Tabs to be very minimal */
:deep(.ant-tabs-nav::before) {
  border-bottom: none !important;
}

:deep(.ant-tabs-tab) {
  font-size: 14px;
  color: #666;
  padding: 8px 0;
  margin: 0 16px 0 0;
}

:deep(.ant-tabs-tab-active .ant-tabs-tab-btn) {
  color: #000 !important;
  font-weight: 500;
}

:deep(.ant-tabs-ink-bar) {
  background: #000 !important;
}

/* Form Item spacing */
:deep(.ant-form-item) {
  margin-bottom: 16px;
}
</style>
