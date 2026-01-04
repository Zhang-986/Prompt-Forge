# Prompt-Forge

[简体中文](./README_zh-CN.md) | English

A full-stack Prompt management platform with version control, AI Arena, and team collaboration.

## What's This?

Managing prompts in a team is painful - different versions flying around in Slack, no way to track what changed, can't compare which prompt actually works better. Prompt-Forge solves this.

**Core Features:**
- Version control for prompts (like Git, but for prompts)
- AI Arena - pit different AI models against each other with ELO ratings
- Prompt Coach - let AI help you improve your prompts
- Template marketplace - share and clone prompts
- Multi-tenant workspaces for team collaboration

## Tech Stack

**Backend:** Spring Boot 3.3 + Spring AI + MyBatis-Plus + Redis + MySQL

**Frontend:** Vue 3 + TypeScript + Vite + Ant Design Vue

## Quick Start

### Prerequisites
- JDK 17+
- Node.js 18+
- MySQL 8.0+
- Redis

### Setup

```bash
# Clone
git clone https://github.com/Zhang-986/prompt-forge.git
cd prompt-forge

# Database
mysql -u root -p < sql/init.sql

# Backend (update application.yml with your DB/Redis config first)
mvn spring-boot:run

# Frontend
cd frontend && npm install && npm run dev
```

**Access:**
- Frontend: http://localhost:5173
- API Docs: http://localhost:8080/swagger-ui.html

## Test Accounts

| User | Password | Role |
|------|----------|------|
| admin | admin123 | Admin |
| demo | demo123 | Member |

## Project Structure

```
├── src/main/java/com/zzk/
│   ├── application/        # Business logic
│   ├── domain/             # Domain models
│   ├── infrastructure/     # DB, cache, utils
│   └── interfaces/         # REST controllers
├── frontend/src/
│   ├── api/                # API calls
│   ├── views/              # Pages
│   └── components/         # Components
└── sql/                    # DB scripts
```

## License

MIT
