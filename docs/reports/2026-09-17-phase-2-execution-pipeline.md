# VoiceOS Phase 2 — Unified Execution Pipeline

**Date:** 2026-09-17  
**Status:** COMPLETE (architecture + local E2E). Live Vapi: **NOT VERIFIED**.  
**Not production-ready.** External travel/email/WhatsApp/calendar/payment providers remain MOCK or unimplemented.

---

## Objective

Establish **one** canonical business execution path:

```
Vapi
  → VapiWebhookController
  → VapiWebhookSecurityService
  → VapiWebhookService (idempotency)
  → VoiceSession / VoiceOsRequestContext
  → ActionEngine
  → PolicyEngine
  → AgentRegistry / AgentSelector
  → Agent
  → ToolRegistry
  → Tool
  → Provider
  → Verification
  → persisted Action
  → Vapi / HTTP response
```

CalculatorTool is the first **REAL** execution example (`125 * 24` → `3000.00`). No travel, email, WhatsApp, calendar, payment, browser, or RAG product work was added.

---

## Starting architecture

Phase 1 had JWT, webhook secret validation, and database-backed webhook idempotency.

Execution was **not** unified:

| Path | What actually ran |
|---|---|
| Vapi tool-calls | `VapiToolService` → `ToolRegistry.executeTool` or `OrchestratorService` |
| HTTP `POST /api/v1/tools/{name}/execute` | `ToolRegistry.executeTool` after JWT |
| HTTP `POST /api/v1/agent/execute` | `OrchestratorService.processUserRequest` |
| `ActionEngine` | In-memory `ConcurrentHashMap`; unused by Vapi/HTTP |
| `PolicyEngine` | Keyword stub (`pay`/`book`/`email`) |
| `ResultVerifier` | Non-empty result string → `COMPLETED` |
| Identity | Vapi tool-calls had **no** VoiceOS user |

READY paused forever (`ActionEngine.processAction` never entered EXECUTING for low-risk work).

---

## Problems found

1. Three parallel execution paths; ActionEngine and PolicyEngine were bypassed.
2. Vapi tool-calls were not mapped to a JWT user. `customer.email` was not used (good) but neither was any session bind — tools ran without a user.
3. Policy was natural-language keyword matching, not a structured gate.
4. ResultVerifier treated any non-empty text as success.
5. WhatsAppProvider description claimed an official API while remaining MOCK.
6. MetricsController invented `0.965` / `0.982` / `14`.
7. Action was a POJO, not persisted.
8. Conversation.voiceSessionId existed; there was no VoiceSession entity.
9. FinanceAgent `canHandle("calculate")` then hardcoded `68 + 140 + 50` instead of evaluating the user expression.

---

## Architecture designed

Smallest safe change: keep existing Agent/Tool/Provider types, persist Action, insert ActionEngine as the only coordinator.

```
Request (Vapi or HTTP JWT)
    → identity (JWT user, or VoiceSession bound to JWT user)
    → Action created (RECEIVED)
    → VALIDATED / AUTHORIZED
    → AgentSelector (tool ownership, then AgentRegistry.findHandler)
    → PLANNED
    → PolicyEngine ALLOW | DENY | REQUIRE_APPROVAL
    → EXECUTING (agent may run only an authorized tool)
    → VERIFYING (explicit tool success + required payload)
    → COMPLETED | FAILED | REJECTED | REQUIRES_APPROVAL
```

No LangChain, Kafka, LiveKit, or extra microservice.

---

## ActionEngine changes

`ActionEngine.execute(ActionCommand)` is the canonical entry.

Responsibilities: validate, create/load action (idempotency key), persist transitions, invoke PolicyEngine, select agent, execute authorized tool, verify, persist final state, return `ActionExecutionResult`.

`initiateAction` now delegates to `execute`. READY no longer deadlocks: `processAction` advances READY → EXECUTING for the legacy path.

HTTP:

- `POST /api/v1/agent/execute` → ActionEngine
- `POST /api/v1/tools/{name}/execute` → ActionEngine (not ToolRegistry)
- `OrchestratorService.processUserRequest` → ActionEngine (no parallel orchestrator path)

Vapi:

- `VapiToolService` → ActionEngine only

Observability (not a second execute API as the primary surface):

- `GET /api/v1/actions`
- `GET /api/v1/actions/{id}` (owner only)
- `POST /api/v1/actions/{id}/cancel`

---

## PolicyEngine changes

Structured `PolicyRequest` / `PolicyDecision`:

| Input | Decision |
|---|---|
| No userId | DENY `UNAUTHENTICATED` |
| Tool `forbidden` or `payment_charge` | DENY `FORBIDDEN_TOOL` |
| Risk HIGH / CRITICAL (`send_email`, booking, …) | REQUIRE_APPROVAL |
| Calculator / LOW / MEDIUM | ALLOW |

Not keyword matching on free-text intent. Risk comes from the tool’s declared `ToolRiskLevel` (with structured inference from tool name / action type as fallback).

This is a **minimal real gate**, not a complete authorization product.

---

## Agent routing

Existing `AgentRegistry` is unchanged as the registry.

New `AgentSelector` / `RegistryAgentSelector`:

1. If a requested tool is present, pick the highest-priority agent that **owns** that tool.
2. Else `AgentRegistry.findHandler`.
3. Else `ConversationAgent`.

`UtilityAgent` (priority 10) owns `calculator`, `probe_fail`, `forbidden`. It executes only authorized tools via ToolRegistry. Arithmetic like `125 multiplied by 24` is handled here so FinanceAgent cannot substitute a hardcoded budget.

---

## Tool routing

Agents determine/authorize tools. Client-supplied tool names are not executed until:

- an agent owns the tool, **and**
- PolicyEngine allows it.

Unknown tools → FAILED. Policy DENY → REJECTED, tool never runs.

---

## Provider routing

- `ProviderMode`: MOCK | REAL
- `CalculatorProvider` / `RealLocalCalculatorProvider` — **REAL** local arithmetic
- `CalculatorTool` delegates to that provider
- `TravelProvider` / `WhatsAppProvider` — explicit **MOCK** descriptions
- `probe_fail` — MOCK failing provider for tests

Agents do not contain calculator HTTP calls. Calculator has no network.

---

## Verification

`ActionVerifier` + `ResultVerifier`:

- Missing tool result (and no agent-only text) → fail
- `toolResult.success() == false` → fail
- Empty payload on success → fail
- Calculator must include `data.result`
- Non-empty error text is **not** treated as success
- COMPLETED only after verification passes

---

## Vapi integration

Unchanged endpoints:

- `POST /api/webhooks/vapi`
- `POST /api/v1/webhooks/vapi`

Phase 1 security + `webhook_events` idempotency reused.

Tool-calls no longer execute tools directly. Duplicate events still return the stored Vapi result and do not re-enter ActionEngine.

---

## Voice session identity

New `voice_sessions` + `VoiceSessionService`.

Bind (JWT required):

```
POST /api/v1/voice/sessions
{ "callId": "<vapi-call-id>", "conversationId": "<optional>" }
```

Resolve on webhook: `callId` → bound, unexpired user. Unbound → Action REJECTED, calculator does not run.

**Not used as authentication:**

- `customer.email`
- first user / default user
- unauthenticated Vapi customer payload

**Production binding still required:** the Vapi Web SDK must supply `call.id` so the logged-in dashboard user can bind before tool-calls. If the SDK message has no call id, bind manually with the known call id. This is documented; it is not an implicit email map.

Frontend Console attempts bind when a Vapi message includes `call.id`.

---

## Idempotency

| Layer | Key | Duplicate behavior |
|---|---|---|
| Webhook (Phase 1) | `vapi:call:{callId}:tools:{toolCallId}` on `webhook_events` | Return stored response; calculator not re-executed |
| Action | `action:vapi:{callId}:{toolCallId}` unique on `actions` | Return existing Action |

Concurrent webhook duplicates still collapse on the unique webhook constraint (Phase 1).

---

## Database changes

Flyway **V1 unchanged**. **V2 unchanged**.

**V3** `V3__actions_and_voice_sessions.sql`:

- `voice_sessions` (call_id unique, user_id, status, expires_at)
- `actions` (user, conversation, call, request, event, idempotency_key unique, agent, tool, provider, provider_mode, status, payload, result, error, verification_passed, timestamps, duration)

Tests use H2 `ddl-auto=create-drop` (Flyway off), as in Phase 1.

Runtime PostgreSQL is required for a real backend. Redis is unused by this pipeline. Qdrant is unused by this pipeline.

---

## Tests

Before Phase 2: **46** passed.

After Phase 2: **61** passed, **0** failed, **0** errors, **0** skipped.

Added:

- `ActionEnginePipelineTest` — calculator 3000.00, deny, fail, unbound user, action-level idempotency
- `PolicyEngineTest` — ALLOW / DENY / REQUIRE_APPROVAL / unauthenticated
- `ExecutionPipelineE2ETest` — Vapi-like webhook calculator, duplicate webhook, policy deny, provider fail, user isolation
- `VapiWebhookServiceTest.unboundVoiceSessionRejectsToolCall`

Existing 46 kept. Existing webhook calculator tests now bind a VoiceSession (intentional security change).

Assertions were not weakened. Calculator result is computed (`125 * 24` = `3000.00`), not hardcoded.

---

## Live Vapi verification

**NOT VERIFIED.**

No live Vapi call was placed against a public webhook in this phase. Local automated tests simulate Vapi HTTP payloads only.

### Manual steps (later)

1. Start PostgreSQL: `docker compose up -d postgres` (port 5433). Redis/Qdrant not required for this pipeline.
2. Configure `.env` (gitignored): `JWT_SECRET`, `VAPI_WEBHOOK_SECRET`, `VAPI_API_KEY`, `VAPI_ASSISTANT_ID`, `VAPI_PUBLIC_KEY`, `DATABASE_URL` / username / password. Do not commit secrets.
3. Run backend (`start-backend.ps1` or `mvnw.cmd spring-boot:run`).
4. Expose HTTPS: `ngrok http 8080` (or equivalent). **Do not hardcode or commit the tunnel URL.**
5. In Vapi dashboard, set assistant Server URL to `https://<tunnel>/api/webhooks/vapi` and the same webhook secret as `VAPI_WEBHOOK_SECRET`.
6. Register/login on the frontend. Start a Vapi call. Confirm `POST /api/v1/voice/sessions` binds `callId` to the JWT user.
7. Ensure the assistant tool-call name is `calculator` with argument `expression`.
8. Speak: “Calculate 125 multiplied by 24.”
9. Expect: webhook 200, Action `COMPLETED`, tool `calculator`, result `3000.00`, spoken result from Vapi TTS — not a hardcoded phrase.
10. Replay the same webhook payload: one `webhook_events` row, calculator must not run twice.

If bind is missing, the voice response is a session/user error, not a calculated number.

---

## Build results

```
mvnw.cmd clean compile   BUILD SUCCESS  2026-09-17T22:49:22+05:30
mvnw.cmd test            BUILD SUCCESS  2026-09-17T22:53:50+05:30
                         Tests run: 61, Failures: 0, Errors: 0, Skipped: 0
mvnw.cmd package         BUILD SUCCESS  2026-09-17T22:55:43+05:30
                         jar: voiceos-backend-1.0.0-SNAPSHOT.jar
npm run build            SUCCESS        vite v8.2.1, 2.33s
```

JAVA_HOME used by the wrapper: local JDK (compile succeeded). No secrets were printed or committed.

---

## Files changed

### New

- `voiceos-backend/src/main/resources/db/migration/V3__actions_and_voice_sessions.sql`
- `action/model/ActionCommand.java`
- `action/model/ActionExecutionResult.java`
- `action/store/ActionStore.java`
- `action/store/ActionRepository.java`
- `action/store/JpaActionStore.java`
- `action/store/InMemoryActionStore.java`
- `action/engine/ActionVerifier.java`
- `provider/ProviderMode.java`
- `provider/calculator/CalculatorProvider.java`
- `provider/calculator/RealLocalCalculatorProvider.java`
- `domain/entity/VoiceSession.java`
- `domain/repository/VoiceSessionRepository.java`
- `service/VoiceSessionService.java`
- `service/PolicyRequest.java`
- `service/PolicyDecision.java`
- `agent/core/AgentSelector.java`
- `agent/core/RegistryAgentSelector.java`
- `agent/impl/UtilityAgent.java`
- `tool/impl/ProbeFailTool.java`
- `tool/impl/ForbiddenTool.java`
- `api/controller/ActionController.java`
- `api/controller/VoiceSessionController.java`
- `src/test/java/com/voiceos/action/ActionEnginePipelineTest.java`
- `src/test/java/com/voiceos/service/PolicyEngineTest.java`
- `src/test/java/com/voiceos/pipeline/ExecutionPipelineE2ETest.java`
- `docs/reports/2026-09-17-phase-2-execution-pipeline.md`

### Modified (Phase 2)

- `Action.java` (JPA entity)
- `ActionStatus.java`
- `ActionEngine.java`
- `ActionPlanner.java`
- `ActionExecutor.java`
- `ResultVerifier.java`
- `PolicyEngine.java`
- `ServiceProvider.java`
- `TravelProvider.java` / `WhatsAppProvider.java` (MOCK labels)
- `Tool.java` / `CalculatorTool.java`
- `VapiToolService.java`
- `OrchestratorService.java` (delegates to ActionEngine)
- `AgentController.java` / `ToolController.java` / `MetricsController.java`
- `VoiceOsRequestContext.java` / `RequestCorrelationFilter.java`
- `VapiWebhookServiceTest.java` / `SecurityApiTest.java`
- `voiceos-frontend/src/pages/ConsolePage.jsx`
- `docs/current-state.md` / `docs/implementation-roadmap.md`

---

## Remaining limitations

1. Live Vapi end-to-end **NOT VERIFIED**.
2. Production voice identity requires an explicit JWT↔`callId` bind; SDK call-id delivery is not guaranteed.
3. Policy is a minimal structured gate, not a full ABAC/RBAC/payment authorization system.
4. Travel, email, WhatsApp, calendar, payments, browser automation, RAG: still MOCK/STUB or not real.
5. FinanceAgent still contains hardcoded budget numbers if it is selected for budget/cost language (calculator arithmetic is UtilityAgent).
6. Orchestrator multi-step planner methods still exist as unused private code; HTTP/Vapi no longer use that path.
7. Cancellation contract exists (`CANCELLED`); not implemented per provider.
8. Timeouts are recorded after tool return; tools are still synchronous (no unbounded thread pool added).
9. Metrics: unavailable unless Evaluation rows exist; hallucination/approval scores are **unavailable**, not invented.
10. Frontend Console is observability-only; Vapi remains the voice interface.
11. Redis optional/unused. Qdrant optional/unused. PostgreSQL required for real runtime.
12. Not production-ready.

---

## Next phase

**STOP.** Do not start Phase 3 implementation in this change set.

Roadmap Phase 3 was originally “persist Actions / idempotency”. Action persistence and webhook idempotency are **already delivered** (Phase 1 + Phase 2 V3). After review, the next implementation phase should **not** be TravelAgent / real Email / WhatsApp / Calendar.

Suggested next reviewed phase: **Phase 3 re-scope** (ActionStep/audit events if still needed) **or skip to honest provider adapters (roadmap Phase 4)** — decision is for review, not this report.

Do not implement real flight/hotel/email/WhatsApp until Phase 2 has been reviewed.
