# VoiceOS

> **Multi-Agent Voice AI Operations Platform**
> A production-quality, modular, zero-cost-first voice AI system built on Java 21 + Spring Boot 3.x + React.

---

## What Is VoiceOS?

VoiceOS is a real-time voice AI platform where you speak naturally to a system that can **act — not just talk**.

The system orchestrates specialized AI agents, calls tools, manages long-term memory, retrieves knowledge (RAG), handles human approvals for dangerous actions, and exposes a premium SaaS dashboard.

```
You say:  "Plan my Bangalore trip next Friday."

VoiceOS:  Creates an execution plan → delegates to TravelAgent + CalendarAgent + FinanceAgent
          → searches options → calculates budget → generates itinerary
          → asks for your approval before any booking
```

---

## Architecture Overview

```
USER (Voice / Text)
  ↓
VOICE PROVIDER (Vapi / WebRTC)
  ↓
SPRING BOOT BACKEND (modular monolith)
  ├── Orchestrator          ← Central brain
  ├── Agent Registry        ← 12 specialized agents
  ├── Tool Registry         ← Tools with risk classification
  ├── Memory System         ← Short/Long/Episodic/Semantic
  ├── RAG Pipeline          ← Document ingestion + vector search
  ├── WebSocket Events      ← Real-time execution timeline
  └── Human-in-the-Loop     ← Approval workflow
  ↓
PERSISTENCE
  ├── PostgreSQL            ← Primary relational store
  ├── Redis                 ← Caching / sessions
  └── Qdrant                ← Vector database (RAG + semantic memory)
  ↓
REACT DASHBOARD
```

---

## Technology Stack

| Layer | Technology | Why |
|---|---|---|
| Language | Java 21 (Virtual Threads) | High-concurrency agent I/O without reactive complexity |
| Framework | Spring Boot 3.3.x | Enterprise-grade, modular, full ecosystem |
| AI Framework | Spring AI 1.0.0 | Portable LLM abstraction, native Micrometer |
| Primary LLM | Google Gemini Flash (free tier) | Generous TPD, free API key from AI Studio |
| Fallback LLM | Groq Llama 3.3 (free tier) | High-speed inference, no credit card required |
| Voice | Vapi (WebRTC/SIP) | Enterprise voice AI platform, ultra-low latency |
| Database | PostgreSQL 16 | Reliable, JSONB for flexible metadata |
| Cache | Redis 7 | Session storage, rate limiting |
| Vector DB | Qdrant | Open-source, Docker, fast similarity search |
| Build | Maven 3.x | |
| Frontend | React + Vite | Fast, modern |
| API Docs | SpringDoc OpenAPI 3 | Auto-generated Swagger UI |
| Migrations | Flyway | Safe schema versioning |
| Observability | Micrometer + Prometheus | Token usage, latency, agent metrics |
| Security | Spring Security + JWT (JJWT) | Stateless, BCrypt passwords |

---

## Zero-Cost Development

**Everything runs for free:**

| Service | Free Path |
|---|---|
| PostgreSQL | Local Docker |
| Redis | Local Docker |
| Qdrant | Local Docker (open-source) |
| Voice Provider | Vapi (Web/Phone/SIP) |
| Gemini LLM | AI Studio free tier (~15 RPM) |
| Groq LLM | Free tier (no credit card) |
| STT | Groq Whisper free tier |
| TTS | Browser Web Speech API (free) |
| All external APIs | Mock mode (`MOCK_EXTERNAL_APIS=true`) |

**Total cost: $0**

---

## Quick Start

### Prerequisites

- Java 21 (OpenJDK or Eclipse Temurin)
- Maven 3.9+
- Docker + Docker Compose

### 1. Clone and configure

```bash
git clone https://github.com/your-username/voiceos
cd voiceos
cp .env.example .env
# Edit .env — add your GEMINI_API_KEY and/or GROQ_API_KEY
```

### 2. Start infrastructure

```bash
docker compose up -d postgres redis qdrant
```

### 3. Run the backend

```bash
cd voiceos-backend
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

### 4. Access

- **API**: http://localhost:8080
- **Swagger UI**: http://localhost:8080/swagger-ui/index.html
- **Health**: http://localhost:8080/actuator/health

---

## API Overview

| Method | Endpoint | Description |
|---|---|---|
| POST | `/api/v1/auth/register` | Register new user |
| POST | `/api/v1/auth/login` | Login + receive JWT |
| POST | `/api/v1/auth/refresh` | Refresh JWT token |
| GET | `/api/v1/system/status` | Provider health status |
| POST | `/api/v1/conversations` | Start a new conversation |
| GET | `/api/v1/conversations/{id}` | Get conversation details |
| POST | `/api/v1/agent/execute` | Execute an agent request |
| GET | `/api/v1/agents` | List available agents |
| GET | `/api/v1/tools` | List available tools |
| POST | `/api/v1/approvals/{id}/approve` | Approve a pending action |
| POST | `/api/v1/approvals/{id}/reject` | Reject a pending action |
| GET | `/api/v1/memory` | List user memories |
| POST | `/api/v1/memory` | Save a memory |
| GET | `/api/v1/tasks` | List user tasks |
| GET | `/api/v1/metrics` | AI quality metrics |

---

## Key Features

### Agent System

12 specialized agents orchestrated by a central `OrchestratorService`:

- **ConversationAgent** — general conversation fallback
- **ResearchAgent** — web research and summarization
- **TravelAgent** — trip planning and itinerary generation
- **CalendarAgent** — schedule management
- **TaskAgent** — task creation and tracking
- **EmailAgent** — email drafting (requires approval to send)
- **FinanceAgent** — budget calculations
- **MemoryAgent** — explicit memory save/retrieve
- **DeveloperAgent** — GitHub analysis, architecture review
- **SupportAgent** — help and guidance
- **EvaluationAgent** — response quality scoring

### Human-in-the-Loop

All tools are classified by risk level:

| Risk | Examples | Behavior |
|---|---|---|
| LOW | Web search, calculations | Executes immediately |
| MEDIUM | Create task, calendar event | Executes immediately |
| HIGH | Send email, modify external records | **Requires approval** |
| CRITICAL | Financial transactions, destructive ops | **Requires approval** |

### Memory System

Four memory types:
- **Short-term** — current conversation context (Redis, expires)
- **Long-term** — user preferences and persistent facts (PostgreSQL)
- **Episodic** — records of completed tasks
- **Semantic** — document knowledge (Qdrant vector search)

### RAG Pipeline

```
Document (PDF/TXT/MD) → Parser → Chunker → Embedding → Qdrant → Retriever → LLM Response
```

Ask: _"What does my resume say about Spring Boot?"_ → Gets relevant chunks → Answers with source references.

---

## Environment Variables

See [`.env.example`](.env.example) for all variables. Required for full functionality:

```env
GEMINI_API_KEY=         # From https://aistudio.google.com
GROQ_API_KEY=           # From https://console.groq.com
JWT_SECRET=             # Random 64+ character string
```

---

## Development Build Phases

| Phase | Status | Scope |
|---|---|---|
| 1 | ✅ Complete | Foundation — Spring Boot, security, DB, provider abstractions |
| 2-7 | ✅ Complete | Vapi Voice Webhooks, Tool Registry, Multi-Agent System |
| 8-10| ✅ Complete | Memory Persistence, Human Approvals, Execution Tracing |
| 11 | ✅ Complete | Modular React Dashboard with WebSockets |
| 12-14| ✅ Complete | RAG (Qdrant), Scheduled Workflows, Evaluation Metrics |
| 15 | ✅ Complete | Final docs and deployment hardening |

---

## Resume Highlights

This project demonstrates:

✅ Real-time voice AI (Vapi WebRTC integration)  
✅ Multi-agent orchestration (12 specialized agents)  
✅ LLM integration (Google Gemini + Groq with fallback)  
✅ Spring AI framework (Spring Boot ecosystem)  
✅ Tool calling with risk classification  
✅ RAG pipeline (document ingestion + Qdrant vector search)  
✅ Long-term memory management (4 memory types)  
✅ Human-in-the-loop approval workflow  
✅ WebSockets for real-time execution streaming  
✅ Webhook processing (idempotency, signature verification)  
✅ JWT authentication + Spring Security  
✅ PostgreSQL with Flyway migrations  
✅ Redis caching and session management  
✅ Qdrant vector database  
✅ AI evaluation framework (quality metrics)  
✅ Docker Compose for full local stack  
✅ Java 21 Virtual Threads  
✅ Clean architecture (interfaces, no god classes)  
✅ Comprehensive error handling (no System.out.println)  

---

## Documentation

- [Architecture](docs/architecture.md)
- [Setup Guide](docs/setup.md)
- [Agent System](docs/agents.md)
- [Tool System](docs/tools.md)
- [Memory System](docs/memory.md)
- [RAG Pipeline](docs/rag.md)
- [Webhooks](docs/webhooks.md)
- [Security](docs/security.md)
- [API Reference](docs/api.md)
- [Deployment](docs/deployment.md)

---

## License

MIT License — see [LICENSE](LICENSE).
