<script setup lang="ts">
defineProps<{
  height?: string
  width?: string
  borderRadius?: string
  animated?: boolean
}>()
</script>

<template>
  <div 
    class="skeleton"
    :class="{ 'skeleton--animated': animated !== false }"
    :style="{
      height: height || '20px',
      width: width || '100%',
      borderRadius: borderRadius || 'var(--radius-md, 8px)'
    }"
  ></div>
</template>

<style scoped>
.skeleton {
  background: var(--color-bg-tertiary, rgba(255, 255, 255, 0.1));
}

.skeleton--animated {
  position: relative;
  overflow: hidden;
}

.skeleton--animated::after {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: linear-gradient(
    90deg,
    transparent,
    rgba(255, 255, 255, 0.08),
    transparent
  );
  animation: skeleton-shimmer 1.5s infinite;
}

@keyframes skeleton-shimmer {
  0% {
    transform: translateX(-100%);
  }
  100% {
    transform: translateX(100%);
  }
}

/* 浅色主题调整 */
:root:not([data-theme="dark"]) .skeleton--animated::after,
[data-theme="light"] .skeleton--animated::after {
  background: linear-gradient(
    90deg,
    transparent,
    rgba(0, 0, 0, 0.04),
    transparent
  );
}
</style>
