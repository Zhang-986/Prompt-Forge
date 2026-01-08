<script setup lang="ts">
import { ref, onUnmounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { login, register, getCaptcha, sendEmailCode, type LoginData, type RegisterData, type CaptchaResult } from '../api/user'
import { message } from 'ant-design-vue'
import { ReloadOutlined } from '@ant-design/icons-vue'
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
        <img :src="logo" alt="Logo" class="logo-icon" />
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
                <a-input v-model:value="loginForm.captchaCode" placeholder="请输入验证码" size="large" class="captcha-input"
                  @pressEnter="handleLogin" />
                <div class="captcha-image-wrapper" @click="fetchCaptcha">
                  <img v-if="captchaData?.captchaImage" :src="captchaData.captchaImage" alt="验证码"
                    class="captcha-image" />
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
            <a-form-item label="邮箱验证码" required>
              <div class="email-code-row">
                <a-input v-model:value="registerForm.emailCode" placeholder="请输入验证码" size="large"
                  class="email-code-input" />
                <a-button size="large" :loading="emailCodeLoading" :disabled="emailCodeCooldown > 0"
                  @click="handleSendEmailCode" class="send-code-btn">
                  {{ emailCodeCooldown > 0 ? `${emailCodeCooldown}秒后重发` : '发送验证码' }}
                </a-button>
              </div>
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
  background: var(--color-bg-primary);
}

.login-card {
  width: 400px;
  padding: var(--space-8);
  background: var(--color-bg-secondary);
  border: 1px solid var(--color-border-light);
  border-radius: var(--radius-lg);
}

.logo {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: var(--space-3);
  margin-bottom: var(--space-8);
}

.logo-icon {
  width: 32px;
  height: 32px;
}

.logo-text {
  font-size: var(--text-xl);
  font-weight: 600;
  color: var(--color-text-primary);
}

/* 验证码样式 */
.captcha-row {
  display: flex;
  gap: var(--space-3);
  align-items: center;
}

.captcha-input {
  flex: 1;
}

.captcha-image-wrapper {
  position: relative;
  width: 120px;
  height: 40px;
  border-radius: var(--radius-md);
  overflow: hidden;
  cursor: pointer;
  background: var(--color-bg-tertiary);
  border: 1px solid var(--color-border-light);
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
  color: var(--color-text-tertiary);
  font-size: var(--text-xs);
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
  color: var(--color-text-primary);
  font-size: 10px;
  opacity: 0;
  transition: opacity var(--transition-fast);
}

.captcha-image-wrapper:hover .captcha-refresh {
  opacity: 1;
}

/* 邮箱验证码样式 */
.email-code-row {
  display: flex;
  gap: var(--space-3);
  align-items: center;
}

.email-code-input {
  flex: 1;
}

.send-code-btn {
  min-width: 120px;
  white-space: nowrap;
}

/* 强制修复密码框内部边框 */
:deep(.ant-input-affix-wrapper) {
  padding: 0;
  background: transparent !important;
  border-color: var(--color-border-light) !important;
}

:deep(.ant-input-affix-wrapper > input.ant-input) {
  border: none !important;
  box-shadow: none !important;
  background: transparent !important;
  margin: 0 !important;
  padding: 4px 11px !important;
  /* 恢复默认内边距 */
  height: 38px !important;
  /* 匹配 large size */
}

/* 聚焦时只改变外层 wrapper 的边框 */
:deep(.ant-input-affix-wrapper:focus),
:deep(.ant-input-affix-wrapper-focused) {
  border-color: var(--color-primary) !important;
  box-shadow: none !important;
}
</style>
