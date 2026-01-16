<script setup lang="ts">
import { useRouter } from 'vue-router'
import {
    BranchesOutlined,
    ThunderboltOutlined,
    RobotOutlined,
    TeamOutlined,
    ArrowRightOutlined,
    GithubOutlined,
    HistoryOutlined,
    DiffOutlined,
    TrophyOutlined,
    SearchOutlined,
    CodeOutlined,
    FileTextOutlined,
    ToolOutlined,
    ApiOutlined,
    SafetyCertificateOutlined,
    CloudOutlined,
    SyncOutlined
} from '@ant-design/icons-vue'
import logo from '@/assets/logo.svg'

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
        badge: 'Core',
        badgeType: 'primary',
        title: 'Prompt 版本控制',
        subtitle: '像管理代码一样管理 Prompt',
        features: [
            { icon: HistoryOutlined, text: '完整提交历史，随时回滚' },
            { icon: DiffOutlined, text: '版本 Diff 对比，改动一目了然' },
            { icon: SyncOutlined, text: '一键恢复任意历史版本' }
        ]
    },
    {
        icon: ThunderboltOutlined,
        badge: 'Hot',
        badgeType: 'hot',
        title: 'LLM 竞技场',
        subtitle: '让模型来一场公平对决',
        features: [
            { icon: SafetyCertificateOutlined, text: '盲测对决，结果说话' },
            { icon: TrophyOutlined, text: 'ELO 排行榜，谁强谁弱清清楚楚' },
            { icon: HistoryOutlined, text: '对战历史回放，复盘每一局' }
        ]
    },
    {
        icon: RobotOutlined,
        badge: 'Agent',
        badgeType: 'agent',
        title: 'Prompt 教练',
        subtitle: 'AI Agent 帮你写最优 Prompt',
        features: [
            { icon: ToolOutlined, text: '8 大技能按需加载' },
            { icon: SearchOutlined, text: '联网搜索最新资料' },
            { icon: CodeOutlined, text: '代码分析、自动优化' }
        ]
    }
]

// Agent Skills
const agentSkills = [
    { icon: SearchOutlined, name: '联网搜索', desc: 'Tavily API 实时获取最新资料' },
    { icon: CodeOutlined, name: '代码分析', desc: '静态分析代码结构与依赖' },
    { icon: ApiOutlined, name: '代码生成', desc: '根据需求自动生成代码' },
    { icon: FileTextOutlined, name: 'Prompt 库', desc: '检索已有模板' },
    { icon: DiffOutlined, name: '版本分析', desc: '智能对比差异' },
    { icon: ToolOutlined, name: '自动优化', desc: '改进 Prompt 结构' },
    { icon: SafetyCertificateOutlined, name: '效果评估', desc: '评估输出质量' },
    { icon: CloudOutlined, name: '结构化输出', desc: 'JSON/YAML 格式' }
]

// 更多功能
const moreFeatures = [
    { icon: TeamOutlined, text: '工作空间 - 团队协作共享' },
    { icon: CloudOutlined, text: '模板广场 - 发现最佳实践' },
    { icon: ApiOutlined, text: '多模型支持 - 20+ LLM' },
    { icon: FileTextOutlined, text: '导入导出 - 备份资产' }
]
</script>

<template>
    <div class="landing-page">
        <!-- 导航栏 -->
        <nav class="nav-header">
            <div class="nav-content">
                <div class="logo-section">
                    <img :src="logo" alt="PromptForge" class="logo-img" />
                    <span class="brand-name">Prompt-Forge</span>
                </div>
                <div class="nav-actions">
                    <a-button type="text" class="nav-link" @click="router.push('/login')">登录</a-button>
                    <a-button type="primary" @click="handleGetStarted">免费使用</a-button>
                </div>
            </div>
        </nav>

        <!-- Hero 区域 -->
        <section class="hero-section">
            <!-- 背景装饰 -->
            <div class="hero-bg">
                <div class="gradient-orb orb-1"></div>
                <div class="gradient-orb orb-2"></div>
                <div class="grid-pattern"></div>
            </div>

            <div class="hero-content">
                <div class="hero-badge">
                    <span class="badge-dot"></span>
                    Prompt 工程一站式平台
                </div>
                <h1 class="hero-title">
                    告别 Prompt 管理混乱<br />
                    <span class="gradient-text">让团队高效协作</span>
                </h1>
                <p class="hero-desc">
                    版本控制 · 模型对决 · AI 教练<br />
                    一站式解决 Prompt 开发中的所有痛点
                </p>
                <div class="hero-actions">
                    <a-button type="primary" size="large" class="cta-primary" @click="handleGetStarted">
                        开始使用
                        <ArrowRightOutlined />
                    </a-button>
                    <a href="https://github.com/Zhang-986/prompt-forge" target="_blank" class="cta-secondary">
                        <GithubOutlined /> 查看源码
                    </a>
                </div>

                <!-- 数据亮点 -->
                <div class="hero-stats">
                    <div class="stat-item">
                        <span class="stat-value">8</span>
                        <span class="stat-label">Agent 技能</span>
                    </div>
                    <div class="stat-divider"></div>
                    <div class="stat-item">
                        <span class="stat-value">20+</span>
                        <span class="stat-label">支持模型</span>
                    </div>
                    <div class="stat-divider"></div>
                    <div class="stat-item">
                        <span class="stat-value">Git</span>
                        <span class="stat-label">版本管理</span>
                    </div>
                </div>
            </div>
        </section>

        <!-- 痛点区域 -->
        <section class="pain-section">
            <div class="section-container">
                <div class="pain-card">
                    <div class="pain-header">
                        <span class="pain-tag">痛点</span>
                        <h3>这些问题，你一定遇到过</h3>
                    </div>
                    <div class="pain-list">
                        <div class="pain-item">
                            <span class="pain-num">01</span>
                            <p>Prompt 改了无数版，群里文档满天飞，哪个是最新的？</p>
                        </div>
                        <div class="pain-item">
                            <span class="pain-num">02</span>
                            <p>GPT-4 和 Claude 到底谁强？每次手动切换太麻烦。</p>
                        </div>
                        <div class="pain-item">
                            <span class="pain-num">03</span>
                            <p>写 Prompt 靠感觉，没人指导，效果出来全凭运气。</p>
                        </div>
                    </div>
                </div>
            </div>
        </section>

        <!-- 核心产品 -->
        <section class="products-section">
            <div class="section-container">
                <div class="section-header">
                    <span class="section-tag">核心功能</span>
                    <h2>每一个都针对真实痛点设计</h2>
                </div>
                <div class="products-grid">
                    <div v-for="card in productCards" :key="card.title" class="product-card">
                        <div class="card-header">
                            <span :class="['card-badge', card.badgeType]">{{ card.badge }}</span>
                        </div>
                        <div class="card-icon">
                            <component :is="card.icon" />
                        </div>
                        <h3>{{ card.title }}</h3>
                        <p class="card-subtitle">{{ card.subtitle }}</p>
                        <ul class="card-features">
                            <li v-for="f in card.features" :key="f.text">
                                <component :is="f.icon" class="feature-icon" />
                                {{ f.text }}
                            </li>
                        </ul>
                        <a class="card-cta" @click="handleGetStarted">
                            立即体验
                            <ArrowRightOutlined />
                        </a>
                    </div>
                </div>
            </div>
        </section>

        <!-- Agent Skills -->
        <section class="skills-section">
            <div class="section-container">
                <div class="section-header">
                    <span class="section-tag">技术亮点</span>
                    <h2>Agent 技能系统</h2>
                    <p>三层按需加载架构，覆盖 Prompt 全生命周期</p>
                </div>
                <div class="skills-grid">
                    <div v-for="skill in agentSkills" :key="skill.name" class="skill-item">
                        <div class="skill-icon">
                            <component :is="skill.icon" />
                        </div>
                        <div class="skill-info">
                            <h4>{{ skill.name }}</h4>
                            <p>{{ skill.desc }}</p>
                        </div>
                    </div>
                </div>
                <div class="skills-note">
                    <div class="note-icon">
                        <ApiOutlined />
                    </div>
                    <div class="note-text">
                        <strong>技术实现：</strong>Level 1 元数据预加载 → Level 2 指令按需获取 → Level 3 执行器动态注入，通过 OpenAI Function
                        Calling 协议，AI 自动根据用户意图选择技能
                    </div>
                </div>
            </div>
        </section>

        <!-- 更多功能 -->
        <section class="more-section">
            <div class="section-container">
                <div class="more-grid">
                    <div v-for="item in moreFeatures" :key="item.text" class="more-item">
                        <component :is="item.icon" class="more-icon" />
                        <span>{{ item.text }}</span>
                    </div>
                </div>
            </div>
        </section>

        <!-- Final CTA -->
        <section class="final-cta">
            <div class="section-container">
                <div class="cta-box">
                    <h2>准备好告别混乱了吗？</h2>
                    <p>免费开始，无需绑卡</p>
                    <a-button type="primary" size="large" @click="handleGetStarted">
                        立即开始
                        <ArrowRightOutlined />
                    </a-button>
                </div>
            </div>
        </section>

        <!-- Footer -->
        <footer class="footer">
            <div class="footer-content">
                <div class="footer-left">
                    <span class="footer-logo">Prompt-Forge</span>
                    <p>Built for AI Engineers, by AI Engineers.</p>
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
    color: #0d0d0d;
    font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, "Noto Sans SC", sans-serif;
    overflow-x: hidden;
}

/* ========== 导航 ========== */
.nav-header {
    position: fixed;
    top: 0;
    left: 0;
    right: 0;
    height: 64px;
    background: rgba(255, 255, 255, 0.85);
    backdrop-filter: blur(12px);
    border-bottom: 1px solid rgba(0, 0, 0, 0.05);
    z-index: 100;
    display: flex;
    justify-content: center;
}

.nav-content {
    width: 100%;
    max-width: 1200px;
    padding: 0 24px;
    display: flex;
    align-items: center;
    justify-content: space-between;
}

.logo-section {
    display: flex;
    align-items: center;
    gap: 10px;
}

.logo-img {
    width: 28px;
    height: 28px;
}

.brand-name {
    font-weight: 700;
    font-size: 18px;
    letter-spacing: -0.5px;
    color: #0d0d0d;
}

.nav-actions {
    display: flex;
    align-items: center;
    gap: 12px;
}

.nav-link {
    color: #555;
}

.nav-link:hover {
    color: #0d0d0d;
}

/* ========== Hero ========== */
.hero-section {
    position: relative;
    min-height: 100vh;
    display: flex;
    align-items: center;
    justify-content: center;
    text-align: center;
    overflow: hidden;
    background: linear-gradient(180deg, #ffffff 0%, #f8f9ff 100%);
}

.hero-bg {
    position: absolute;
    inset: 0;
    overflow: hidden;
}

.gradient-orb {
    position: absolute;
    border-radius: 50%;
    filter: blur(100px);
    opacity: 0.5;
    animation: float 8s ease-in-out infinite;
}

.orb-1 {
    width: 500px;
    height: 500px;
    background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
    top: -15%;
    left: -5%;
}

.orb-2 {
    width: 400px;
    height: 400px;
    background: linear-gradient(135deg, #f093fb 0%, #f5576c 100%);
    bottom: -10%;
    right: -5%;
    animation-delay: -4s;
}

@keyframes float {

    0%,
    100% {
        transform: translateY(0) scale(1);
    }

    50% {
        transform: translateY(-20px) scale(1.03);
    }
}

.grid-pattern {
    position: absolute;
    inset: 0;
    background-image:
        linear-gradient(rgba(0, 0, 0, 0.02) 1px, transparent 1px),
        linear-gradient(90deg, rgba(0, 0, 0, 0.02) 1px, transparent 1px);
    background-size: 50px 50px;
}

.hero-content {
    position: relative;
    z-index: 1;
    max-width: 800px;
    padding: 0 24px;
}

.hero-badge {
    display: inline-flex;
    align-items: center;
    gap: 8px;
    padding: 8px 18px;
    background: rgba(102, 126, 234, 0.1);
    border: 1px solid rgba(102, 126, 234, 0.2);
    border-radius: 99px;
    font-size: 14px;
    font-weight: 500;
    color: #667eea;
    margin-bottom: 32px;
}

.badge-dot {
    width: 8px;
    height: 8px;
    background: #22c55e;
    border-radius: 50%;
    animation: pulse 2s infinite;
}

@keyframes pulse {

    0%,
    100% {
        opacity: 1;
    }

    50% {
        opacity: 0.5;
    }
}

.hero-title {
    font-size: 56px;
    font-weight: 800;
    line-height: 1.15;
    letter-spacing: -2px;
    margin-bottom: 24px;
    color: #0d0d0d;
}

.gradient-text {
    background: linear-gradient(135deg, #667eea 0%, #764ba2 50%, #f5576c 100%);
    -webkit-background-clip: text;
    -webkit-text-fill-color: transparent;
    background-size: 200% 200%;
    animation: gradient-shift 5s ease infinite;
}

@keyframes gradient-shift {

    0%,
    100% {
        background-position: 0% 50%;
    }

    50% {
        background-position: 100% 50%;
    }
}

.hero-desc {
    font-size: 18px;
    color: #555;
    line-height: 1.8;
    margin-bottom: 40px;
}

.hero-actions {
    display: flex;
    gap: 16px;
    justify-content: center;
    flex-wrap: wrap;
    margin-bottom: 60px;
}

.cta-primary {
    height: 52px;
    padding: 0 32px;
    font-weight: 600;
    font-size: 16px;
    background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
    border: none;
}

.cta-primary:hover {
    background: linear-gradient(135deg, #7c8ff0 0%, #8a5cb0 100%);
}

.cta-secondary {
    height: 52px;
    padding: 0 28px;
    display: inline-flex;
    align-items: center;
    gap: 8px;
    border: 1px solid #ddd;
    border-radius: 8px;
    color: #333;
    text-decoration: none;
    font-weight: 500;
    transition: all 0.2s;
}

.cta-secondary:hover {
    background: #f5f5f5;
    border-color: #ccc;
}

.hero-stats {
    display: flex;
    justify-content: center;
    align-items: center;
    gap: 40px;
}

.stat-item {
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: 4px;
}

.stat-value {
    font-size: 28px;
    font-weight: 700;
    background: linear-gradient(135deg, #667eea 0%, #f5576c 100%);
    -webkit-background-clip: text;
    -webkit-text-fill-color: transparent;
}

.stat-label {
    font-size: 13px;
    color: #888;
}

.stat-divider {
    width: 1px;
    height: 40px;
    background: #e0e0e0;
}

/* ========== Pain Section ========== */
.pain-section {
    padding: 100px 24px;
    background: #fff;
}

.section-container {
    max-width: 1200px;
    margin: 0 auto;
}

.pain-card {
    background: linear-gradient(135deg, rgba(102, 126, 234, 0.05) 0%, rgba(245, 87, 108, 0.05) 100%);
    border: 1px solid #eee;
    border-radius: 24px;
    padding: 48px;
}

.pain-header {
    margin-bottom: 32px;
}

.pain-tag {
    display: inline-block;
    padding: 4px 12px;
    background: rgba(245, 87, 108, 0.1);
    color: #f5576c;
    border-radius: 6px;
    font-size: 12px;
    font-weight: 600;
    margin-bottom: 12px;
}

.pain-card h3 {
    font-size: 24px;
    font-weight: 600;
    color: #0d0d0d;
}

.pain-list {
    display: grid;
    grid-template-columns: repeat(auto-fit, minmax(300px, 1fr));
    gap: 24px;
}

.pain-item {
    display: flex;
    gap: 16px;
    align-items: flex-start;
}

.pain-num {
    font-size: 32px;
    font-weight: 800;
    background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
    -webkit-background-clip: text;
    -webkit-text-fill-color: transparent;
    line-height: 1;
}

.pain-item p {
    font-size: 15px;
    color: #555;
    line-height: 1.6;
    margin: 0;
}

/* ========== Products ========== */
.products-section {
    padding: 100px 24px;
    background: #f9f9fb;
}

.section-header {
    text-align: center;
    margin-bottom: 56px;
}

.section-tag {
    display: inline-block;
    padding: 4px 12px;
    background: rgba(102, 126, 234, 0.1);
    color: #667eea;
    border-radius: 6px;
    font-size: 12px;
    font-weight: 600;
    margin-bottom: 16px;
}

.section-header h2 {
    font-size: 36px;
    font-weight: 700;
    margin: 0;
    color: #0d0d0d;
}

.section-header p {
    font-size: 16px;
    color: #666;
    margin-top: 12px;
}

.products-grid {
    display: grid;
    grid-template-columns: repeat(auto-fit, minmax(340px, 1fr));
    gap: 24px;
}

.product-card {
    background: #fff;
    border: 1px solid #eee;
    border-radius: 20px;
    padding: 32px;
    transition: all 0.3s ease;
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.02);
}

.product-card:hover {
    border-color: #ddd;
    box-shadow: 0 12px 32px rgba(0, 0, 0, 0.08);
    transform: translateY(-4px);
}

.card-header {
    margin-bottom: 20px;
}

.card-badge {
    display: inline-block;
    padding: 4px 12px;
    border-radius: 6px;
    font-size: 12px;
    font-weight: 600;
}

.card-badge.primary {
    background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
    color: #fff;
}

.card-badge.hot {
    background: linear-gradient(135deg, #f5576c 0%, #f093fb 100%);
    color: #fff;
}

.card-badge.agent {
    background: linear-gradient(135deg, #22c55e 0%, #16a34a 100%);
    color: #fff;
}

.card-icon {
    width: 56px;
    height: 56px;
    background: linear-gradient(135deg, rgba(102, 126, 234, 0.1) 0%, rgba(118, 75, 162, 0.1) 100%);
    border-radius: 14px;
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 26px;
    color: #667eea;
    margin-bottom: 24px;
}

.product-card h3 {
    font-size: 22px;
    font-weight: 600;
    margin-bottom: 8px;
    color: #0d0d0d;
}

.card-subtitle {
    font-size: 14px;
    color: #888;
    margin-bottom: 24px;
}

.card-features {
    list-style: none;
    padding: 0;
    margin: 0 0 24px 0;
}

.card-features li {
    display: flex;
    align-items: center;
    gap: 12px;
    font-size: 14px;
    color: #555;
    padding: 10px 0;
    border-bottom: 1px solid #f0f0f0;
}

.card-features li:last-child {
    border-bottom: none;
}

.feature-icon {
    color: #667eea;
}

.card-cta {
    display: inline-flex;
    align-items: center;
    gap: 6px;
    font-size: 14px;
    font-weight: 500;
    color: #667eea;
    cursor: pointer;
    transition: color 0.2s;
}

.card-cta:hover {
    color: #8a9af0;
}

/* ========== Skills ========== */
.skills-section {
    padding: 100px 24px;
    background: #fff;
}

.skills-grid {
    display: grid;
    grid-template-columns: repeat(auto-fit, minmax(260px, 1fr));
    gap: 16px;
    margin-bottom: 32px;
}

.skill-item {
    display: flex;
    align-items: flex-start;
    gap: 14px;
    padding: 20px;
    background: #f9f9fb;
    border: 1px solid #eee;
    border-radius: 14px;
    transition: all 0.2s;
}

.skill-item:hover {
    background: #f5f5f8;
    border-color: #ddd;
}

.skill-icon {
    width: 40px;
    height: 40px;
    background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
    border-radius: 10px;
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 18px;
    color: #fff;
    flex-shrink: 0;
}

.skill-info h4 {
    font-size: 15px;
    font-weight: 600;
    margin: 0 0 4px 0;
    color: #0d0d0d;
}

.skill-info p {
    font-size: 13px;
    color: #888;
    margin: 0;
}

.skills-note {
    display: flex;
    align-items: flex-start;
    gap: 16px;
    background: rgba(102, 126, 234, 0.08);
    border: 1px solid rgba(102, 126, 234, 0.15);
    border-radius: 14px;
    padding: 20px 24px;
}

.note-icon {
    width: 40px;
    height: 40px;
    background: #667eea;
    border-radius: 10px;
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 18px;
    color: #fff;
    flex-shrink: 0;
}

.note-text {
    font-size: 14px;
    color: #444;
    line-height: 1.6;
}

/* ========== More ========== */
.more-section {
    padding: 60px 24px;
    background: #f9f9fb;
}

.more-grid {
    display: grid;
    grid-template-columns: repeat(auto-fit, minmax(260px, 1fr));
    gap: 16px;
}

.more-item {
    display: flex;
    align-items: center;
    gap: 12px;
    padding: 18px 22px;
    background: #fff;
    border: 1px solid #eee;
    border-radius: 12px;
    font-size: 14px;
    color: #333;
}

.more-icon {
    font-size: 18px;
    color: #667eea;
}

/* ========== Final CTA ========== */
.final-cta {
    padding: 100px 24px;
    background: #fff;
}

.cta-box {
    text-align: center;
    background: linear-gradient(135deg, rgba(102, 126, 234, 0.08) 0%, rgba(245, 87, 108, 0.08) 100%);
    border: 1px solid #eee;
    border-radius: 32px;
    padding: 64px 48px;
}

.cta-box h2 {
    font-size: 32px;
    font-weight: 700;
    margin-bottom: 12px;
    color: #0d0d0d;
}

.cta-box p {
    font-size: 16px;
    color: #666;
    margin-bottom: 32px;
}

/* ========== Footer ========== */
.footer {
    padding: 48px 24px;
    background: #fff;
    border-top: 1px solid #eee;
}

.footer-content {
    max-width: 1200px;
    margin: 0 auto;
    display: flex;
    justify-content: space-between;
    align-items: center;
}

.footer-logo {
    font-weight: 700;
    font-size: 16px;
    color: #0d0d0d;
}

.footer-left p {
    font-size: 13px;
    color: #888;
    margin-top: 4px;
}

.footer-right a {
    color: #666;
    text-decoration: none;
    font-size: 14px;
}

.footer-right a:hover {
    color: #0d0d0d;
}

/* ========== 响应式 ========== */
@media (max-width: 768px) {
    .hero-title {
        font-size: 36px;
    }

    .hero-stats {
        flex-wrap: wrap;
        gap: 24px;
    }

    .stat-divider {
        display: none;
    }

    .pain-card {
        padding: 32px 24px;
    }

    .footer-content {
        flex-direction: column;
        gap: 16px;
        text-align: center;
    }
}
</style>
