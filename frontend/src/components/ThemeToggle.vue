<script setup lang="ts">
import { ref, onMounted, watch } from 'vue'

type Theme = 'light' | 'dark' | 'system'

const currentTheme = ref<Theme>('system')
const actualTheme = ref<'light' | 'dark'>('dark')

// 检测系统主题偏好
const getSystemTheme = (): 'light' | 'dark' => {
  return window.matchMedia('(prefers-color-scheme: dark)').matches ? 'dark' : 'light'
}

// 应用主题到 DOM
const applyTheme = (theme: 'light' | 'dark') => {
  actualTheme.value = theme
  document.documentElement.setAttribute('data-theme', theme)
}

// 更新主题
const updateTheme = () => {
  if (currentTheme.value === 'system') {
    applyTheme(getSystemTheme())
  } else {
    applyTheme(currentTheme.value)
  }
}

// 切换主题
const toggleTheme = () => {
  const themes: Theme[] = ['light', 'dark', 'system']
  const currentIndex = themes.indexOf(currentTheme.value)
  currentTheme.value = themes[(currentIndex + 1) % themes.length]!
}

// 获取主题图标
const getThemeIcon = (): string => {
  if (currentTheme.value === 'system') {
    return '💻'
  }
  return actualTheme.value === 'dark' ? '🌙' : '☀️'
}

// 获取主题提示
const getThemeLabel = (): string => {
  switch (currentTheme.value) {
    case 'light': return '浅色'
    case 'dark': return '深色'
    case 'system': return '跟随系统'
  }
}

// 监听主题变化
watch(currentTheme, (newTheme) => {
  localStorage.setItem('theme', newTheme)
  updateTheme()
})

// 监听系统主题变化
onMounted(() => {
  // 从 localStorage 恢复主题设置
  const savedTheme = localStorage.getItem('theme')
  if (savedTheme === 'light' || savedTheme === 'dark' || savedTheme === 'system') {
    currentTheme.value = savedTheme as Theme
  }
  updateTheme()

  // 监听系统主题变化
  const mediaQuery = window.matchMedia('(prefers-color-scheme: dark)')
  mediaQuery.addEventListener('change', () => {
    if (currentTheme.value === 'system') {
      updateTheme()
    }
  })
})
</script>

<template>
  <button 
    class="theme-toggle" 
    @click="toggleTheme"
    :title="'当前: ' + getThemeLabel()"
  >
    <span class="theme-icon">{{ getThemeIcon() }}</span>
    <span class="theme-label">{{ getThemeLabel() }}</span>
  </button>
</template>

<style scoped>
.theme-toggle {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 8px 12px;
  background: var(--color-bg-tertiary, rgba(255, 255, 255, 0.1));
  border: 1px solid var(--color-border, rgba(255, 255, 255, 0.1));
  border-radius: var(--radius-md, 8px);
  color: var(--color-text-secondary, #888);
  font-size: 14px;
  cursor: pointer;
  transition: all var(--transition-fast, 0.15s ease);
}

.theme-toggle:hover {
  background: var(--color-primary-light, rgba(94, 106, 210, 0.1));
  border-color: var(--color-primary, #5e6ad2);
  color: var(--color-primary, #5e6ad2);
}

.theme-icon {
  font-size: 16px;
}

.theme-label {
  font-size: 12px;
}

/* 移动端隐藏文字标签 */
@media (max-width: 768px) {
  .theme-label {
    display: none;
  }
  
  .theme-toggle {
    padding: 8px;
  }
}
</style>
