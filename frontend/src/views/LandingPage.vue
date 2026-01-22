<script setup lang="ts">
import { useRouter } from 'vue-router'
import {
    BranchesOutlined,
    ThunderboltOutlined,
    RobotOutlined,
    GithubOutlined,
    DiffOutlined,
    SearchOutlined,
    CodeOutlined,
    FileTextOutlined,
    ToolOutlined,
    ApiOutlined,
    SafetyCertificateOutlined,
    CloudOutlined
} from '@ant-design/icons-vue'

const router = useRouter()

const handleGetStarted = () => {
    const token = localStorage.getItem('token')
    if (token) {
        router.push('/app/prompts')
    } else {
        router.push('/login')
    }
}

// 核心产品卡片
const productCards = [
    {
        icon: BranchesOutlined,
        title: 'Prompt 版本控制',
        subtitle: '像管理代码一样管理 Prompt。完整提交历史，随时回滚。'
    },
    {
        icon: ThunderboltOutlined,
        title: 'LLM 竞技场',
        subtitle: '让模型公平对决。盲测评估，数据说话。'
    },
    {
        icon: RobotOutlined,
        title: 'Prompt 教练',
        subtitle: 'AI Agent 自动优化 Prompt。按需加载，智能迭代。'
    }
]

// Agent Skills
const agentSkills = [
    { icon: SearchOutlined, name: '联网搜索', desc: 'Tavily API 实时获取' },
    { icon: CodeOutlined, name: '代码分析', desc: '静态分析代码结构' },
    { icon: ApiOutlined, name: '代码生成', desc: '自动生成业务代码' },
    { icon: FileTextOutlined, name: 'Prompt 库', desc: '检索最佳实践' },
    { icon: DiffOutlined, name: '版本分析', desc: '智能对比差异' },
    { icon: ToolOutlined, name: '自动优化', desc: '改进 Prompt 结构' },
    { icon: SafetyCertificateOutlined, name: '效果评估', desc: '多维度评分' },
    { icon: CloudOutlined, name: '结构化输出', desc: 'JSON/YAML 格式' }
]
</script>

<template>
    <div class="landing-page">
        <!-- 导航栏 -->
        <nav class="nav-header">
            <div class="nav-content">
                <div class="logo-section">
                    <span class="brand-name">Prompt-Forge</span>
                </div>
                <div class="nav-actions">
                    <a-button type="text" class="nav-link" @click="router.push('/login')">登录</a-button>
                    <a-button type="primary" class="nav-btn-primary" @click="handleGetStarted">免费注册</a-button>
                </div>
            </div>
        </nav>

        <!-- Hero 区域 -->
        <section class="hero-section">
            <div class="hero-content">
                <h1 class="hero-title">
                    驾驭 Prompt 工程<br />
                    赋能团队协作
                </h1>
                <p class="hero-desc">
                    企业级 Prompt 一站式开发平台。版本控制、竞技场评测、AI 智能优化，全方位提升提示词工程效率。
                </p>
                <div class="hero-actions">
                    <a-button type="primary" size="large" class="cta-primary" @click="handleGetStarted">
                        立即开始
                    </a-button>
                    <a href="https://github.com/Zhang-986/prompt-forge" target="_blank" class="cta-secondary">
                        <GithubOutlined /> 查看源码
                    </a>
                </div>
            </div>
        </section>

        <!-- 核心功能 -->
        <section class="features-section">
            <div class="section-container">
                <div class="features-grid">
                    <div v-for="card in productCards" :key="card.title" class="feature-card">
                        <div class="card-icon">
                            <component :is="card.icon" />
                        </div>
                        <h3>{{ card.title }}</h3>
                        <p>{{ card.subtitle }}</p>
                    </div>
                </div>
            </div>
        </section>

        <!-- Agent Skills -->
        <section class="skills-section">
            <div class="section-container">
                <div class="section-header">
                    <h2>AI 能力矩阵</h2>
                </div>
                <div class="skills-list">
                    <div v-for="skill in agentSkills" :key="skill.name" class="skill-row">
                        <component :is="skill.icon" class="skill-icon-small" />
                        <span class="skill-name">{{ skill.name }}</span>
                        <span class="skill-desc">{{ skill.desc }}</span>
                    </div>
                </div>
            </div>
        </section>

        <!-- Footer -->
        <footer class="footer">
            <div class="footer-content">
                <div class="footer-left">
                    <span class="footer-logo">Prompt-Forge</span>
                </div>
                <div class="footer-right">
                    <a href="https://github.com/Zhang-986/prompt-forge" target="_blank">GitHub</a>
                </div>
            </div>
        </footer>
    </div>
</template>

<style scoped>
/* ========== 基础 ========== */
.landing-page {
    min-height: 100vh;
    background: #ffffff;
    color: #000000;
    font-family: "Söhne", ui-sans-serif, system-ui, -apple-system, "Segoe UI", Roboto, Ubuntu, Cantarell, "Noto Sans", sans-serif, "Helvetica Neue", Arial, "Apple Color Emoji", "Segoe UI Emoji", "Segoe UI Symbol", "Noto Color Emoji";
}

/* ========== 导航 ========== */
.nav-header {
    height: 64px;
    display: flex;
    justify-content: center;
    position: sticky;
    top: 0;
    background: rgba(255, 255, 255, 0.9);
    backdrop-filter: blur(8px);
    z-index: 100;
}

.nav-content {
    width: 100%;
    max-width: 1200px;
    padding: 0 24px;
    display: flex;
    align-items: center;
    justify-content: space-between;
}

.brand-name {
    font-weight: 600;
    font-size: 16px;
    color: #000;
}

.nav-actions {
    display: flex;
    gap: 12px;
}

.nav-link {
    color: #666;
}

.nav-link:hover {
    color: #000;
    background: transparent;
}

.nav-btn-primary {
    background: #000;
    border-color: #000;
    color: #fff;
    border-radius: 4px;
    font-weight: 500;
}

.nav-btn-primary:hover {
    background: #333;
    border-color: #333;
}

/* ========== Hero ========== */
.hero-section {
    padding: 120px 24px 80px;
    display: flex;
    justify-content: center;
    text-align: center;
}

.hero-content {
    max-width: 800px;
}

.hero-title {
    font-size: 48px;
    font-weight: 600;
    line-height: 1.1;
    letter-spacing: -0.02em;
    margin-bottom: 24px;
    color: #000;
}

.hero-desc {
    font-size: 20px;
    color: #666;
    line-height: 1.5;
    margin-bottom: 40px;
    max-width: 600px;
    margin-left: auto;
    margin-right: auto;
}

.hero-actions {
    display: flex;
    gap: 12px;
    justify-content: center;
}

.cta-primary {
    height: 48px;
    padding: 0 24px;
    background: #000;
    border: none;
    border-radius: 4px;
    font-size: 16px;
    font-weight: 500;
}

.cta-primary:hover {
    background: #333;
}

.cta-secondary {
    height: 48px;
    padding: 0 24px;
    display: inline-flex;
    align-items: center;
    gap: 8px;
    color: #000;
    text-decoration: none;
    font-weight: 500;
    border-radius: 4px;
}

.cta-secondary:hover {
    background: #f5f5f5;
}

/* ========== Features ========== */
.features-section {
    padding: 80px 24px;
    background: #fff;
    border-top: 1px solid #f0f0f0;
}

.section-container {
    max-width: 1200px;
    margin: 0 auto;
}

.features-grid {
    display: grid;
    grid-template-columns: repeat(auto-fit, minmax(300px, 1fr));
    gap: 40px;
}

.feature-card {
    padding: 24px;
    /* Minimalist: No border/shadow by default, maybe refined on hover or just clean text */
    text-align: left;
}

.card-icon {
    font-size: 24px;
    color: #000;
    margin-bottom: 16px;
}

.feature-card h3 {
    font-size: 18px;
    font-weight: 600;
    margin-bottom: 8px;
    color: #000;
}

.feature-card p {
    font-size: 15px;
    color: #666;
    line-height: 1.6;
}

/* ========== Skills ========== */
.skills-section {
    padding: 80px 24px;
    background: #f9f9f9;
}

.section-header {
    margin-bottom: 40px;
}

.section-header h2 {
    font-size: 24px;
    font-weight: 600;
    color: #000;
}

.skills-list {
    display: grid;
    grid-template-columns: repeat(auto-fit, minmax(280px, 1fr));
    gap: 20px;
}

.skill-row {
    display: flex;
    align-items: center;
    gap: 12px;
    padding: 16px;
    background: #fff;
    border: 1px solid #e5e5e5;
    border-radius: 6px;
}

.skill-icon-small {
    font-size: 18px;
    color: #666;
}

.skill-name {
    font-weight: 500;
    color: #000;
}

.skill-desc {
    font-size: 13px;
    color: #888;
    margin-left: auto;
}

/* ========== Footer ========== */
.footer {
    padding: 60px 24px;
    border-top: 1px solid #f0f0f0;
}

.footer-content {
    max-width: 1200px;
    margin: 0 auto;
    display: flex;
    justify-content: space-between;
    align-items: center;
}

.footer-logo {
    font-weight: 600;
    font-size: 14px;
    color: #666;
}

.footer-right a {
    color: #666;
    text-decoration: none;
    font-size: 14px;
}

.footer-right a:hover {
    color: #000;
}
</style>
