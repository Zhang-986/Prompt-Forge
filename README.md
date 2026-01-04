# Prompt-Forge 🔥

[简体中文](./README_zh-CN.md) | English

> Enterprise-grade PromptOps Platform for collaborative prompt engineering, version control, and AI model evaluation.

[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.3.7-brightgreen)](https://spring.io/projects/spring-boot)
[![Vue.js](https://img.shields.io/badge/Vue.js-3.5-blue)](https://vuejs.org/)
[![License](https://img.shields.io/badge/License-MIT-yellow)](LICENSE)

## ✨ Features

- **🗂️ Workspace Management** - Multi-tenant workspaces for team collaboration
- **📝 Prompt Version Control** - Git-like versioning with commit history and diff view
- **🏟️ AI Arena** - Compare multiple AI models side-by-side with ELO ratings
- **💡 Prompt Coach** - AI-powered prompt optimization assistant
- **🛒 Prompt Plaza** - Public template marketplace with cloning support
- **🏷️ Tag System** - Flexible tagging for prompt organization
- **🔐 JWT Authentication** - Secure user authentication with role-based access
- **📊 Admin Dashboard** - System statistics and management console

## 🛠️ Tech Stack

### Backend
- **Framework**: Spring Boot 3.3.7, Spring AI 1.0.0-M5
- **Database**: MySQL 8.0, MyBatis-Plus 3.5.9
- **Cache**: Redis, Caffeine (L1/L2 Cache)
- **Resilience**: Resilience4j (Circuit Breaker, Rate Limiter)
- **Security**: JWT, BCrypt
- **API Docs**: SpringDoc OpenAPI 2.3.0

### Frontend
- **Framework**: Vue 3.5 + TypeScript
- **UI Library**: Ant Design Vue 4.x
- **Build Tool**: Vite 6.x
- **HTTP Client**: Axios

## 🚀 Quick Start

### Prerequisites
- JDK 17+
- Node.js 18+
- MySQL 8.0+
- Redis 7.0+

### Backend Setup

```bash
# Clone the repository
git clone https://github.com/Zhang-986/prompt-forge.git
cd prompt-forge

# Configure database
mysql -u root -p < sql/init.sql

# Update application.yml with your MySQL/Redis credentials

# Run the application
mvn spring-boot:run
```

### Frontend Setup

```bash
cd frontend

# Install dependencies
npm install

# Development server
npm run dev

# Production build
npm run build
```

### Access Points
- **Frontend**: http://localhost:5173
- **Backend API**: http://localhost:8080
- **API Docs**: http://localhost:8080/swagger-ui.html

## 📁 Project Structure

```
prompt-forge/
├── src/main/java/com/zzk/
│   ├── application/        # Application services
│   ├── domain/             # Domain models & repositories
│   ├── infrastructure/     # Persistence, cache, utils
│   └── interfaces/         # Controllers & DTOs
├── frontend/
│   ├── src/
│   │   ├── api/            # API layer
│   │   ├── components/     # Vue components
│   │   ├── views/          # Page views
│   │   └── router/         # Vue Router
│   └── package.json
└── sql/                    # Database scripts
```

## 🔑 Default Accounts

| Username | Password | Role |
|----------|----------|------|
| admin | admin123 | ADMIN |
| demo | demo123 | MEMBER |

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## 🤝 Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

1. Fork the repository
2. Create your feature branch (`git checkout -b feature/AmazingFeature`)
3. Commit your changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to the branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request

---

Made with ❤️ by [zzk](https://github.com/your-username)
