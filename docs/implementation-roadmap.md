# VoiceOS Implementation Roadmap

**Created:** 2026-09-17  
**Based on:** `docs/current-state.md` (full repository audit)  
**Constraint:** Do not replace Spring Boot or Vapi. Do not introduce LiveKit or another voice platform. Evolve the existing project.

**Completion rule:** A phase is `DONE` only after compile, tests, and the listed acceptance criteria have actually passed. Status values: `NOT STARTED` | `IN PROGRESS` | `BLOCKED` | `DONE`.

**Current global status:** Phase 0 audit `DONE`. Phase 0B compilation recovery `DONE`. Phase 1 security foundation `DONE`. Phase 2 execution pipeline `DONE`. Phase 3 action steps + audit events `DONE`. Phase 4 provider abstraction + honest mocks `DONE` (`mvnw.cmd test` — 83 tests, 0 failures). Live Vapi `NOT VERIFIED`. Travel/email/WhatsApp/calendar remain MOCK. Product is **not production-ready**.

---

## Phase 0 — Audit (this document's predecessor)

| Field | Content |
|---|---|
| **Objective** | Inspect the existing repository. Produce current-state and this roadmap. Do not implement features. |
| **Files affected** | `docs/current-state.md`, `docs/implementation-roadmap.md`, `docs/reports/2026-09-17-phase0-audit.md` |
| **Dependencies** | None |
| **Implementation** | Read-only inspection + documentation |
| **Tests** | `mvn compile` executed; `BUILD FAILURE` (20 errors). Tests not runnable. |
| **Acceptance criteria** | Audit documents exist; compile errors inventoried; no invented completion percentages |
| **Status** | `DONE` (audit only) |

---

## Phase 0b — Restore a compiling, testable baseline

| Field | Content |
|---|---|
| **Objective** | Make the existing backend compile and run the four existing tests. Do not add features. |
| **Files affected** | `VapiWebhookService.java`, `PlannerAgent.java`, `ApprovalAgent.java`, `NotificationAgent.java`, `SecurityAgent.java`, `WorkflowAgent.java`, `ApprovalController.java`, `ApprovalService.java`, `MemoryDeleteTool.java`; possibly `VapiWebhookServiceTest.java`; possibly a `TaskScheduler` `@Bean` |
| **Dependencies** | Phase 0 |
| **Implementation** | Add missing `Agent` methods; fix `AgentResult`/`ToolResult`/`LLMRequest` call sites; import `VoiceConfiguration`; remove dead `toolRegistry` assignment; fix test constructors |
| **Tests** | `mvnw.cmd compile`; `mvnw.cmd test`; `mvnw.cmd package`; existing `ToolRegistryTest` (8), `AgentRegistryTest` (3), `VapiWebhookServiceTest` (6), `VoiceOsApplicationTests` (1) |
| **Acceptance criteria** | Zero compile errors. Existing tests either pass or fail for behavioral reasons documented in a report — not because the module cannot compile. |
| **Status** | `DONE` — see `docs/reports/2026-09-17-phase-0b-compilation-recovery.md` |

---

## Phase 1 — Secret hygiene and fail-closed security

| Field | Content |
|---|---|
| **Objective** | Remove committed secrets, stop fail-open webhooks, restore authentication on dashboard APIs, close arbitrary tool execution. |
| **Files affected** | `SecurityConfig.java`, JWT/auth services, all controllers' identity resolution, `ToolController.java`, Vapi webhook security/idempotency services, `application.yml`, `.env.example`, frontend token handling, Flyway `V2` |
| **Dependencies** | Phase 0b |
| **Implementation** | JWT from env, fail-closed. Dashboard APIs require authentication. First-user fallback removed. Tool execute requires JWT identity. Vapi `x-vapi-secret` fail-closed. Stable DB idempotency with unique-constraint concurrency. CORS origins explicit. CSRF disabled because stateless Bearer JWT (documented). Mock auth/payment tokens labeled MOCK. |
| **Tests** | 46 tests, 0 failures, 0 skipped: existing 18 plus JWT, webhook security, idempotency keys, SecurityApiTest (authn/authz/tools/webhooks/concurrency), secret hygiene |
| **Acceptance criteria** | No secrets in git-tracked config. Webhook fail-closed. Dashboard APIs require JWT. Tool execute not public. Duplicate Vapi events do not execute twice. |
| **Status** | `DONE` — see `docs/reports/2026-09-17-phase-1-security.md` |

**Phase 1 API behavior changes (intentional, insecure old behavior removed):**

| Endpoint | Old | New | Reason |
|---|---|---|---|
| `/api/v1/conversations`, `/tasks`, `/memory`, `/approvals`, `/agent/**`, `/tools/**`, `/metrics`, `/system/**` | `permitAll` + first-user fallback | 401 without JWT; identity from security context | Identity before action |
| `POST /api/v1/tools/{name}/execute` | Unauthenticated | Authenticated; `userId` taken from JWT | Prevent arbitrary tool execution |
| `POST /api/webhooks/vapi` | Blank secret allowed | Blank secret rejected unless explicit insecure-local flag | Fail closed |
| Approval authenticate/pay dummy tokens | Treated as success | Labeled MOCK/STUB; MOCK rejected when mock mode is off | Dummy ≠ real authorization |

---

## Phase 2 — Target architecture wiring (single execution path)

| Field | Content |
|---|---|
| **Objective** | Connect Vapi → webhook validate → session → ActionEngine → PolicyEngine → Agent → ToolRegistry → Provider → Verification → events → Vapi + frontend. |
| **Files affected** | `VapiWebhookController`, `VapiWebhookService`, `VapiToolService`, `VapiRequestValidator` (new), `VapiResponseBuilder` (new), `OrchestratorService`, `ActionEngine`, `ActionPlanner`, `ActionExecutor`, `ResultVerifier`, `PolicyEngine` |
| **Dependencies** | Phase 0b, Phase 1 |
| **Implementation** | Delete the split between Path A and Path B. `READY` must transition to `EXECUTING` for low-risk actions. Persist Action. Correlation IDs on every request (`correlationId`, `sessionId`, `actionId`). Vapi must not call tools that skip policy. |
| **Tests** | Unit: legal/illegal state transitions. Integration: tool-call payload → Action created → agent selected → structured Vapi result. Duplicate tool-call does not double-execute. |
| **Acceptance criteria** | One pipeline. Impossible ActionStatus transitions rejected. Vapi responses structured JSON with toolCallId. |
| **Status** | `DONE` — see `docs/reports/2026-09-17-phase-2-execution-pipeline.md` |

**Phase 2 delivered:**

- Canonical path: Vapi/HTTP → ActionEngine → PolicyEngine → AgentRegistry → Agent → ToolRegistry → Tool → Provider → Verification
- Persisted `actions` + `voice_sessions` (Flyway V3)
- JWT bind of Vapi `callId` (no email-as-auth, no first-user)
- Calculator REAL E2E locally: `125 * 24` → `3000.00`
- Policy DENY prevents tool execution; provider failure → FAILED not COMPLETED
- Webhook idempotency reused; duplicate tool-call does not re-execute calculator
- Live Vapi: NOT VERIFIED

---

## Phase 3 — Action steps and audit events

| Field | Content |
|---|---|
| **Objective** | Durable ActionStep sequence + append-only AuditEvent evidence so an action timeline can be reconstructed without logs. |
| **Files affected** | Flyway `V4`; `ActionStep` / `AuditEvent` entities + stores; `ActionTraceService`; `ActionEngine` observer hooks; `ActionController` timeline/steps/events; Console timeline; tests |
| **Dependencies** | Phase 2 |
| **Implementation** | Action 1→N ActionStep with unique `(action_id, sequence_no)`. Explicit step transitions. Audit events keyed by `actionId:eventType:stepId`. Secrets redacted. Owner-scoped timeline DTO. Audit observes; it does not execute. |
| **Tests** | 67 tests, 0 failures: step transitions, redaction, calculator timeline, policy deny, provider failure, unbound identity, duplicate Vapi webhook, user isolation on timeline/steps/events |
| **Acceptance criteria** | Success/failure/deny timelines are honest. Duplicate Vapi event does not duplicate the execution timeline. Non-owners get 404. No secrets in step/audit metadata. |
| **Status** | `DONE` — see `docs/reports/2026-09-17-phase-3-action-steps-audit-events.md`. Action persistence and webhook idempotency landed earlier in Phase 1/2. Do **not** begin Travel/Email/WhatsApp here. |

**Phase 3 delivered:**

- `action_steps` + `audit_events` (Flyway V4)
- ActionEngine emits step transitions and audit events on the existing pipeline
- `GET /api/v1/actions/{id}/timeline|steps|events` owner-scoped
- Console loads real backend timeline data
- Calculator REAL E2E still `125 * 24` → `3000.00` with a complete timeline
- Live Vapi: NOT VERIFIED. No new providers. No LiveKit.

---

## Phase 4 — Provider abstraction and honest mocks

| Field | Content |
|---|---|
| **Objective** | Honest Tool → Provider contract. REAL vs MOCK is explicit. No silent mock fallback. No real external integrations. |
| **Files affected** | `provider/**`; Calculator/Email/Calendar/ProbeFail tools; `ProviderRegistry`; `ActionExecutor`; `VoiceOsProperties`; Console provider/mode; tests |
| **Dependencies** | Phase 2, Phase 3 |
| **Implementation** | `ProviderResult` SUCCESS/FAILURE/UNAVAILABLE/TIMEOUT. Registry by name and capability. Mock travel/WhatsApp/email/calendar/payment labeled MOCK. Calculator remains REAL local arithmetic. HTTP cannot select a provider implementation. |
| **Tests** | 83 tests, 0 failures: previous 67 plus provider honesty, timeout mapping, catalog auth, ignored client provider name |
| **Acceptance criteria** | Calculator `125 * 24` = `3000.00` REAL. Mocks never claim real booking/delivery. REAL without implementation is UNAVAILABLE. |
| **Status** | `DONE` — see `docs/reports/2026-09-17-phase-4-provider-abstraction.md`. Do **not** begin real Flight/Hotel/Email/WhatsApp/Calendar/Payment APIs. |

**Phase 4 delivered:**

- Shared provider contract + registry
- Honest MOCK travel / WhatsApp / email / calendar / payment
- REAL local calculator preserved
- FailingProbeProvider remains MOCK failure
- GET `/api/v1/providers` catalog (authenticated, no secrets, no execute)
- Live Vapi: NOT VERIFIED. No LiveKit.

---

## Phase 5 — Agent contracts (11 required agents)

| Field | Content |
|---|---|
| **Objective** | Each required agent has responsibility, input/output contract, tool permissions, validation, error handling, execution state, observability, tests. No visual-only agents. |
| **Files affected** | `agent/impl/*`, `agent/core/*`, tests per agent |
| **Dependencies** | Phase 4 |
| **Implementation** | Travel, Task, Email, Finance, Research, Calendar, Memory, Developer, Support, Evaluation, Conversation. Fix or remove compile-broken stubs (Approval/Notification/Workflow/Security) by folding into engines, not fake agents. WhatsApp sending belongs in NotificationEngine, not a decorative agent, unless a real WhatsAppAgent with a contract is required — then implement for real. |
| **Tests** | One test class per agent: happy path, missing entities, unauthorized tool, provider failure. |
| **Acceptance criteria** | Agents do not call third-party APIs directly. FinanceAgent does not invent totals. EvaluationAgent does not invent scores. EmailAgent does not invent recipients. |
| **Status** | `DONE` — see `docs/reports/2026-09-17-phase-5-agent-contracts.md`. Do **not** begin LLM integration or real Flight/Hotel/Email/WhatsApp/Calendar/Payment APIs. |

**Phase 5 delivered:**

- Structured `AgentRequest` / `AgentResult` / `AgentOutcome` / `AgentCapability`
- Deterministic `AgentRegistry` + `RegistryAgentSelector` (priority then name; duplicate names fail fast)
- ActionEngine maps agent outcomes onto ActionStatus; PolicyEngine still gates execution
- Honest Finance/Travel/Email/Evaluation/Workflow/stub agents
- Calculator path unchanged: UtilityAgent → CalculatorTool → RealLocalCalculatorProvider = `3000.00` REAL
- Live Vapi: NOT VERIFIED. No LiveKit. No new LLM providers.

---

## Phase 6 — Voice accuracy and multilingual

| Field | Content |
|---|---|
| **Objective** | Preserve names, dates, locations, amounts, emails, phones. Ask when uncertain. Support listed languages and mid-conversation language switch without restarting. |
| **Files affected** | `VoiceConfiguration`, `LanguageService`, `LanguageDetectionService`, `LanguagePreferenceService`, Vapi assistant config, entity extraction service (new), voice test dataset |
| **Dependencies** | Phase 2 |
| **Implementation** | Structured entity schema on tool calls. Confirmation utterances for low confidence. Do not silently remap Friday→Saturday or Hyderabad→Ahmedabad. Wire language preference to Vapi transcriber/TTS. Preserve entities across language switch. |
| **Tests** | Dataset in English, Hindi, Telugu, Hinglish, mixed; ambiguous dates/numbers. Tests assert entities preserved or clarification requested. |
| **Acceptance criteria** | Uncertain critical fields trigger ASK. Language switch updates date without resetting conversation. |
| **Status** | `NOT STARTED` |

---

## Phase 7 — Travel system (demo E2E)

| Field | Content |
|---|---|
| **Objective** | Full mock travel workflow: search → compare → select → prepare → authenticate → payment → verify → ticket → email → WhatsApp → calendar → complete. |
| **Files affected** | TravelAgent, TravelProvider mocks, workflow definition, NotificationEngine consumers |
| **Dependencies** | Phases 3–6, 8–11 (notification/auth/payment at least as mocks) |
| **Implementation** | OFFICIAL_ONLY flag. Never claim booking success without confirmation. Current request overrides memory (Hotel B this time). |
| **Tests** | Workflow test for the exact demo utterance. Failure tests: no flights, timeout, duplicate booking, auth fail, payment fail. |
| **Acceptance criteria** | Demo utterance produces COMPLETED action, timeline, verified voice result, `[MOCK]` labeled. |
| **Status** | `NOT STARTED` |

---

## Phase 8 — Email

| Field | Content |
|---|---|
| **Objective** | EmailAgent + EmailProvider (Mock + SMTP/Gmail real). Validate recipient/subject/body/attachment. Never send to ambiguous recipient. |
| **Files affected** | New `EmailProvider`; replace `EmailTool` fake SENT; EmailAgent; SMTP config |
| **Dependencies** | Phase 4 |
| **Implementation** | Mock records sent messages in memory/DB for tests. Real uses env credentials. Unsafe send is not blindly retried. |
| **Tests** | Mock send without credentials. Ambiguous recipient → WAITING_FOR_INFORMATION. Duplicate idempotency key → one send. |
| **Acceptance criteria** | Tests pass without SMTP. Real mode documented as OPTIONAL + paid/account requirements. |
| **Status** | `NOT STARTED` |

---

## Phase 9 — WhatsApp (official API only)

| Field | Content |
|---|---|
| **Objective** | Official WhatsApp Cloud API adapter + mock. No WhatsApp Web automation. |
| **Files affected** | Replace lying `WhatsAppProvider` description; MockWhatsAppProvider; RealWhatsAppCloudProvider; NotificationEngine channel |
| **Dependencies** | Phase 4 |
| **Implementation** | Text + document. Delivery status. Credentials from env. Document Meta business account / approval / paid requirements honestly. |
| **Tests** | Mock complete path. Real tests skipped in CI without credentials (`BLOCKED BY EXTERNAL CREDENTIAL`). |
| **Acceptance criteria** | Demo works. Real adapter exists but is not faked as connected. |
| **Status** | `NOT STARTED` |

---

## Phase 10 — Calendar

| Field | Content |
|---|---|
| **Objective** | Create/update/cancel, conflict detection, timezone, participants, reminders. Mock + Google Calendar real. |
| **Files affected** | Replace hardcoded `CalendarTool`; CalendarProvider; CalendarAgent |
| **Dependencies** | Phase 4 |
| **Implementation** | Verify event creation (mock store or Google API). OAuth handoff, no password collection. |
| **Tests** | Create, conflict, timezone. Mock without OAuth. |
| **Acceptance criteria** | CalendarAgent uses provider. Hardcoded “Team Architecture Sync” is gone. |
| **Status** | `NOT STARTED` |

---

## Phase 11 — Tasks, research, finance, memory, developer, support, evaluation

| Field | Content |
|---|---|
| **Objective** | Complete remaining agents against contracts. |
| **Files affected** | TaskAgent/Tool, ResearchAgent, FinanceAgent, MemoryAgent (+ secret denylist), DeveloperAgent (read-only GitHub, no shell), SupportAgent (no secrets), EvaluationAgent (real metrics) |
| **Dependencies** | Phase 4–5 |
| **Implementation** | Finance: deterministic calc from actual option prices. Memory: no passwords/OTP/CVV/cards. Developer: permissioned inspect only. Evaluation: intentCorrect, entitiesCorrect, toolCorrect, providerCorrect, actionCompleted, resultVerified, responseAccurate from real data. |
| **Tests** | Per agent including failure paths. Memory refuses secret storage. Developer refuses shell. |
| **Acceptance criteria** | No hardcoded $258. No fake 96.4%. No `System.out` workflows. |
| **Status** | `NOT STARTED` |

---

## Phase 12 — Authentication and payment handoff

| Field | Content |
|---|---|
| **Objective** | AI prepares, pauses, user completes provider-secure auth/payment, system resumes. Never collect secrets via Vapi. |
| **Files affected** | ApprovalService, ApprovalController, frontend ApprovalsPage, MockAuthenticationProvider, MockPaymentProvider, ActionEngine resume |
| **Dependencies** | Phase 3 |
| **Implementation** | Dummy tokens must not auto-succeed except in explicit DEMO mock provider that still records a simulated provider confirmation event. Low-risk actions do not require human clicks. |
| **Tests** | Auth failure stays paused. Payment failure does not book. Resume from last valid state (no workflow restart). |
| **Acceptance criteria** | No card/OTP/CVV in events, logs, or memory. |
| **Status** | `NOT STARTED` |

---

## Phase 13 — NotificationEngine and WorkflowEngine

| Field | Content |
|---|---|
| **Objective** | Shared notifications (EMAIL, WHATSAPP, VAPI, FRONTEND). Reusable pause/resume workflows. |
| **Files affected** | New engines; TravelAgent must not duplicate notify logic; replace compiling WorkflowAgent stub |
| **Dependencies** | Phases 8–12 |
| **Implementation** | FLIGHT_BOOKING_WORKFLOW steps as specified. Pause on auth/payment. Resume. |
| **Tests** | Notification fan-out. Workflow pause/resume. Expired workflow fails safe. |
| **Acceptance criteria** | One notification implementation. TravelAgent calls the engine. |
| **Status** | `NOT STARTED` |

---

## Phase 14 — Frontend operations center

| Field | Content |
|---|---|
| **Objective** | Dashboard shows real backend state. Test Agent buttons invoke real backend test workflows. Timeline distinguishes SUCCESS / WAITING / RUNNING / FAILED / REQUIRES_USER / VERIFICATION. |
| **Files affected** | All `voiceos-frontend/src/pages/*`, Header, Sidebar, WebSocketService, new API clients |
| **Dependencies** | Phases 2–3, 5, catalog/status APIs |
| **Implementation** | Replace placeholders (Tasks, RAG, Metrics). AgentsPage from `/api/v1/agent/catalog`. Connected accounts from backend, not local fake “Connected”. Remove always-green webhook badge; use health endpoint. Add Test Agent per required agent. |
| **Tests** | Frontend build + lint. Manual/e2e: each Test Agent hits backend and does not show fake success if backend failed. Browser verification of console, approvals, timeline, agents. |
| **Acceptance criteria** | No placeholder pages. No fake connected WhatsApp. Timeline matches Action events. |
| **Status** | `NOT STARTED` |

---

## Phase 15 — Observability, performance, database completeness

| Field | Content |
|---|---|
| **Objective** | Structured logs with required IDs. No secret logging. Timeouts, pooling, bounded concurrency. Schema for remaining models without duplicate tables. |
| **Files affected** | logging pattern, MDC filter, Flyway Vn, remove unused Redis/Qdrant claims or actually use them if RAG is in scope |
| **Dependencies** | Phase 3 |
| **Implementation** | Either implement RAG against Qdrant or stop claiming it. Either use Redis for short-term memory or remove it from “working architecture” claims. |
| **Tests** | Log redaction test. Migration test on H2/Testcontainers Postgres. |
| **Acceptance criteria** | Logs have correlation IDs. No token/card/OTP in logs. Schema matches documented models. |
| **Status** | `NOT STARTED` |

---

## Phase 16 — Automated tests and CI

| Field | Content |
|---|---|
| **Objective** | Unit, integration, contract, workflow, security, Vapi, e2e. CI without paid credentials. |
| **Files affected** | `voiceos-backend/src/test/**`, optional Testcontainers, `.github/workflows/ci.yml`, frontend lint/build job |
| **Dependencies** | Phases 0b–15 as they land; CI can start after 0b |
| **Implementation** | Minimum CI: compile, unit, integration with mocks, static analysis. Failure tests from Phase 38. Voice dataset from Phase 36. |
| **Tests** | CI green on mocks. |
| **Acceptance criteria** | PR cannot merge if compile or mock tests fail. No real API keys in CI. |
| **Status** | `NOT STARTED` |

---

## Phase 17 — Documentation and environment

| Field | Content |
|---|---|
| **Objective** | Create/update the docs listed in product Phase 43. Honest `.env.example` with REQUIRED / OPTIONAL / DEMO ONLY / REAL MODE. |
| **Files affected** | `docs/**`, `.env.example`, README (remove false “complete” checkmarks) |
| **Dependencies** | Ongoing; finalize after demo e2e |
| **Implementation** | Replace README phase table with evidence-based status. Document each real API’s account/paid/OAuth needs. Do not claim free when not free. |
| **Tests** | n/a (review) |
| **Acceptance criteria** | Docs match code. No “should work” language. |
| **Status** | `NOT STARTED` (audit docs done) |

---

## Phase 18 — Real provider onboarding (one at a time)

| Field | Content |
|---|---|
| **Objective** | After DEMO e2e passes, enable real integrations in order: Vapi, webhook, email, calendar, WhatsApp, research, travel search, auth handoff, payment sandbox, booking sandbox. |
| **Files affected** | Real provider adapters, env, `docs/real-mode.md`, `docs/e2e-test-report.md` |
| **Dependencies** | Demo e2e green |
| **Implementation** | Sandbox only. No real purchases unless explicitly configured. Skip and document `BLOCKED BY EXTERNAL CREDENTIAL` when keys are absent. |
| **Tests** | Per-provider verify credentials, response, errors, rate limits, verification. |
| **Acceptance criteria** | Real mode reports unavailable integrations instead of fake success. |
| **Status** | `NOT STARTED` |

---

## Phase 19 — Live Vapi voice test and final readiness report

| Field | Content |
|---|---|
| **Objective** | Actual spoken calls in multiple languages. Produce `docs/final-readiness-report.md` with IMPLEMENTED / TESTED / VERIFIED / NOT IMPLEMENTED / BLOCKED BY EXTERNAL CREDENTIAL / PARTIALLY IMPLEMENTED only. |
| **Files affected** | `docs/final-readiness-report.md`, `docs/e2e-test-report.md`, `docs/reports/` dated report |
| **Dependencies** | Phases 7, 14, 16, 18 (Vapi at least) |
| **Implementation** | Speak, do not type. Verify transcript, language, intent, entities, tool call, backend execution, spoken result. |
| **Tests** | Manual voice protocol recorded with evidence (call id, action id, timeline screenshot/log). |
| **Acceptance criteria** | Do not mark VoiceOS DONE unless acceptance checklist in the product spec has passed with evidence. |
| **Status** | `NOT STARTED` |

---

## Dependency graph (summary)

```
0 Audit ──► 0b Compile/tests
              │
              ▼
            1 Security
              │
              ▼
            2 Single pipeline
              │
              ├─► 3 Action steps + audit events (DONE)
              ├─► 4 Providers/mocks (DONE)
              ├─► 6 Voice/multilingual
              │
              ▼
            5 Agent contracts
              │
              ├─► 8 Email ─┐
              ├─► 9 WhatsApp┤
              ├─► 10 Calendar┤──► 13 Notification + Workflow
              ├─► 11 Other agents
              └─► 12 Auth/Payment ┘
                              │
                              ▼
                         7 Travel demo e2e
                              │
                    14 Frontend ◄── 15 Observability/DB
                              │
                         16 Tests/CI (can start after 0b)
                         17 Docs
                              │
                         18 Real providers
                              │
                         19 Voice test + final report
```

---

## Immediate next phase

**Phase 1 — Secret hygiene and fail-closed security.**

Do not start automatically. Wait for an explicit request.

Phase 0B is complete: `mvnw.cmd test` — 18 tests, 0 failures.
