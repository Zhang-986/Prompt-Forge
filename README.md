# 🚀 Prompt-Forge

> 企业级 Prompt 协同平台 | Enterprise PromptOps Platform

## 📋 项目简介

Prompt-Forge 是一个高复杂度的 PromptOps 系统，帮助开发者通过"控制变量法"和"并行竞技"来迭代 Prompt。

### ✨ 核心特性

| 特性 | 描述 |
|------|------|
| 🏟️ **多模型竞技场** | 同时调用 GPT-4、DeepSeek、Claude 等多个 AI 模型并行作答 |
| 🌲 **类 Git 版本控制** | 链式存储结构，支持 Diff 对比、版本回溯 |
| ⚡ **高可用设计** | Resilience4j 熔断降级、限流保护 |
| 💾 **多级缓存** | Caffeine + Redis 二级缓存架构 |
| 🔐 **企业级权限** | RBAC 权限控制 + 多租户数据隔离 |
| 📊 **异步任务** | 消息队列 + 任务状态机 |

---

## 🏗️ 技术架构

### 技术栈

| 类别 | 技术 | 版本 |
|------|------|------|
| 语言 | Java | 17 |
| 框架 | Spring Boot | 3.3.7 |
| AI | Spring AI | 1.0.0-M4 |
| ORM | MyBatis-Plus | 3.5.9 |
| 缓存 | Redis + Caffeine | - |
| 熔断 | Resilience4j | 2.2.0 |
| 分布式锁 | Redisson | 3.37.0 |
| API 文档 | SpringDoc OpenAPI | 2.3.0 |

### DDD 分层架构

```
┌─────────────────────────────────────────────────────┐
│                  Interface Layer                     │
│              (Controllers, DTOs)                     │
├─────────────────────────────────────────────────────┤
│                 Application Layer                    │
│        (AppService, Command, Assembler)              │
├─────────────────────────────────────────────────────┤
│                   Domain Layer                       │
│  (Aggregate, Entity, ValueObject, DomainService)     │
├─────────────────────────────────────────────────────┤
│               Infrastructure Layer                   │
│   (AI Strategy, Repository, Cache, Lock, Config)     │
└─────────────────────────────────────────────────────┘
```

---

## 🚀 快速开始

### 环境要求

- JDK 17+
- Maven 3.8+
- MySQL 8.0+
- Redis 6.0+

### 1. 克隆项目

```bash
git clone https://github.com/your-username/prompt-forge.git
cd prompt-forge
```

### 2. 初始化数据库

```bash
mysql -u root -p < sql/init.sql
```

### 3. 配置应用

编辑 `src/main/resources/application-dev.yml`，配置数据库和 Redis 连接信息。

### 4. 启动应用

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

### 5. 访问 API 文档

打开浏览器访问：http://localhost:8080/swagger-ui.html

---

## 📁 项目结构

```
prompt-forge/
├── pom.xml
├── sql/
│   └── init.sql                 # 数据库初始化脚本
└── src/main/java/com/zzk/
    ├── PromptForgeApplication.java
    ├── interfaces/              # 接口层
    ├── application/             # 应用层
    ├── domain/                  # 领域层
    └── infrastructure/          # 基础设施层
```

---

## 🔑 核心 API

### 竞技场 API

```http
POST /api/arena/compete
Content-Type: application/json

{
  "promptVersionId": 1,
  "variables": {"topic": "人工智能"},
  "models": ["gpt-4", "deepseek", "claude"]
}
```

### Prompt 版本 API

```http
POST /api/prompts/{promptId}/commit
Content-Type: application/json

{
  "content": "请用{{style}}风格介绍{{topic}}",
  "parentVersionId": 1,
  "commitMessage": "优化了语气"
}
```

---

## 📄 License

MIT License

---

## 👨‍💻 作者

Created with ❤️ by zzk
