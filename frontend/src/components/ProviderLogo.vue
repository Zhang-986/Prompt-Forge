<script setup lang="ts">
import { h } from 'vue'

// Import Config Assets
import iconOpenAI from '@/assets/openai.svg'
import iconGoogle from '@/assets/google-color.svg'
import iconClaude from '@/assets/claude-color.svg'
import iconDeepSeek from '@/assets/deepseek-color.svg'
import iconQwen from '@/assets/qwen-color.svg'
import iconZhipu from '@/assets/zhipu-color.svg'
import iconHunyuan from '@/assets/hunyuan-color.svg'
import iconCloudflare from '@/assets/cloudflare-color.svg'
import iconGithub from '@/assets/githubcopilot.svg'
import iconMoonshot from '@/assets/moonshot.svg'

const props = defineProps<{
    providerId: string,
    size?: number
}>()

const logoMap: Record<string, string> = {
    openai: iconOpenAI,
    google: iconGoogle,
    claude: iconClaude,
    deepseek: iconDeepSeek,
    aliyun: iconQwen,
    zhipu: iconZhipu,
    hunyuan: iconHunyuan,
    cloudflare: iconCloudflare,
    github: iconGithub,
    moonshot: iconMoonshot
}

const renderLogo = () => {
    const size = props.size || 24
    const style = { width: `${size}px`, height: `${size}px`, objectFit: 'contain' as const }
    const pid = props.providerId?.toLowerCase()

    if (logoMap[pid]) {
        return h('img', { src: logoMap[pid], alt: pid, style })
    }

    // Fallback for providers without assets
    return h('svg', { viewBox: "0 0 24 24", fill: "none", stroke: "currentColor", 'stroke-width': 2, style }, [
        h('rect', { x: 3, y: 11, width: 18, height: 10, rx: 2 }),
        h('circle', { cx: 12, cy: 16, r: 2 }),
        h('path', { d: "M8.5 11V7a3.5 3.5 0 0 1 7 0v4" })
    ])
}
</script>

<template>
    <component :is="renderLogo" />
</template>
