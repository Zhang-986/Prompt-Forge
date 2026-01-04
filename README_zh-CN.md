# Prompt-Forge 🔥

[English](./README.md) | 简体中文

> 企业级 PromptOps 平台，支持团队协作、版本控制和 AI 模型评测。

[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.3.7-brightgreen)](https://spring.io/projects/spring-boot)
[![Vue.js](https://img.shields.io/badge/Vue.js-3.5-blue)](https://vuejs.org/)
[![License](https://img.shields.io/badge/License-MIT-yellow)](LICENSE)

## ✨ 核心功能

- **🗂️ 工作空间管理** - 多租户隔离，支持团队协作
- **📝 Prompt 版本控制** - 类 Git 版本管理，支持提交历史和差异对比
- **🏟️ AI 竞技场** - 多模型并行对比，ELO 评分系统
- **💡 Prompt 教练** - AI 驱动的 Prompt 优化助手
- **🛒 Prompt 广场** - 公共模板市场，一键克隆
- **🏷️ 标签系统** - 灵活的标签管理，方便组织分类
- **🔐 JWT 认证** - 安全的用户认证与角色权限
- **📊 管理后台** - 系统统计与管理控制台

## 🛠️ 技术栈

### 后端
- **框架**: Spring Boot 3.3.7, Spring AI 1.0.0-M5
- **数据库**: MySQL 8.0, MyBatis-Plus 3.5.9
- **缓存**: Redis + Caffeine 二级缓存
- **容错**: Resilience4j (熔断器、限流器)
- **安全**: JWT, BCrypt 加密
- **文档**: SpringDoc OpenAPI 2.3.0

### 前端
- **框架**: Vue 3.5 + TypeScript
- **组件库**: Ant Design Vue 4.x
- **构建工具**: Vite 6.x
- **HTTP 客户端**: Axios

## 🚀 快速开始

### 环境要求
- JDK 17+
- Node.js 18+
- MySQL 8.0+
- Redis 7.0+

### 后端启动

```bash
# 克隆仓库
git clone https://github.com/your-username/prompt-forge.git
cd prompt-forge

# 初始化数据库
mysql -u root -p < sql/init.sql

# 修改 application.yml 配置数据库和 Redis 连接信息

# 启动应用
mvn spring-boot:run
```

### 前端启动

```bash
cd frontend

# 安装依赖
npm install

# 开发模式
npm run dev

# 生产构建
npm run build
```

### 访问地址
- **前端**: http://localhost:5173
- **后端 API**: http://localhost:8080
- **API 文档**: http://localhost:8080/swagger-ui.html

## 📁 项目结构

```
prompt-forge/
├── src/main/java/com/zzk/
│   ├── application/        # 应用服务层
│   ├── domain/             # 领域模型和仓储接口
│   ├── infrastructure/     # 基础设施（持久化、缓存、工具）
│   └── interfaces/         # 接口层（控制器、DTO）
├── frontend/
│   ├── src/
│   │   ├── api/            # API 调用层
│   │   ├── components/     # Vue 组件
│   │   ├── views/          # 页面视图
│   │   └── router/         # 路由配置
│   └── package.json
└── sql/                    # 数据库脚本
```

## 🔑 默认账户

| 用户名 | 密码 | 角色 |
|--------|------|------|
| admin | admin123 | 管理员 |
| demo | demo123 | 普通用户 |

## 📄 许可证

本项目采用 MIT 许可证 - 详情请参阅 [LICENSE](LICENSE) 文件。

## 🤝 贡献指南

欢迎提交 Pull Request！

1. Fork 本仓库
2. 创建特性分支 (`git checkout -b feature/AmazingFeature`)
3. 提交更改 (`git commit -m 'Add some AmazingFeature'`)
4. 推送分支 (`git push origin feature/AmazingFeature`)
5. 提交 Pull Request

---

Made with ❤️ by [zzk](https://github.com/your-username)
