# VoiceOS Phase 3 — Action Steps and Audit Events

**Date:** 2026-09-17  
**Status:** COMPLETE (local architecture + tests). Live Vapi: **NOT VERIFIED**.  
**Not production-ready.** External travel/email/WhatsApp/calendar/payment providers remain MOCK or unimplemented. No LiveKit.

---

## 1. Objective

Convert the Phase 2 `Action` from a single persisted execution record into a durable execution record that contains:

1. An ordered sequence of `ActionStep` rows
2. Append-only `AuditEvent` rows
3. An owner-scoped timeline API
4. A Console display fed by real backend data

A debugger must be able to reconstruct what happened without application logs.

---

## 2. Starting state

Phase 2 established one canonical path:

```
Vapi / HTTP JWT
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

Verified at Phase 2:

- compile SUCCESS
- tests: 61 passed
- package SUCCESS
- frontend build SUCCESS
- Calculator REAL: `125 * 24` = `3000.00`
- Live Vapi **NOT VERIFIED**

Gaps:

- Action had no step history
- No durable audit events
- Console showed a local last-action summary, not a reconstructable timeline
- Failure/policy/identity stages were not queryable as structured evidence

---

## 3. Architecture

ActionEngine remains the **only** business execution coordinator.

`ActionTraceService` is an observer. It records steps and events. It does not select agents, evaluate policy, execute tools, or decide COMPLETED vs FAILED.

```
ActionEngine.execute()
    → persist Action
    → ActionTraceService.startStep / succeed / fail
    → ActionTraceService.emit(AuditEvent)
    → PolicyEngine / Agent / Tool / Provider / ResultVerifier
    → persist Action terminal status
    → GET /api/v1/actions/{id}/timeline  (owner-scoped DTO)
```

No second orchestration path was added. No Redis, Kafka, LiveKit, LangChain, or provider integrations.

---

## 4. ActionStep design

Entity: `com.voiceos.action.model.ActionStep`  
Table: `action_steps`  
Relationship: Action 1 → N ActionStep. Unique `(action_id, sequence_no)`.

### Step types (meaningful stages only)

| Type | When |
|---|---|
| `RECEIVED` | Action created |
| `VALIDATING` | Command has an operation/tool |
| `AUTHORIZING` | User identity resolved |
| `PLANNING` | Agent and tool selected |
| `POLICY_CHECK` | PolicyEngine decision |
| `TOOL_EXECUTION` | Agent/tool/provider ran |
| `VERIFICATION` | ResultVerifier |
| `APPROVAL_WAIT` | Policy REQUIRE_APPROVAL |
| `USER_WAIT` | not a separate type; `WAITING_USER` is a step **status** |
| `COMPLETION` | Terminal success step |

`AGENT_SELECTION` and `TOOL_SELECTION` are audit events on the PLANNING step, not extra rows. `PROVIDER_EXECUTION` is recorded as provider audit events on TOOL_EXECUTION. `FAILURE` is a step **status**, not a type.

### Step status

`PENDING → RUNNING → SUCCEEDED | FAILED | WAITING_USER | CANCELLED`

`PENDING` may also go to `WAITING_USER`, `SKIPPED`, or `CANCELLED`.

Terminal: `SUCCEEDED`, `FAILED`, `SKIPPED`, `CANCELLED`. A terminal step cannot return to `RUNNING`. Invalid transitions throw `IllegalStateException`.

Each step stores: id, actionId, sequenceNo, type, status, startedAt, completedAt, durationMs, agent/tool/provider/providerMode when known, requestId, redacted metadata/result JSON, errorCode, errorMessage.

---

## 5. AuditEvent design

Entity: `com.voiceos.action.model.AuditEvent`  
Table: `audit_events`  
Append-only from application APIs. No UPDATE or DELETE endpoints.

### Event types used

`ACTION_CREATED`, `ACTION_VALIDATED`, `IDENTITY_RESOLVED`, `IDENTITY_UNRESOLVED`, `POLICY_EVALUATED`, `POLICY_DENIED`, `APPROVAL_REQUIRED`, `AGENT_SELECTED`, `TOOL_SELECTED`, `TOOL_STARTED`, `TOOL_SUCCEEDED`, `TOOL_FAILED`, `PROVIDER_STARTED`, `PROVIDER_SUCCEEDED`, `PROVIDER_FAILED`, `VERIFICATION_STARTED`, `VERIFICATION_PASSED`, `VERIFICATION_FAILED`, `ACTION_COMPLETED`, `ACTION_FAILED`, `ACTION_REJECTED`, `ACTION_CANCELLED`

Actor types: `SYSTEM`, `USER`, `VAPI`, `AGENT`, `TOOL`, `PROVIDER`.

Fields: id, actionId, actionStepId (nullable), eventType, eventKey, occurredAt, requestId, callId, userId, actorType, actorId, redacted metadata JSON.

Missing correlation values are stored as null. They are never invented.

---

## 6. Database changes

Flyway **V4** (`V4__action_steps_and_audit_events.sql`). Next number after existing V3.

`action_steps`:

- FK `action_id → actions.id` ON DELETE CASCADE
- UNIQUE `(action_id, sequence_no)`
- indexes: `action_id`, `(action_id, status)`, `request_id`

`audit_events`:

- FK `action_id → actions.id` ON DELETE CASCADE
- FK `action_step_id → action_steps.id` ON DELETE SET NULL
- UNIQUE `event_key`
- indexes: `(action_id, occurred_at)`, `action_step_id`, `request_id`

JSONB is used only for small redacted metadata/result maps. Tests use H2 `ddl-auto: create-drop` (Flyway off), matching Phase 1/2.

---

## 7. API changes

Authenticated, owner-scoped, DTO-only:

| Method | Path | Returns |
|---|---|---|
| GET | `/api/v1/actions/{id}` | existing action view |
| GET | `/api/v1/actions/{id}/timeline` | Action + ordered steps + ordered events + flattened timeline |
| GET | `/api/v1/actions/{id}/steps` | ordered StepView list |
| GET | `/api/v1/actions/{id}/events` | ordered EventView list |

Non-owners receive **404** (same as Phase 2 action GET — no existence leak, no foreign callId/result/timeline).

No audit write/update/delete APIs.

---

## 8. Security model

Phase 1 guarantees preserved:

- JWT on `/api/v1/**`
- Vapi webhook secret validation
- VoiceSession bind of `callId`
- owner-only action access
- no first-user fallback
- secrets not in application.yml defaults

Phase 3 additions:

- `SensitiveDataRedactor` on Action.payload, step result/metadata, audit metadata
- keys such as password/jwt/authorization/token/secret/apiKey/otp/card/cvv redacted to `[REDACTED]`
- JWT-shaped strings and `Bearer ` values of sufficient length also redacted
- logs for observability failures include exception **class name** only, not payloads

No admin RBAC was invented.

---

## 9. Transaction decisions

Existing convention: ActionEngine is not `@Transactional` around the whole pipeline. Action rows are saved at each status change.

**Business persistence failure** (Action save fails): execution fails. That is a business failure.

**Observability persistence failure** (step or audit insert fails after a tool already ran):

- caught as `RuntimeException`
- logged with step/event type, actionId, and exception class name
- **does not** rewrite COMPLETED → FAILED
- **does not** silently swallow without a log

Rationale: a real calculator result of `3000.00` must not be reported as FAILED because an audit row could not be written. The Action remains the source of business truth. The missing audit row is an observability gap, not a fabricated success.

Duplicate `event_key` on append is ignored (idempotent replay), not treated as a crash.

This is an explicit tradeoff: audit is evidence, not a second commit coordinator.

---

## 10. Idempotency strategy

Three layers, no `System.currentTimeMillis()` identity:

1. **Vapi webhook** unique `idempotency_key` (Phase 1) — retry never re-enters ActionEngine
2. **Action** unique `idempotency_key` (Phase 2) — `ActionEngine.execute` returns the prior Action and **does not emit new steps/events**
3. **Audit** unique `event_key` = `actionId + ":" + eventType + ":" + (stepId or "action")` — same logical occurrence cannot duplicate; different steps can emit the same event type

Two legitimate `TOOL_STARTED` events on different actions or different tool-execution steps do not collapse.

---

## 11. Frontend changes

- `voiceos-frontend/src/services/api.js`: `fetchActionTimeline(actionId)` uses existing `apiFetch` JWT/401 handling
- `ConsolePage.jsx`: after HTTP execute, loads `/actions/{id}/timeline`
- Last Action panel shows actionId, status, agent, tool, provider/mode, policy, verification, result, error, duration, and backend timeline items
- Honest empty state when no actions exist
- No mock execution rows. Missing fields render `n/a` or `unavailable`

Vapi remains the voice interface. The Console is still an observability/control-plane UI.

---

## 12. Tests

Existing Phase 2 suite retained. New/extended coverage:

| Test | What it proves |
|---|---|
| `ActionStepTransitionTest` | legal transitions; terminal cannot return to RUNNING |
| `SensitiveDataRedactorTest` | jwt/password/authorization redacted; expression kept |
| `ActionEnginePipelineTest` | calculator timeline; policy deny without TOOL_STARTED; provider failure without TOOL_SUCCEEDED/VERIFICATION_PASSED; unbound IDENTITY_UNRESOLVED; duplicate idempotency does not grow the event list; secrets not in payload/audit blob |
| `ExecutionPipelineE2ETest` | Vapi-like 125*24=3000.00 timeline; duplicate webhook ACTION_COMPLETED count=1; forbidden DENY timeline; probe_fail FAILED timeline; user B 404 on A’s timeline/steps/events |

---

## 13. Build results

```
mvnw.cmd clean compile   BUILD SUCCESS  (2026-09-17T23:12:04+05:30)
mvnw.cmd test            BUILD SUCCESS  Tests run: 67, Failures: 0, Errors: 0, Skipped: 0
mvnw.cmd package         BUILD SUCCESS  (2026-09-17T23:19:27+05:30)  jar: voiceos-backend-1.0.0-SNAPSHOT.jar
npm run build (frontend) SUCCESS        vite v8.2.1, 3.07s
```

---

## 14. Known limitations

- Live Vapi path is **NOT VERIFIED** (local MockMvc webhook only)
- Observability insert failure after a successful tool leaves Action COMPLETED with a logged gap
- `processAction` / `resumeAction` legacy path still exists for pause/resume; live HTTP/Vapi execute does not use it
- FinanceAgent still lists calculator in `getTools()`; UtilityAgent priority 10 still wins for calculator
- Travel/email/WhatsApp/calendar/payment remain MOCK or unimplemented
- No admin cross-user timeline
- Frontend timeline is loaded after HTTP execute; live Vapi Console timeline still depends on a later live-Vapi phase
- WebSocket “live stream” items are conversation events, not the durable audit log

---

## 15. Deferred work

- Honest provider abstractions (Phase 4)
- Real Travel/Email/WhatsApp/Calendar/Payment
- Live Vapi verification
- RAG / Qdrant / Redis orchestration
- Admin RBAC
- Outbox / async audit bus (intentionally not added)

---

## 16. Exact next recommended phase

**Phase 4 — Provider abstraction and honest mocks.**

Do not implement TravelAgent product flows, real booking APIs, real WhatsApp, real email, LiveKit, or live Vapi testing until that phase is authorized.

Keep the single ActionEngine path. Providers must be labeled REAL / MOCK / STUB / UNIMPLEMENTED honestly.
