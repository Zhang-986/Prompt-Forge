# 📋 PromptForge TODO List

> 最后更新: 2025-12-26

---

## ✅ 已完成功能

- [x] 用户注册/登录 (JWT 认证)
- [x] 工作空间管理 (创建/列表)
- [x] Prompt CRUD 操作
- [x] 类 Git 版本控制 (commit/rollback/diff)
- [x] 多模型竞技场 (SSE 流式对比)
- [x] 用户自定义 AI API 配置 (5 种提供商)
- [x] 移除系统级 API Key，完全使用用户配置

---

## 🚧 待完善功能

### 高优先级 ⭐⭐⭐

- [x] **测试覆盖**
  - [x] 后端单元测试 (JUnit 5)
  - [x] 集成测试 (MockMvc)
  - [x] 前端组件测试

- [x] **错误处理优化**
  - [x] 统一 API 错误响应格式
  - [x] 前端全局错误提示
  - [x] AI 调用失败重试策略完善

- [x] **UI/UX 优化**
  - [x] 响应式设计 (移动端适配)
  - [x] 深色/浅色主题切换
  - [x] Loading 状态优化

### 中优先级 ⭐⭐

- [ ] **权限与协作**
  - [ ] RBAC 权限控制 UI
  - [ ] 邀请成员加入工作空间
  - [ ] 工作空间角色管理 (Admin/Member/Viewer)

- [ ] **Prompt 增强**
  - [x] Prompt 标签分类
  - [ ] Prompt 模板库 (公共模板)
  - [x] Prompt 导入/导出

- [x] **竞技场增强**
  - [x] 对比结果保存与历史查看
  - [x] Token 消耗统计
  - [x] 响应时间可视化对比

### 低优先级 ⭐

- [ ] **数据分析**
  - [ ] 用户使用统计仪表盘
  - [ ] Prompt 使用频率分析
  - [ ] AI 模型效果对比报告

- [ ] **异步任务**
  - [ ] 批量 Prompt 测试
  - [ ] 后台任务状态展示
  - [ ] 任务队列管理

- [ ] **API 文档**
  - [ ] Swagger UI 完善
  - [ ] API 使用示例
  - [ ] 开发者文档

---

## 💡 创意功能 (Future)

- [ ] AI 自动优化 Prompt 建议
- [ ] Prompt A/B 测试功能
- [ ] 团队协作实时编辑
- [ ] Webhook 集成 (Slack/Discord 通知)
- [ ] CLI 工具支持

---

## 🐛 已知问题

- [ ] 暂无明显 Bug

---

## 📝 备注

- 技术栈: Spring Boot 3.3 + Vue3 + TypeScript
- 支持的 AI 模型: Google Gemini, 智谱 GLM, DeepSeek, OpenAI, Claude
- 数据库: MySQL 8.0 + Redis

---

*Good luck with your Vibe Coding journey! 🚀*
