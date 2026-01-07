<script setup lang="ts">
import { RouterView } from 'vue-router'
import { ConfigProvider } from 'ant-design-vue'

// Ant Design Vue ChatGPT 风格主题配置 - 黑色按钮
const themeConfig = {
  token: {
    colorPrimary: '#0d0d0d',             // 黑色按钮
    borderRadius: 8,
    colorBgContainer: '#ffffff',         // 纯白容器背景
    colorBgElevated: '#ffffff',          // 弹窗背景
    colorBgLayout: '#ffffff',            // 页面背景
    colorBorder: '#e5e5e5',              // 淡边框
    colorText: '#0d0d0d',                // 主文字 - 近黑
    colorTextSecondary: '#6e6e80',       // 次要文字
    colorTextTertiary: '#8e8ea0',        // 辅助文字
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
  /* ===== ChatGPT 纯净风格颜色系统 ===== */
  --color-bg-primary: #ffffff;        /* 主背景 - 纯白 */
  --color-bg-secondary: #f7f7f8;      /* 次级背景 - 极浅灰 */
  --color-bg-tertiary: #ececec;       /* 三级背景 - 悬浮/高亮 */
  --color-bg-elevated: #ffffff;       /* 弹窗/下拉菜单 */
  
  --color-text-primary: #0d0d0d;      /* 主文字 - 近黑 */
  --color-text-secondary: #6e6e80;    /* 次要文字 */
  --color-text-tertiary: #8e8ea0;     /* 辅助文字/占位符 */
  --color-text-muted: #acacbe;        /* 禁用/最弱文字 */
  
  --color-border: #e5e5e5;            /* 主边框 - 浅灰 */
  --color-border-light: #ebebeb;      /* 淡边框 */
  
  /* 主色 = 黑色 (ChatGPT 按钮风格) */
  --color-primary: #0d0d0d;
  --color-primary-hover: #353535;
  --color-primary-muted: rgba(13, 13, 13, 0.05);
  
  --color-danger: #ef4444;
  --color-success: #22c55e;
  --color-warning: #f59e0b;
  
  /* ===== 字体系统 ===== */
  --font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
  --font-mono: 'Fira Code', 'Monaco', monospace;
  
  --text-xs: 12px;
  --text-sm: 14px;
  --text-base: 15px;
  --text-lg: 17px;
  --text-xl: 20px;
  --text-2xl: 24px;
  
  /* ===== 间距系统 ===== */
  --space-1: 4px;
  --space-2: 8px;
  --space-3: 12px;
  --space-4: 16px;
  --space-5: 20px;
  --space-6: 24px;
  --space-8: 32px;
  --space-10: 40px;
  --space-12: 48px;
  
  /* ===== 圆角 ===== */
  --radius-sm: 6px;
  --radius-md: 8px;
  --radius-lg: 12px;
  --radius-xl: 16px;
  --radius-full: 9999px;
  
  /* ===== 过渡 ===== */
  --transition-fast: 0.15s ease;
  --transition-base: 0.2s ease;
}

* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
}

body {
  font-family: var(--font-family);
  font-size: var(--text-base);
  -webkit-font-smoothing: antialiased;
  -moz-osx-font-smoothing: grayscale;
  background: var(--color-bg-primary);
  color: var(--color-text-primary);
}

/* 页面容器 - 纯白背景 */
.page-container,
.arena-container,
.settings-container,
.coach-container,
.login-container,
.admin-container {
  min-height: 100vh;
  background: var(--color-bg-primary) !important;
  color: var(--color-text-primary) !important;
}

/* ===== Ant Design 组件全局覆盖 ===== */

/* 主按钮 - 黑色 */
.ant-btn-primary {
  background: var(--color-primary) !important;
  border-color: var(--color-primary) !important;
  color: #ffffff !important;
}

.ant-btn-primary:hover {
  background: var(--color-primary-hover) !important;
  border-color: var(--color-primary-hover) !important;
}

/* 默认按钮 */
.ant-btn-default {
  background: var(--color-bg-primary) !important;
  border-color: var(--color-border) !important;
  color: var(--color-text-primary) !important;
}

.ant-btn-default:hover {
  border-color: var(--color-primary) !important;
  color: var(--color-primary) !important;
}

/* 下拉菜单 - 确保白色背景 */
.ant-dropdown,
.ant-dropdown-menu,
.ant-select-dropdown,
.ant-picker-dropdown,
.ant-popover-inner {
  background: var(--color-bg-primary) !important;
  border: 1px solid var(--color-border) !important;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08) !important;
}

.ant-dropdown-menu-item,
.ant-select-item {
  color: var(--color-text-primary) !important;
}

.ant-dropdown-menu-item:hover,
.ant-select-item-option-active {
  background: var(--color-bg-secondary) !important;
}

.ant-select-item-option-selected {
  background: var(--color-bg-tertiary) !important;
}

/* 弹窗 Modal */
.ant-modal-content {
  background: var(--color-bg-primary) !important;
}

.ant-modal-header {
  background: var(--color-bg-primary) !important;
  border-bottom: 1px solid var(--color-border) !important;
}

.ant-modal-footer {
  border-top: 1px solid var(--color-border) !important;
}

/* 标签 Tag */
.ant-tag {
  border-radius: var(--radius-md);
}

/* 卡片 */
.ant-card {
  background: var(--color-bg-primary);
  border-color: var(--color-border);
}

/* 选择框 */
.ant-select-selector {
  background: var(--color-bg-primary) !important;
  border-color: var(--color-border) !important;
}

/* 输入框 */
.ant-input,
.ant-input-affix-wrapper {
  background: var(--color-bg-primary) !important;
  border-color: var(--color-border) !important;
}

/* 原生表单元素 */
select {
  background: var(--color-bg-primary) !important;
  color: var(--color-text-primary) !important;
  border: 1px solid var(--color-border) !important;
  border-radius: var(--radius-md);
}

select option {
  background: var(--color-bg-primary);
  color: var(--color-text-primary);
}

input, textarea {
  background: var(--color-bg-primary) !important;
  color: var(--color-text-primary) !important;
  border: 1px solid var(--color-border) !important;
  border-radius: var(--radius-md);
  transition: border-color var(--transition-fast);
}

input:focus, textarea:focus {
  border-color: var(--color-primary) !important;
  outline: none;
}

input::placeholder, textarea::placeholder {
  color: var(--color-text-tertiary);
}

/* Header 通用样式 */
.header {
  background: var(--color-bg-primary) !important;
  border-bottom: 1px solid var(--color-border) !important;
}

/* Logo 样式 - 简洁黑色 */
.logo-icon {
  width: 28px !important;
  height: 28px !important;
  color: var(--color-primary);
}

.logo-text {
  font-size: var(--text-lg);
  font-weight: 600;
  color: var(--color-text-primary);
}

/* 头部布局 */
.header-left, .header-right {
  display: flex;
  align-items: center;
  gap: var(--space-3);
}
</style>

