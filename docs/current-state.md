# VoiceOS Current State — Repository Audit

**Audit date:** 2026-09-17  
**Last verified:** 2026-09-19 Phase 7 real intelligence + safe tool calling (real Gemini/Groq **UNVERIFIED** in this run; live Vapi microphone loop still **PARTIAL / UNVERIFIED**)  
**Scope:** Entire existing repository (`D:\vapi`).  
**Evidence standard:** A feature is **complete** only if it is implemented **and** tested. Existing classes, UI cards, and configuration files are not completion evidence. Live Vapi is complete only after an actual microphone call.

**Phase 0 audit compile (before 0B):**

```
mvnw.cmd compile  (JAVA_HOME=C:\Program Files\Java\jdk-26.0.2)
BUILD FAILURE
20 compilation errors
Finished at: 2026-09-17T21:37:12+05:30
```

**Phase 0B compile and tests (after recovery):**

```
mvnw.cmd clean compile   BUILD SUCCESS  (2026-09-17T21:42:40+05:30)
mvnw.cmd test            BUILD SUCCESS  Tests run: 18, Failures: 0, Errors: 0, Skipped: 0
mvnw.cmd package         BUILD SUCCESS  jar: voiceos-backend-1.0.0-SNAPSHOT.jar
npm run build (frontend) SUCCESS        vite v8.2.1, 6.08s
```

**Phase 1 compile and tests (security foundation):**

```
mvnw.cmd clean compile   BUILD SUCCESS  (2026-09-17T22:12:24+05:30)
mvnw.cmd test            BUILD SUCCESS  Tests run: 46, Failures: 0, Errors: 0, Skipped: 0
mvnw.cmd package         BUILD SUCCESS  (2026-09-17T22:19:30+05:30)  jar: voiceos-backend-1.0.0-SNAPSHOT.jar
npm run build (frontend) SUCCESS        vite v8.2.1, 3.06s
```

**Phase 2 compile and tests (execution pipeline):**

```
mvnw.cmd clean compile   BUILD SUCCESS  (2026-09-17T22:49:22+05:30)
mvnw.cmd test            BUILD SUCCESS  Tests run: 61, Failures: 0, Errors: 0, Skipped: 0
mvnw.cmd package         BUILD SUCCESS  (2026-09-17T22:55:43+05:30)  jar: voiceos-backend-1.0.0-SNAPSHOT.jar
npm run build (frontend) SUCCESS        vite v8.2.1, 2.33s
```

Canonical path is now Vapi/HTTP → ActionEngine → PolicyEngine → Agent → Tool → Provider → Verification. Calculator `125 * 24` = `3000.00` is real local arithmetic. Live Vapi is **NOT VERIFIED**. Travel/email/WhatsApp/calendar/payment are not real. Product-level completion remains **not production-ready**.

See `docs/reports/2026-09-17-phase-2-execution-pipeline.md`.

**Phase 3 compile and tests (action steps + audit events):**

```
mvnw.cmd clean compile   BUILD SUCCESS  (2026-09-17T23:12:04+05:30)
mvnw.cmd test            BUILD SUCCESS  Tests run: 67, Failures: 0, Errors: 0, Skipped: 0
mvnw.cmd package         BUILD SUCCESS  (2026-09-17T23:19:27+05:30)  jar: voiceos-backend-1.0.0-SNAPSHOT.jar
npm run build (frontend) SUCCESS        vite v8.2.1, 3.07s
```

Actions now persist ordered `ActionStep` rows and append-only `AuditEvent` rows (Flyway V4). Owner-scoped `GET /api/v1/actions/{id}/timeline` reconstructs execution. Calculator success, policy deny, provider failure, user isolation, and duplicate-webhook idempotency are covered by tests. Live Vapi remains **NOT VERIFIED**. No new external providers.

See `docs/reports/2026-09-17-phase-3-action-steps-audit-events.md`.

**Phase 4 compile and tests (provider abstraction):**

```
mvnw.cmd clean compile   BUILD SUCCESS  (2026-09-17T23:42:58+05:30)
mvnw.cmd test            BUILD SUCCESS  Tests run: 83, Failures: 0, Errors: 0, Skipped: 0
mvnw.cmd package         BUILD SUCCESS  (2026-09-17T23:47:17+05:30)  jar: voiceos-backend-1.0.0-SNAPSHOT.jar
npm run build (frontend) SUCCESS        vite v8.2.1, 2.03s
```

Provider layer is explicit: Tool → ProviderRequest → ServiceProvider → ProviderResult → ToolResult. Calculator remains **REAL** local arithmetic (`125 * 24` = `3000.00`). Travel/WhatsApp/Email/Calendar/Payment are honest **MOCK** (no silent REAL→MOCK fallback). Live Vapi remains **NOT VERIFIED**. No new external APIs.

See `docs/reports/2026-09-17-phase-4-provider-abstraction.md`.

**Phase 5 compile and tests (agent contracts):**

```
mvnw.cmd clean compile   BUILD SUCCESS  (2026-09-18T00:20:13+05:30)
mvnw.cmd test            BUILD SUCCESS  Tests run: 102, Failures: 0, Errors: 0, Skipped: 0
mvnw.cmd package         BUILD SUCCESS  (2026-09-18T00:32:07+05:30)  jar: voiceos-backend-1.0.0-SNAPSHOT.jar
npm run build (frontend) SUCCESS        vite v8.2.1, 12.17s
```

Agent layer is now a structured contract: `AgentRequest` → `Agent` → `AgentResult` (`EXECUTE` / `NEEDS_INFORMATION` / `NEEDS_USER_CONFIRMATION` / `FAILED` / `REJECTED` / `NOT_IMPLEMENTED`). Selection is deterministic (tool ownership → canHandle → ConversationAgent; lower priority wins, then name). Calculator remains **REAL** (`125 * 24` = `3000.00` via UtilityAgent). FinanceAgent no longer invents `$258`. Incomplete agents return `NOT_IMPLEMENTED`. Live Vapi remains **NOT VERIFIED**. No LLM integration. No new external APIs.

See `docs/reports/2026-09-17-phase-5-agent-contracts.md`.

**Phase 6 compile and tests (universal query routing):**

```
mvnw.cmd clean compile   BUILD SUCCESS  (2026-09-18)
mvnw.cmd test            BUILD SUCCESS  Tests run: 128, Failures: 0, Errors: 0, Skipped: 0
mvnw.cmd package         BUILD SUCCESS  jar: voiceos-backend-1.0.0-SNAPSHOT.jar
npm run build (frontend) SUCCESS        vite v8.2.1, 3.45s
```

Every unmatched utterance now falls through to `GeneralQueryAgent` (ConversationAgent removed). Specialized agents still win on their domains. General answers use existing `LLMProvider` (tests/dev default **MOCK**, labeled `[MOCK DATA]`; missing real ChatModel is **UNAVAILABLE**, no silent MOCK fallback). Calculator remains **REAL** `3000.00`. Weather/current facts are not fabricated. Live human Vapi loop remains **PARTIAL / UNVERIFIED**. VoiceOS cannot answer everything.

See `docs/reports/2026-09-18-phase-6-universal-query-agent.md`.

**Phase 7 compile and tests (real intelligence + safe tool calling):**

```
mvnw.cmd clean compile   BUILD SUCCESS  (2026-09-19)
mvnw.cmd test            BUILD SUCCESS  Tests run: 142, Failures: 0, Errors: 0, Skipped: 1
mvnw.cmd package         BUILD SUCCESS  jar: voiceos-backend-1.0.0-SNAPSHOT.jar
npm run build (frontend) SUCCESS        vite v8.2.1, 4.25s
```

GeneralQueryAgent may request server-allowlisted `calculator` through ActionEngine → PolicyEngine → ToolRegistry → RealLocalCalculatorProvider (**REAL** `3000.00`) → LLM continuation. Specialized UtilityAgent still owns “Calculate 125 multiplied by 24.” Model output is structured and untrusted. Mock LLM remains default in tests/dev. Optional real LLM test skipped (`VOICEOS_REAL_LLM_TEST` missing). Live human Vapi remains **PARTIAL / UNVERIFIED**. VoiceOS cannot answer everything.

See `docs/reports/2026-09-19-phase-7-real-intelligence-tool-use.md`.

**Track A Item 1 — Live Vapi verification (2026-09-18):**

```
mvnw.cmd test            BUILD SUCCESS  Tests run: 106, Failures: 0, Errors: 0, Skipped: 0
mvnw.cmd package         BUILD SUCCESS  jar: voiceos-backend-1.0.0-SNAPSHOT.jar
npm run build (frontend) SUCCESS        vite v8.2.1, 3.88s
```

Live calculator voice loop is **PARTIAL**, not PASS. Local `.env` Vapi keys, ngrok public HTTPS, assistant calculator tool, WebRTC bind, and public webhook secret checks are SET. A bound public `tool-calls` webhook executed UtilityAgent / calculator / RealLocalCalculatorProvider REAL `3000.00`. No microphone utterance and no spoken `3000` were heard in the automated browser. See `docs/reports/2026-09-18-live-vapi-verification.md`.

See `docs/reports/2026-09-18-live-vapi-verification.md`.

---

## 1. Repository structure

```
D:\vapi\
├── README.md                          # Claims phases 1–15 complete; overstates readiness
├── .env.example                       # Present; mixed REQUIRED/OPTIONAL; LiveKit leftover
├── .gitignore
├── docker-compose.yml                 # postgres:16, redis:7, qdrant; backend/frontend behind profile "full"
├── start-all.ps1                      # Starts docker + backend + frontend
├── start-backend.ps1
├── patch_vapi.py                      # Loads VAPI_* from environment; no committed key
├── vapi_patch.json                    # Placeholder env vars only
├── docs\
│   ├── current-state.md               # This file (replaces stale prior version)
│   ├── implementation-roadmap.md
│   ├── upgrade-plan.md                # Prior plan; several items now outdated
│   └── vapi-architecture.md           # Target architecture; several claims untrue vs code
├── voiceos-backend\                   # Spring Boot 3.3.4 / Java 21 / Maven
│   ├── pom.xml
│   ├── Dockerfile
│   ├── src\main\java\com\voiceos\     # 118 Java source files
│   ├── src\main\resources\
│   │   ├── application.yml
│   │   ├── application-dev.yml
│   │   └── db\migration\V1__init_schema.sql
│   └── src\test\java\com\voiceos\     # 4 test classes only
└── voiceos-frontend\                  # React 19 + Vite 8
    ├── package.json
    ├── vite.config.js
    └── src\
        ├── App.jsx
        ├── pages\                     # 7 pages; 3 are placeholders
        ├── components\
        └── services\WebSocketService.js
```

**Missing from repository (claimed in README, not present):**

- `docs/architecture.md`, `docs/setup.md`, `docs/agents.md`, `docs/tools.md`, `docs/memory.md`, `docs/rag.md`, `docs/webhooks.md`, `docs/security.md`, `docs/api.md`, `docs/deployment.md`
- `.github/` CI workflows
- `voiceos-frontend/Dockerfile` (compose references it)
- Flyway migrations after `V1`
- LICENSE file (README claims MIT)

**Not a new project.** This is an existing Spring Boot + React + Vapi codebase. LiveKit is **not** used as a runtime provider. A leftover `livekit.version` Maven property and `WEBHOOK_LIVEKIT_SECRET` env var remain.

---

## 2. Current architecture (as implemented)

### Intended (from docs and comments)

```
USER → VAPI → Spring Boot webhook → Orchestrator → Agent → Tool → Provider → Result → VAPI + Frontend
```

### Actual (from source)

Two **disconnected** execution paths exist:

**Path A — used by Vapi and the frontend console**

```
USER (text in ConsolePage, or Vapi tool-call)
  → AgentController / VapiToolService
  → OrchestratorService
  → AgentRegistry.findHandler()          # keyword matching
  → Agent.execute()
  → ToolRegistry.executeTool()           # tools contain mock/hardcoded data
  → PostgreSQL (tasks/memory/approvals if user exists)
```

`ActionEngine`, `PolicyEngine`, and `ServiceProvider` implementations are **not** called by `OrchestratorService` or `VapiToolService`.

**Path B — unused by Vapi/frontend**

```
ActionEngine.initiateAction()
  → ActionPlanner (keyword if/else)
  → PolicyEngine (keyword if/else)
  → pause in REQUIRES_PAYMENT / REQUIRES_AUTHENTICATION / REQUIRES_APPROVAL
  → ActionExecutor (in-memory provider map)
  → TravelProvider / WhatsAppProvider
  → ResultVerifier (non-empty string = success)
```

Actions live in a `ConcurrentHashMap`. They are **not** persisted. No REST controller exposes `ActionEngine`.

### Layer responsibilities vs code

| Layer | Required role | Actual |
|---|---|---|
| Vapi | Voice, STT, TTS, conversation, tool calls | Frontend SDK can start a call if public key + assistant ID are set. Backend webhook **compiles**. `VapiVoiceProvider.terminateSession` is a no-op. `isAvailable()` always returns `true`. Live voice **NOT VERIFIED**. |
| Spring Boot | Orchestration, agents, tools, policy, persistence | Orchestrator is keyword-based. Several agents fail to compile. Policy engine is unused by Path A. |
| React | Operations dashboard | Console + Approvals call backend. Agents/Tasks/RAG/Metrics/Accounts are static or placeholder. No “Test Agent” buttons. |

---

## 3. Existing agents

All agents are Spring `@Component` beans discovered by `AgentRegistry`. Routing is **English keyword `contains()`**, not intent classification.

| Agent | File | `canHandle` | Execute behavior | Compiles | Tests |
|---|---|---|---|---|---|
| TravelAgent | `agent/impl/TravelAgent.java` | trip/flight/hotel/travel/bangalore/vacation/itinerary | Calls `search_web` then Mock/LLM synthesis | Yes (if rest of module compiled) | Routing only (`AgentRegistryTest`) |
| TaskAgent | `agent/impl/TaskAgent.java` | task/todo/remind/checklist | Calls `task_manager` | Yes | None |
| EmailAgent | `agent/impl/EmailAgent.java` | email/mail to/send a message to | Hardcodes recipient `john@example.com` (or `ganeshmaheshwaram@gmail.com` if input contains “ganesh”). Returns approval-required. Does not send. | Yes | None |
| FinanceAgent | `agent/impl/FinanceAgent.java` | budget/cost/expense/price/calculate/total | **Always** computes `68+140+50` and reports `$258.00`. Ignores user amounts. | Yes | None |
| ResearchAgent | `agent/impl/ResearchAgent.java` | research/competitor/analyze company/market/investigate | `search_web` + `document_search` + LLM | Yes | None |
| CalendarAgent | `agent/impl/CalendarAgent.java` | schedule/calendar/meeting/free slot/appointment | Always `check_schedule` for **today** | Yes | None |
| MemoryAgent | `agent/impl/MemoryAgent.java` | remember/preference/forget | `memory_save` / `memory_search` / `memory_delete` | Yes | None |
| DeveloperAgent | `agent/impl/DeveloperAgent.java` | github/repository/repo/PR/architecture/code review | Always inspects hardcoded `voiceos/core` | Yes | None |
| SupportAgent | `agent/impl/SupportAgent.java` | help/how do i/support/features/docs | LLM prompt only; no tools | Yes | None |
| EvaluationAgent | `agent/impl/EvaluationAgent.java` | evaluate/quality/metrics/benchmark/accuracy | **Hardcoded fake metrics** (96.4% success, 0.03 hallucination). Does not read `EvaluationRepository`. | Yes | None |
| ConversationAgent | removed in Phase 6 | — | Replaced by GeneralQueryAgent | — | — |
| GeneralQueryAgent | `agent/impl/GeneralQueryAgent.java` | always true (fallback, excluded from specialized findHandler) | LLMProvider + structured tool requests (calculator allowlist); MOCK/UNAVAILABLE/REAL honest | Yes | Phase6 + Phase7 tests |
| PlannerAgent | `agent/impl/PlannerAgent.java` | multi-step / execution plan | `LLMProvider.chat`; JSON array or `PLAN_NOT_GENERATED` failure | Yes | None dedicated |
| ApprovalAgent | `agent/impl/ApprovalAgent.java` | approval queue keywords | Returns `NOT_IMPLEMENTED` failure. Real approvals are `ApprovalService`. | Yes | None |
| NotificationAgent | `agent/impl/NotificationAgent.java` | send notification | Returns `NOT_IMPLEMENTED`. Does not send. | Yes | None |
| WorkflowAgent | `agent/impl/WorkflowAgent.java` | schedule workflow | STUB in-memory callback; does not run agents | Yes | None |
| SecurityAgent | `agent/impl/SecurityAgent.java` | security policy | Returns `NOT_IMPLEMENTED`. Does not claim check passed. | Yes | None |
| RagAgent | `agent/impl/RagAgent.java` | document/file/resume/pdf | Calls `document_search` (hardcoded chunks) | Yes | None |
| WhatsAppAgent | — | — | **Does not exist** | — | — |

`OrchestratorAgent` is mentioned in `AgentRegistry.findHandler` (filtered out) but **no such class exists**.

Multi-step planning in `OrchestratorService.isMultiStepGoal()` is a hardcoded English phrase check (`trip`+`plan`/`bangalore`, `research`+`email`, `itinerary`+`budget`). It then calls `PlannerAgent` (compiles; plan quality untested).

---

## 4. Existing tools

Registered via `ToolRegistry` from Spring `Tool` beans.

| Tool name | Class | Risk | Real I/O | Behavior | Tests |
|---|---|---|---|---|---|
| `calculator` | `CalculatorTool` | LOW | Local arithmetic | Supports `+` and `*` only. Subtraction/division stripped. Invalid input returns `0.0`. | `ToolRegistryTest.testCalculatorExecution` (100+50+25 → 175.00) — **cannot run until compile is fixed** |
| `search_web` | `SearchTool` | LOW | None | Hardcoded Bangalore flights/hotels or generic snippet. Not a web API. | `ToolRegistryTest.testSearchExecution` |
| `task_manager` | `TaskTool` | MEDIUM | PostgreSQL | create/list/complete. If `userId` missing, assigns to **first user in the database**. | None |
| `send_email` | `EmailTool` | HIGH | None | Logs and returns `"SENT"`. No SMTP. Always success. | None |
| `calendar` | `CalendarTool` | MEDIUM | None | Returns two hardcoded meetings. Ignores create/update/cancel. | None |
| `github_inspector` | `GitHubTool` | LOW | None | Hardcoded repo stats. Does not call GitHub. | None |
| `document_search` | `DocumentSearchTool` | LOW | Injects repositories then **ignores them** | Returns hardcoded resume + architecture chunks. | None |
| `memory_save` | `MemorySaveTool` | LOW | PostgreSQL | Saves LONG_TERM. Falls back to first user if `userId` missing. | None |
| `memory_search` | `MemorySearchTool` | LOW | PostgreSQL | Lists memories; if no userId, `findAll()`. | None |
| `memory_delete` | `MemoryDeleteTool` | MEDIUM | PostgreSQL | Keyword delete. Compiles. | None |

`ToolRegistry.executeTool` documents timeout/retry but:

- timeout is checked **after** the call completes
- retry sleeps on the calling thread
- Vapi path often calls `tool.execute(args)` **directly**, bypassing `ToolRegistry` validation/retry

`ToolController.POST /api/v1/tools/{name}/execute` executes any registered tool with no authentication (see Security).

---

## 5. Existing APIs

| Method | Path | Auth in SecurityConfig | Notes |
|---|---|---|---|
| POST | `/api/v1/auth/register` | PUBLIC | Tested in `SecurityApiTest` |
| POST | `/api/v1/auth/login` | PUBLIC | Valid → JWT; invalid → 401; missing → 400 |
| POST | `/api/v1/auth/refresh` | PUBLIC | |
| GET | `/api/v1/system/status` | AUTHENTICATED | JWT required |
| POST | `/api/v1/conversations` | AUTHENTICATED | Identity from security context |
| GET | `/api/v1/conversations/{id}` | AUTHENTICATED | 401 if missing JWT; 404 if another user's conversation |
| POST | `/api/v1/agent/execute` | AUTHENTICATED | Conversation must belong to caller |
| GET | `/api/v1/agent/catalog` | AUTHENTICATED | |
| GET | `/api/v1/tools` | AUTHENTICATED | |
| POST | `/api/v1/tools/{name}/execute` | AUTHENTICATED | `userId` overwritten from JWT |
| GET/POST | `/api/v1/approvals/**` | AUTHENTICATED | No first-user fallback |
| GET/POST | `/api/v1/tasks/**` | AUTHENTICATED | |
| GET/POST | `/api/v1/memory/**` | AUTHENTICATED | |
| GET | `/api/v1/metrics` | AUTHENTICATED | Mix of DB averages and **hardcoded** 0.965 / 0.024 remains |
| GET | `/api/executions/**` | AUTHENTICATED | Scoped to current user |
| POST | `/api/webhooks/vapi`, `/api/v1/webhooks/vapi` | VAPI (`x-vapi-secret`) | Fail-closed. Stable DB idempotency. |
| GET | `/api/webhooks/vapi/health` | PUBLIC | Static UP; no secrets in body |
| GET | `/actuator/health`, `/actuator/info` | PUBLIC | |
| GET | `/swagger-ui/**`, `/api-docs/**` | PUBLIC | Still publicly reachable |
| WS | `/ws/**` | Handshake public; origins restricted | Allowed origins from `FRONTEND_URL` / localhost. No `*`. |

There is **no** Action REST API, Booking API, WhatsApp API, Payment API, Connected Accounts API, Workflow API, or Provider configuration API.

---

## 6. Vapi integration status

| Component | Exists | Works | Evidence |
|---|---|---|---|
| `VapiWebhookController` | Yes | Cannot verify | Compiles; depends on broken `VapiWebhookService` |
| `VapiWebhookService` | Yes | PARTIAL | Compiles. Secret verification fail-closed via `VapiWebhookSecurityService`. Idempotency key is stable (`message.id` or `callId`+`toolCall.id` or `callId`+type+status/timestamp) and stored in `webhook_events` with unique constraint. Duplicate deliveries return the stored result. |
| `VapiToolService` | Yes | Partial design | Routes `execute_agent_action` / `voiceos_orchestrator` to Orchestrator. Direct tool execution. Agent-name matching via `contains`. High-risk tools can be skipped with `confirmed=true` from the LLM. |
| `VapiEventProcessor` | Yes | Partial | Transcript/status/EOR persist only if conversation already exists. End-of-call evaluation always `taskSuccess=true`, `hallucinationScore=0.01`. |
| `VapiVoiceProvider` | Yes | Stubbed | Session URL hardcoded `https://api.vapi.ai`. Public key falls back to `vapi_public_token_demo`. Terminate is a comment. |
| `StubVoiceProvider` | Yes | Dev stub | Used when `VOICE_PROVIDER=stub` |
| `VoiceConfiguration.toVapiAssistantConfig` | Yes | Unverified | Returns OpenAI `gpt-4o` + 11labs + Deepgram nova-2. Not multilingual auto-detect. |
| Frontend `@vapi-ai/web` | Yes | Requires env | `ConsolePage` starts call with `VITE_VAPI_PUBLIC_KEY` + `VITE_VAPI_ASSISTANT_ID`. No Test Agent flow. |
| `patch_vapi.py` | Yes | **Unsafe** | Commits a live-looking Vapi Bearer token and sets ngrok webhook URL + secret `ganeshmaheshwaram`. |

**Vapi does not access the database directly** (good). Vapi **does** reach tools and orchestrator without PolicyEngine (bad).

Webhook verification is a shared-secret header equality check, not HMAC signature verification.

---

## 7. Frontend status

| Page | File | Backend-backed | Status |
|---|---|---|---|
| Vapi Voice Console | `ConsolePage.jsx` | Partial | Text path POSTs `/api/v1/agent/execute`. Voice path uses Vapi SDK. Timeline seeds fake “Console Initialized” and appends WebSocket events as `RUNNING` without SUCCESS/FAILED/WAITING distinction. |
| Multi-Agent Fleet | `AgentsPage.jsx` | **No** | Hardcoded 8 cards. No live status. **No Test Agent buttons.** Missing Conversation, Support, Evaluation, Planner, etc. |
| Human Approvals | `ApprovalsPage.jsx` | Partial | Fetches `/api/v1/approvals`. Auth header is `'Bearer placeholder-if-needed'`. Authenticate/Pay send `mock-auth-token` / `mock-txn-id`. |
| Tasks & Memory | `TasksPage.jsx` | **No** | Placeholder paragraph. |
| RAG Knowledge | `RagPage.jsx` | **No** | Placeholder paragraph. |
| Quality & Metrics | `MetricsPage.jsx` | **No** | Placeholder paragraph. |
| Connected Accounts | `ConnectedAccountsPage.jsx` | **No** | Hardcoded Google/WhatsApp/Calendar/Travel rows. Connect/Disconnect toggles local React state only. Claims WhatsApp “Connected” with fake phone. |
| Sidebar health | `Sidebar.jsx` | **No** | Always shows “Vapi Webhook: ACTIVE” (green). |

Frontend package is a Vite React template (`voiceos-frontend/README.md` is the default Vite README). There is no frontend test suite.

---

## 8. Database status

**Engine:** PostgreSQL 16 via Docker (`localhost:5433`). Flyway `V1__init_schema.sql` only. Hibernate `ddl-auto: validate`.

**Existing tables:** `users`, `conversations`, `messages`, `agent_executions`, `tool_executions`, `agent_plans`, `tasks`, `memory_entries`, `documents`, `document_chunks`, `approvals`, `webhook_events`, `evaluations`.

**Required models vs schema:**

| Required | Present | Notes |
|---|---|---|
| User | Yes | password hashed (BCrypt). Role enum exists, unused for authorization. |
| UserPreference | **No** | |
| Conversation | Yes | `user_id` NOT NULL. Orchestrator can create a Conversation **without a user** → runtime failure if that path is hit. |
| Action | **No** | In-memory `Action` POJO only |
| ActionStep | **No** | |
| AgentExecution | Yes | |
| ToolExecution | Yes | Orchestrator does not consistently persist tool rows |
| Workflow | **No** | In-memory `WorkflowService` map |
| Booking | **No** | |
| Notification | **No** | |
| ConnectedAccount | **No** | |
| Memory | Yes (`memory_entries`) | No secret-type filtering |
| AuditEvent | Partial (`webhook_events`) | Not a general audit log |
| ProviderConfiguration | **No** | |

Redis is configured in `application.yml` and docker-compose. **No Java code uses RedisTemplate, caching, or Redis session storage.**  
Qdrant is configured. **No Java code writes or queries Qdrant.** `DocumentSearchTool` returns hardcoded text.

---

## 9. Authentication status

| Capability | Status | Evidence |
|---|---|---|
| User register/login/refresh | Implemented and tested | `AuthService` + `AuthController` + JWT HS256. `SecurityApiTest` covers valid/invalid/missing login. |
| Password hashing | Implemented | BCrypt strength 12. Passwords are not logged. |
| JWT filter | Implemented and tested | `JwtAuthenticationFilter`. Expired and malformed tokens → 401. |
| JWT secret | Fail-closed | `JWT_SECRET` required at startup. No YAML default. Known placeholders rejected. |
| Frontend login | Minimal compatibility | Header login/register stores JWT in `localStorage`. Console/approvals send `Authorization: Bearer`. 401 handled. Not a dashboard redesign. |
| Unauthenticated API access | Protected | Dashboard `/api/v1/**` routes require authentication. First-user fallback removed. Unauthenticated tool execute → 401. |
| Authentication handoff (provider OAuth/MFA) | STUB/MOCK only | Mock tokens are labeled `MOCK` and accepted only when `MOCK_EXTERNAL_APIS=true`. They are not treated as real OAuth. |

---

## 10. Authorization status

| Capability | Status | Evidence |
|---|---|---|
| Identity source | Security context | `AuthenticatedUserService.requireUser()`. Request-body `userId` is overwritten on tool execute. |
| Resource ownership | Partial, tested | User A cannot read user B's conversation (`SecurityApiTest`). Executions/tasks/memory/approvals scoped to authenticated user. |
| RBAC | **Not enforced** | `User.role` → `ROLE_USER` in UserDetails. No `@PreAuthorize` on admin operations. |
| Tool permissions | Auth required; policy not wired | `POST /api/v1/tools/{name}/execute` requires JWT. PolicyEngine is still not the execution gate. |
| Provider permissions | **Not implemented** | |
| Action policies | Keyword stub | `PolicyEngine` not wired to Vapi/Orchestrator |
| Vapi webhook | Fail-closed | Blank secret rejected unless `VAPI_WEBHOOK_ALLOW_INSECURE_LOCAL=true` is explicit. Invalid `x-vapi-secret` → 401. |
| Idempotency | Database-backed | Stable key from Vapi `message.id` / `toolCall.id` / `call.id`+type. Unique constraint on `webhook_events.idempotency_key`. Duplicate returns stored result. |
| Fail closed | Webhook + JWT + identity | Blank JWT secret refuses startup. Missing user → 401, not first DB user. Mock tokens are not real authorization. |

---

## 11. Provider integrations

### Mock / fake providers (what actually exists)

| Name | Class | Realistic failure modes | Official API | Notes |
|---|---|---|---|---|
| MockLLMProvider | `ai/mock/MockLLMProvider.java` | No | N/A | Keyword canned text. Used when `voiceos.ai.mock=true` (dev default). |
| TravelProvider | `provider/travel/TravelProvider.java` | Partial (booking without payment pauses) | No | Always mock. Search returns one string. Claims “Official Travel Provider Mock”. |
| WhatsAppProvider | `provider/whatsapp/WhatsAppProvider.java` | **No — always success** | No | Comment says “Mocking the official WhatsApp Business API”. Description falsely says “Official WhatsApp Business API integration.” |
| SearchTool / CalendarTool / EmailTool / GitHubTool / DocumentSearchTool | tools | Always success | No | Hardcoded results |
| StubVoiceProvider | `voice/stub` | N/A | No | Dev stub |

**Named mocks required by the product spec that do not exist as classes:**

`MockTravelProvider`, `MockEmailProvider`, `MockWhatsAppProvider`, `MockCalendarProvider`, `MockPaymentProvider`, `MockAuthenticationProvider`, `MockResearchProvider`, `MockTaskProvider`, `MockMemoryProvider`.

None of the mocks simulate timeout, invalid data, already-completed, or provider-unavailable as first-class modes.

### Real providers

| Provider | Adapter | Config present | Used | Status |
|---|---|---|---|---|
| Vapi | `VapiVoiceProvider` + webhook | `VAPI_*` env | Partial | Compile broken; no live call verified in this audit |
| Gemini | `GeminiLLMProvider` | `GEMINI_*` | Only if mock=false and ChatModel beans exist | Untested |
| Groq | `GroqLLMProvider` | `GROQ_*` | Same | Untested |
| SMTP | none | `SMTP_*` in yml | **Unused** | EmailTool never reads SMTP |
| Google Calendar | none | `.env.example` only | **Unused** | |
| GitHub | none | `GITHUB_TOKEN` unused | GitHubTool is fake | |
| WhatsApp Cloud API | none | none | WhatsAppProvider is fake | |
| Travel official APIs | none | none | | |
| Payment | none | none | | |
| Qdrant | Spring AI starter on classpath | host/port | **No application code** | |
| Redis | starter on classpath | host/port | **No application code** | |

---

## 12. Environment variables

From `.env.example` and `application.yml`.

| Variable | Classification | Notes |
|---|---|---|
| `DATABASE_URL`, `DATABASE_USERNAME`, `DATABASE_PASSWORD` | REQUIRED for real Postgres | Default password in `application.yml` is hardcoded `Ganesh23` if env missing |
| `POSTGRES_*` | REQUIRED for docker-compose | |
| `REDIS_HOST`, `REDIS_PORT`, `REDIS_PASSWORD` | OPTIONAL today (unused in code) | |
| `QDRANT_*` | OPTIONAL today (unused in code) | |
| `JWT_SECRET` | REQUIRED for production | Weak default in yml |
| `AI_PROVIDER` | OPTIONAL | gemini \| groq |
| `GEMINI_API_KEY` | REAL MODE | |
| `GROQ_API_KEY` | REAL MODE | |
| `MOCK_EXTERNAL_APIS` / `voiceos.ai.mock` | DEMO | Dev profile forces `mock: true` |
| `VOICE_PROVIDER` | REQUIRED conceptually | Default `vapi` |
| `VAPI_API_KEY` | REAL MODE | Also hardcoded in `patch_vapi.py` (must rotate) |
| `VAPI_ASSISTANT_ID` | REAL MODE | |
| `VAPI_WEBHOOK_SECRET` | REQUIRED if webhook exposed | Fail-open if blank |
| `VAPI_PUBLIC_KEY` | REQUIRED for browser voice | Also needed as `VITE_VAPI_PUBLIC_KEY` (not documented in `.env.example`) |
| `FRONTEND_URL` | OPTIONAL | CORS |
| `SMTP_*` | REAL MODE / unused | |
| `GOOGLE_CALENDAR_*` | REAL MODE / unused | |
| `GITHUB_TOKEN` | REAL MODE / unused | |
| `WEBHOOK_LIVEKIT_SECRET` | **Dead** | LiveKit not used |
| `RATE_LIMIT_*` | Documented, **not implemented** | |

`.env.example` does **not** distinguish REQUIRED / OPTIONAL / DEMO ONLY / REAL MODE clearly.

---

## 13. Tests

| Test class | What it covers | Compiles with current main | Runnable now | Phase 0B result |
|---|---|---|---|---|
| `ToolRegistryTest` | discovery, calculator +/*, invalid input, search_web, unknown tool, risk | Yes | Yes | 8 tests passed |
| `AgentRegistryTest` | 2-agent registry, travel routing, conversation fallback | Yes | Yes | 3 tests passed |
| `VapiWebhookServiceTest` | secret validation, calculator tool-call, assistant-request, empty payload, unknown event, missing function name | Yes | Yes | 6 tests passed |
| `VoiceOsApplicationTests` | Spring context smoke (H2, mock LLM, Redis/Qdrant autoconfig excluded) | Yes | Yes | 1 test passed |

**Suite total (2026-09-17 `mvnw.cmd test` and `mvnw.cmd package`):** Tests run: 18, Failures: 0, Errors: 0, Skipped: 0.

**Missing entirely:** integration tests, contract tests, workflow tests, security tests, Vapi webhook replay/idempotency tests, multilingual entity tests, e2e tests, frontend tests, CI.

Hallucinated quality numbers are stored by Orchestrator (`hallucinationScore=0.02`, `toolSuccessRate=1.0`) and EvaluationAgent (96.4%). These are not measurements.

---

## 14. Known bugs (evidence-based)

1. **Backend compile was broken (Phase 0); fixed in Phase 0B.** Remaining product bugs below still apply.
2. **Orchestrator can persist Conversation without `user`** while schema requires `user_id NOT NULL`.
3. **Idempotency is non-functional** — key includes `System.currentTimeMillis()`.
4. **Webhook fail-open** when secret unset.
5. **FinanceAgent ignores the user’s numbers**.
6. **EmailAgent invents recipient and subject**.
7. **EmailTool reports SENT without sending**.
8. **WhatsAppProvider always reports delivered**.
9. **EvaluationAgent and MetricsController invent success rates**.
10. **ActionEngine not connected** to Vapi or Orchestrator; READY state never auto-executes (`processAction` pauses on READY).
11. **PolicyEngine `evaluatePolicy` returning null** is fine for READY, but ActionEngine never transitions READY → EXECUTING.
12. **TravelAgent does not use TravelProvider**.
13. **VapiToolService can bypass approval** with `confirmed=true`.
14. **ApprovalService.processApproval** calls `ToolResult.success(Map, String)` with wrong arity — compile error.
15. **Frontend conversation create** POSTs with no JSON body/auth; will fail if no user exists.
16. **Sidebar always shows webhook ACTIVE**.
17. **`patch_vapi.py` committed secret**.
18. **Hardcoded DB password** `Ganesh23` in `application.yml`.
19. **LiveKit leftovers** in pom property, VoiceProvider javadoc, `.env.example`.
20. **No TaskScheduler bean configuration** found besides `@EnableScheduling`; `WorkflowService` may fail context load even after agent compile fixes (not verified because compile fails earlier).

---

## 15. Missing functionality (required by product spec, absent or stub)

- Deterministic Action state machine with persisted Actions and legal transitions
- AgentOrchestrator that builds multi-agent plans with structured data passing (PlannerAgent compiles; not a full orchestrator)
- Domain provider interfaces with mock **and** real implementations for email, calendar, payment, auth, research, tasks, memory
- WhatsAppAgent + official WhatsApp Cloud API adapter
- NotificationEngine (email/WhatsApp/Vapi/frontend) shared across agents
- WorkflowEngine with pause/resume for auth and payment
- Structured domain events (`ActionCreated`, `PaymentCompleted`, …)
- Real idempotency on unsafe operations
- Retry policy that forbids blind retry of booking/payment/email/WhatsApp
- Voice confidence / “ask don’t guess” entity confirmation beyond a system-prompt sentence
- Language switching during a call (LanguageService exists; not wired to Vapi events or TTS)
- Memory secret filtering (passwords/OTP/CVV)
- OFFICIAL_ONLY travel enforcement
- Booking verification / tickets
- Demo mode vs Real mode as a single provider switch (today: LLM mock flag only)
- Frontend operations center with real agent/tool/action/timeline/health data
- Test Agent buttons
- Automated tests and CI
- Documentation set listed in Phase 43 (most files missing)

---

## 16. What is duplicated

- Two orchestration stacks: `OrchestratorService` vs `ActionEngine` (not integrated).
- Two approval paths: `AgentResult.approvalRequired` vs `PolicyEngine` vs Vapi `confirmed` flag.
- Two Vapi secret properties: `voiceos.voice.vapi.webhook-secret` and `voiceos.webhooks.vapi-secret`.
- Two conversation ID schemes: DB UUID vs `UUID.nameUUIDFromBytes(call.id)`.
- README vs code: README marks phases 1–15 complete; RAG/Redis unused; compile now succeeds after Phase 0B.
- `docs/current-state.md` (previous version) claimed ActionEngine/WhatsAppProvider missing; both now exist as stubs. That document was stale.

---

## 17. What needs refactoring

1. Fix compilation and make the module build. **DONE (Phase 0B).**
2. Collapse Path A and Path B into one Action-centric pipeline used by Vapi and HTTP.
3. Move business I/O out of tools/agents into provider interfaces.
4. Replace keyword `canHandle` with explicit intent + entity contracts (keep keywords only as a fast prefilter).
5. Restore authentication on dashboard APIs; remove first-user fallback. **DONE (Phase 1).**
6. Replace hardcoded “success” tools with mock providers that can fail.
7. Stop writing fake evaluation scores.
8. Remove LiveKit leftovers without introducing LiveKit.
9. Make frontend pages consume catalog/status/timeline APIs.
10. Split secrets out of `application.yml` defaults. **DONE for JWT, Vapi, database password (Phase 1).** Previously committed Vapi credentials must still be rotated outside git.

---

## 18. Security issues

| ID | Severity | Issue |
|---|---|---|
| S1 | Critical | A Vapi API key was previously committed in `patch_vapi.py`. The file now reads `VAPI_API_KEY` from the environment. **Rotate the exposed key in the Vapi dashboard.** Do not commit replacements. **Still required outside the repo.** |
| S2 | Critical | A webhook secret was previously committed in `patch_vapi.py` and `vapi_patch.json`. Those files now use env placeholders. **Rotate the webhook secret.** **Still required outside the repo.** |
| S3 | High | **Mitigated (Phase 1).** Dashboard APIs require JWT. First-user fallback removed. |
| S4 | High | **Mitigated (Phase 1).** `POST /api/v1/tools/{name}/execute` requires authentication. PolicyEngine still not wired. |
| S5 | High | **Mitigated (Phase 1).** Vapi webhook fail-closed. Invalid/missing secret → 401. |
| S6 | High | **Mitigated (Phase 1).** JWT has no YAML default. Startup fails if `JWT_SECRET` is missing, short, or a known placeholder. |
| S7 | High | **Mitigated (Phase 1).** Dummy tokens are MOCK/STUB, not real authorization. MOCK rejected when mock mode is off. |
| S8 | Medium | **Mitigated (Phase 1).** WebSocket origins use the configured CORS list. Wildcard `*` is rejected. |
| S9 | Medium | EmailAgent contains a personal Gmail address. **Unchanged.** |
| S10 | Medium | Role exists but is never checked. Rate-limit properties exist but no filter. **Unchanged.** |
| S11 | Medium | **Mitigated (Phase 1).** Approval authenticate no longer stores `oauth_token` from dummy tokens. |
| S12 | Low | LiveKit secret env leftover. **Unchanged; LiveKit was not introduced.** |
| S13 | Low | Evaluation/metrics endpoints invent compliance numbers (integrity issue). **Unchanged.** |

Principle “FAIL CLOSED” is **not** implemented.

---

## 19. Technical debt

- README “Resume Highlights” and phase checkmarks claim production completeness.
- `pom.xml` still has unused `livekit.version` and unused MapStruct property.
- Spring AI milestone repositories required.
- Java 26 installed locally vs project Java 21 (`start-all.ps1` sets JAVA_HOME to jdk-26.0.2).
- `DocumentSearchTool` injects unused repositories.
- `System.out.println` in WorkflowAgent.
- `OrchestratorService` uses raw `List.class` Jackson parse.
- No structured correlation IDs on logs (conversationId only in some paths).
- Frontend Vite README never replaced.
- No frontend Dockerfile despite compose `frontend` service.

---

## 20. What cannot currently be verified

Because compile failed, the following were **not** executed in this audit:

- `mvn test`
- Spring context load
- Flyway migration against Docker Postgres
- Redis/Qdrant connectivity
- Live Vapi call
- Frontend `npm run build` / `npm run lint` (not run in this phase; frontend is JS and likely builds, but was not used as completion evidence)
- End-to-end demo workflow

Frontend was inspected as source, not as a running UI.

---

## 21. Exact completion percentage by feature

**Rule:** Completion % = 100 only if the feature is implemented **and** covered by an automated test that can currently pass. Otherwise 0.

`mvn compile` failed with 20 errors on 2026-09-17, so **no automated test can currently pass through the project build.**

| Feature | Status | Implemented | Tested | Verified | Completion % |
|---|---|---|---|---|---|
| Backend starts | PARTIALLY IMPLEMENTED | Context loads in tests | VoiceOsApplicationTests passed | Not a live server run | 0 |
| Frontend starts | PARTIALLY IMPLEMENTED | Vite production build passed | `npm run build` | Dev server not run | 0 |
| Database schema | PARTIALLY IMPLEMENTED | V1 Flyway | No | Not run | 0 |
| Vapi webhook | PARTIALLY IMPLEMENTED | Controller + service compile | Unit tests pass | Structural only | 0 |
| Vapi tool call | PARTIALLY IMPLEMENTED | `VapiToolService` calculator path | Unit test pass | Calculator only | 0 |
| Voice interaction | BLOCKED BY EXTERNAL CREDENTIAL | SDK + provider stub | No live call | No | 0 |
| Transcript capture | PARTIALLY IMPLEMENTED | Event processor + frontend listener | No | No | 0 |
| Intent routing | PARTIALLY IMPLEMENTED | Keyword registry | `AgentRegistryTest` passed | Unit only | 0 |
| Critical entity preservation | NOT IMPLEMENTED | Prompt text only | No | No | 0 |
| Missing-info clarification | NOT IMPLEMENTED | Prompt text only | No | No | 0 |
| Multilingual conversation | PARTIALLY IMPLEMENTED | Unicode heuristic detector; not wired | No | No | 0 |
| Agent routing | PARTIALLY IMPLEMENTED | Keyword | Test passed | Unit only | 0 |
| Tool routing | PARTIALLY IMPLEMENTED | Registry | Test passed | Unit only | 0 |
| Mock providers (spec) | PARTIALLY IMPLEMENTED | Ad-hoc hardcoded tools | No failure-mode tests | No | 0 |
| Real providers | NOT IMPLEMENTED / BLOCKED BY EXTERNAL CREDENTIAL | Gemini/Groq adapters exist, unused in default mock | No | No | 0 |
| Travel workflow | PARTIALLY IMPLEMENTED | Keyword agent + fake search | Routing test only | No | 0 |
| Email | PARTIALLY IMPLEMENTED | Draft+fake send | No | No | 0 |
| WhatsApp official | NOT IMPLEMENTED | Fake provider; no agent | No | No | 0 |
| Calendar | PARTIALLY IMPLEMENTED | Hardcoded schedule | No | No | 0 |
| Tasks | PARTIALLY IMPLEMENTED | JPA tool + REST | No | No | 0 |
| Research | PARTIALLY IMPLEMENTED | Fake search + LLM | No | No | 0 |
| Memory | PARTIALLY IMPLEMENTED | JPA tools; delete compiles | No dedicated test | No | 0 |
| Finance calculations | PARTIALLY IMPLEMENTED | Calculator tool real; FinanceAgent fake | Calculator tests pass | Unit only | 0 |
| Backend compile | IMPLEMENTED / TESTED | Maven compile | Yes | Yes | 100 |
| Existing unit tests | IMPLEMENTED / TESTED | 18 tests | Yes | Yes | 100 |
| Authentication handoff | PARTIALLY IMPLEMENTED | Status enum + dummy token | No | No | 0 |
| Payment handoff | PARTIALLY IMPLEMENTED | Status enum + dummy txn | No | No | 0 |
| Verification | PARTIALLY IMPLEMENTED | Non-empty string check | No | No | 0 |
| Idempotency | NOT IMPLEMENTED | Column exists; key is unique per millisecond | No | No | 0 |
| Retry policy | PARTIALLY IMPLEMENTED | Blind retry in ToolRegistry including unsafe tools | No | No | 0 |
| Error recovery | PARTIALLY IMPLEMENTED | GlobalExceptionHandler exists | No | No | 0 |
| Audit logging | PARTIALLY IMPLEMENTED | webhook_events; broken idempotency | No | No | 0 |
| Security checks | NOT IMPLEMENTED | Config present; APIs open | No security tests | No | 0 |
| Frontend reflects backend | PARTIALLY IMPLEMENTED | Console/Approvals only | No | No | 0 |
| Execution timeline | PARTIALLY IMPLEMENTED | WebSocket + incomplete UI states | No | No | 0 |
| Evaluation | NOT IMPLEMENTED | Fake numbers | No | No | 0 |
| Demo mode | PARTIALLY IMPLEMENTED | LLM mock flag only | No | No | 0 |
| Real mode | PARTIALLY IMPLEMENTED | Config switch; not verified | No | No | 0 |
| Documentation | PARTIALLY IMPLEMENTED | 4 docs; most required files missing | n/a | n/a | 0 |
| CI/CD | NOT IMPLEMENTED | None | n/a | n/a | 0 |
| ActionEngine | PARTIALLY IMPLEMENTED | In-memory; unused; READY never executes | No | No | 0 |
| PolicyEngine | PARTIALLY IMPLEMENTED | Keyword stub | No | No | 0 |
| NotificationEngine | NOT IMPLEMENTED | NotificationAgent returns NOT_IMPLEMENTED | No | No | 0 |
| WorkflowEngine | NOT IMPLEMENTED | In-memory scheduler STUB compiles | No | No | 0 |
| Observability / correlation IDs | PARTIALLY IMPLEMENTED | Some logs; no required ID set | No | No | 0 |

**Product-level completion under Definition of Done: 0%.**  
**Code-presence estimate (classes exist for a portion of the vision): not used as completion.**

---

## 22. Recommended implementation order

Do not implement features on a non-compiling tree. **Phase 0B unblocked the build.** Remaining order:

1. **Unblock build** — **DONE** (Phase 0B).
2. **Secret hygiene** — rotate previously committed Vapi key; JWT default still in yml.
3. **Fail closed** — require webhook secret; restore JWT on APIs; remove first-user fallback; close arbitrary tool execution.
4. **Unify execution path** — Vapi webhook → validate → idempotency → ActionEngine → Policy → Agent → Tool → Provider → Verify.
5. **Honest mock providers** — labeled `[MOCK]`, with success/failure/timeout/unavailable; never claim official delivery.
6. **Persist Actions** + legal state transitions + correlation IDs.
7. **Travel demo workflow** on mocks (search → select → auth pause → payment pause → verify → notify).
8. **Email / WhatsApp / Calendar** via NotificationEngine + mock/real adapters.
9. **Frontend** — live catalog, Test Agent buttons hitting backend test workflows, real timeline states.
10. **Tests** — unit, workflow, security, Vapi contract, e2e demo.
11. **CI** without paid credentials (mocks only).
12. **Real providers one at a time** after demo e2e is green: Vapi, email, calendar, WhatsApp, research, travel search, auth, payment sandbox.

---

## 23. Compile error inventory (2026-09-17)

All 20 errors listed below were **fixed in Phase 0B**. `mvnw.cmd clean compile` is BUILD SUCCESS.

| File | Former error | Phase 0B resolution |
|---|---|---|
| `VapiWebhookService.java` | `VoiceConfiguration` not imported | Import added |
| `ApprovalAgent.java` | missing `getTools`/`canHandle`; success arity | Implemented; returns `NOT_IMPLEMENTED` failure |
| `NotificationAgent.java` | same | Implemented; returns `NOT_IMPLEMENTED` failure |
| `PlannerAgent.java` | missing methods; wrong `LLMRequest`; `text()`; arity | Uses `LLMProvider.chat` / `content()` |
| `SecurityAgent.java` | missing methods; success arity | Implemented; returns `NOT_IMPLEMENTED` failure |
| `WorkflowAgent.java` | missing methods; success arity | Implemented; STUB in-memory schedule, no fake workflow completion |
| `ApprovalController.java` | `this.toolRegistry` with no field | Unused constructor param removed |
| `ApprovalService.java` | `ToolResult.success(Map, String)` | Added durationMs |
| `MemoryDeleteTool.java` | `ToolResult.success(Map, String)` | Added durationMs |

---

## 24. WHAT EXISTS / WORKS / PARTIAL / MOCKED / BROKEN / MISSING

| Category | Finding |
|---|---|
| **EXISTS** | Spring Boot modular monolith, Flyway V1, JWT auth classes, 17 agent classes, 10 tools, ActionEngine package, Vapi webhook/controller/DTOs, React dashboard shell, docker-compose for PG/Redis/Qdrant, MockLLMProvider |
| **WORKS** | Maven compile. 18 unit/context tests. Calculator `+`/`*`. AgentRegistry keyword routing. Frontend Vite production build. |
| **PARTIALLY WORKS** | Console text → `/agent/execute` design; Approvals UI fetch design; Vapi browser SDK wiring; Memory/Task JPA tools |
| **MOCKED** | LLM (dev), search, calendar, email send, GitHub, document RAG, travel, WhatsApp, finance totals, evaluation scores, connected accounts, metrics fallbacks |
| **BROKEN** | Idempotency; webhook fail-open; ActionEngine READY deadlock; PolicyEngine unused by Vapi; README claiming completeness |
| **MISSING** | WhatsAppAgent, NotificationEngine, WorkflowEngine, persisted Actions, real official APIs, CI, most docs, Test Agent buttons, e2e tests, fail-closed security |
| **UNSAFE** | Previously committed Vapi credentials (rotate); open APIs; dummy payment/auth success; JWT default still in yml |
| **UNTESTED** | Product workflows, security, Vapi live calls, e2e |

---

## 25. Audit conclusion

VoiceOS is an **existing, incomplete** Spring Boot + React + Vapi project. Phase 0B restored a **compiling, testable baseline**. It is **not** an operational AI employee.

**Next engineering step:** Phase 1 — secret hygiene and fail-closed security. Do not start until explicitly requested.
