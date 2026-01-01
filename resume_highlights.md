# Prompt-Forge 智能化 Prompt 协同工程平台 | Java 后端开发

**项目描述**：基于 **Spring Boot 3 + DDD (领域驱动设计)** 构建的企业级 Prompt 协同与优化平台。通过 **Reactive 响应式编程** 与 **策略模式** 实现了多模型（GPT-4, Claude, Gemini, DeepSeek）的统一接入与流式调度，解决了 Prompt 版本混乱、模型接口异构及调用高并发下的稳定性问题。

**深度实践 DDD 领域驱动设计**，重构传统 CRUD 架构。定义 `Prompt` 为核心聚合根，设计 **Git-like 版本控制模型**（HEAD 指针 + 增量版本），将版本回滚、权限校验等业务逻辑内聚于领域内部，有效降低了系统的耦合度与维护成本。

- **基于 WebClient + Flux 构建高并发 AI 网关**，采用 **动态策略模式 (Dynamic Strategy Pattern)** 统一封装 Google、OpenAI、Claude 等异构 API。通过 **Server-Sent Events (SSE)** 实现全链路流式响应，将大模型推理的首字延迟（TTFT）在大并发场景下降低至毫秒级。

- **实现高可用级联降级策略**，设计 `Priority Chain` 模型选择算法（GPT-4 -> Claude -> Gemini）。结合 Reactor 的 **Exponential Backoff Retry (指数退避重试)** 机制，自动处理上游 "429 Too Many Requests" 限流异常，保障核心业务的高可用性。

  **研发 Meta-Prompt 提示词优化引擎**，基于元提示词技术自动重写用户输入。通过 **Function Calling** (预留扩展点) 与 **Template Engine** 实现结构化 Prompt 生成，显著提升了普通用户的 Prompt 质量与模型输出准确率。
