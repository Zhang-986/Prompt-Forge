<script setup lang="ts">
import { RouterView } from 'vue-router'
import { ConfigProvider, theme } from 'ant-design-vue'

// Ant Design Vue 暗色主题配置
const themeConfig = {
  algorithm: theme.darkAlgorithm,
  token: {
    colorPrimary: '#7c3aed', // 紫色主色调
    borderRadius: 8,
    colorBgContainer: '#1a1a2e',
    colorBgElevated: '#252542',
    colorBgLayout: '#0f0f1a',
  },
}
</script>

<template>
  <ConfigProvider :theme="themeConfig">
    <RouterView />
  </ConfigProvider>
</template>

<style>
:root {
  /* 暗色主题变量 */
  --color-bg-primary: #0f0f1a;
  --color-bg-secondary: #1a1a2e;
  --color-bg-tertiary: #16162a;
  --color-bg-elevated: #252542;
  
  --color-text-primary: #ffffff;
  --color-text-secondary: #a0a0b0;
  --color-text-tertiary: #707080;
  
  --color-border: rgba(255, 255, 255, 0.1);
  --color-primary: #7c3aed;
  --color-primary-hover: #8b5cf6;
  --color-primary-light: rgba(124, 58, 237, 0.15);
  --color-primary-gradient: linear-gradient(135deg, #7c3aed, #a855f7);
  
  --color-danger: #ef4444;
  --color-success: #22c55e;
  --color-warning: #f59e0b;
}

* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
}

body {
  font-family: 'Inter', -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif;
  -webkit-font-smoothing: antialiased;
  background: var(--color-bg-primary);
  color: var(--color-text-primary);
}

/* 强制所有页面使用暗色背景 */
.page-container,
.arena-container,
.settings-container {
  min-height: 100vh;
  background: linear-gradient(135deg, var(--color-bg-primary) 0%, var(--color-bg-tertiary) 100%) !important;
  color: var(--color-text-primary) !important;
}

/* 确保 Ant Design 组件在暗色模式下正常显示 */
.ant-btn {
  font-weight: 500;
}

.ant-card {
  background: var(--color-bg-elevated);
  border-color: var(--color-border);
}

/* 选择框 select 暗色样式 */
select {
  background: var(--color-bg-secondary) !important;
  color: var(--color-text-primary) !important;
  border: 1px solid var(--color-border) !important;
}

select option {
  background: var(--color-bg-secondary);
  color: var(--color-text-primary);
}

/* 输入框暗色样式 */
input, textarea {
  background: var(--color-bg-secondary) !important;
  color: var(--color-text-primary) !important;
  border: 1px solid var(--color-border) !important;
}

input::placeholder, textarea::placeholder {
  color: var(--color-text-tertiary);
}

/* header 暗色样式 */
.header {
  background: var(--color-bg-secondary) !important;
  border-bottom: 1px solid var(--color-border) !important;
}

/* 全局 logo-icon 样式 */
.logo-icon {
  width: 32px !important;
  height: 32px !important;
  max-width: 32px !important;
  max-height: 32px !important;
  font-size: 24px;
  color: var(--color-primary);
}

/* 头部按钮间距 */
.header-left, .header-right {
  display: flex;
  align-items: center;
  gap: 12px;
}
</style>
