# Prompt-Forge

[English](./README.md) | 简体中文

一个带版本控制、AI 竞技场、团队协作的 Prompt 管理平台。

## 这是什么？

团队里管理 Prompt 很头疼——各种版本在群里飞，改了什么没人知道，哪个版本效果好也说不清楚。Prompt-Forge 就是来解决这个问题的。

**主要功能：**
- Prompt 版本控制（类似 Git）
- AI 竞技场 - 让不同模型 PK，看谁效果好
- Prompt 教练 - AI 帮你优化 Prompt
- 模板广场 - 分享和克隆 Prompt
- 工作空间 - 团队协作

## 技术栈

**后端:** Spring Boot 3.3 + Spring AI + MyBatis-Plus + Redis + MySQL

**前端:** Vue 3 + TypeScript + Vite + Ant Design Vue

## 快速开始

### 环境
- JDK 17+
- Node.js 18+
- MySQL 8.0+
- Redis

### 启动

```bash
# 克隆
git clone https://github.com/Zhang-986/prompt-forge.git
cd prompt-forge

# 导入数据库
mysql -u root -p < sql/init.sql

# 后端（先改 application.yml 里的数据库和 Redis 配置）
mvn spring-boot:run

# 前端
cd frontend && npm install && npm run dev
```

**访问地址:**
- 前端: http://localhost:5173
- API 文档: http://localhost:8080/swagger-ui.html

## 测试账号

| 用户 | 密码 | 角色 |
|------|------|------|
| admin | admin123 | 管理员 |
| demo | demo123 | 普通用户 |

## 目录结构

```
├── src/main/java/com/zzk/
│   ├── application/        # 业务逻辑
│   ├── domain/             # 领域模型
│   ├── infrastructure/     # 数据库、缓存、工具
│   └── interfaces/         # 控制器
├── frontend/src/
│   ├── api/                # API 调用
│   ├── views/              # 页面
│   └── components/         # 组件
└── sql/                    # 数据库脚本
```

## License

MIT
