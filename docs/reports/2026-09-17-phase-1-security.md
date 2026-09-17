# VoiceOS Phase 1 Security Report

**Date:** 2026-09-17  
**Phase:** 1 — Security, Identity, Secrets, Webhook Integrity & Idempotency  
**Voice platform:** Vapi (unchanged)  
**Not claimed:** production-ready product

---

## 1. Objective

Establish a fail-closed security foundation so later autonomous multi-agent execution can attribute every action to a real identity:

```
User → Authentication → User Identity → Authorization
    → Vapi Webhook Verification → Idempotency → Request Context
    → Future Agent Execution
```

This phase does **not** implement travel, email, WhatsApp, payments, ActionEngine wiring, PolicyEngine wiring, or a new voice provider.

---

## 2. Initial security state

Source of truth after Phase 0B (`docs/reports/2026-09-17-phase-0b-compilation-recovery.md`):

- Spring Boot 3.3.4 / Java 21 / React 19 / Vite 8.2.1
- Backend compiled; 18 tests passing
- Known remaining problems:
  1. Webhook fail-open when secret blank
  2. Most `/api/v1/**` routes `permitAll`
  3. First-user identity fallback
  4. Unauthenticated `POST /api/v1/tools/{name}/execute`
  5. JWT non-empty default in `application.yml`
  6. Dummy auth/payment tokens treated as success
  7. WebSocket CORS `*`
  8. Idempotency key used `System.currentTimeMillis()`
  9. Previously committed Vapi credentials must be rotated outside git

---

## 3. Findings

Inspection confirmed the Phase 0B list and additional issues:

- Identity in controllers used `userRepository.findAll().stream().findFirst()` when JWT was absent
- `TaskTool` / `MemorySaveTool` used the same first-user fallback
- `ExecutionController` returned all users’ executions
- Approval `authenticate` defaulted `token` to `dummy-auth-token` and stored it as `oauth_token`
- Two Vapi secret properties (`voiceos.voice.vapi.webhook-secret` and `voiceos.webhooks.vapi-secret`); verification now reads both, still fail-closed
- Vapi DTOs have `call.id`, `toolCall.id`, optional `message.id`, `payload.timestamp` — no invented event ID
- Authentication model is stateless JWT Bearer in `Authorization` (no auth cookies)
- Git-tracked config no longer contains live Vapi keys; local `.env` is gitignored and must never be committed
- Previously committed Vapi credentials remain a rotation obligation outside the repository

---

## 4. Architecture changes

Centralized security rather than duplicating checks:

| Component | Role |
|---|---|
| `AuthenticatedUserService` | Identity from Spring Security context only |
| `JwtTokenProvider` | HS256 sign/verify; fail-closed secret validation |
| `JsonAuthEntryPoint` / `JsonAccessDeniedHandler` | 401 / 403 JSON, no stack traces |
| `VapiWebhookSecurityService` | Single Vapi `x-vapi-secret` verifier |
| `VapiIdempotencyKeyBuilder` | Stable key from actual Vapi fields |
| `VapiIdempotencyService` | DB unique-constraint claim + stored result |
| `VoiceOsRequestContext` + `RequestCorrelationFilter` | `X-Request-Id` / MDC `requestId` |
| `RequestAuditFilter` | method, path, status, duration, user email — no secrets |

Request context fields established (minimal): `requestId`, `userId`, `conversationId`, `callId`, `eventId`.

---

## 5. Authentication changes

- Register / login / refresh remain PUBLIC
- Valid credentials → JWT; invalid → 401; missing body fields → 400
- Expired and malformed Bearer tokens → 401 on protected routes
- Identity is the validated JWT subject (email) loaded from `UserRepository`
- Passwords remain BCrypt 12; auth logs email/id only
- JWT secret: required env `JWT_SECRET`, minimum 32 characters, known placeholders rejected, never auto-generated
- Frontend: compact header login/register, `localStorage` token, `Authorization` on API calls, 401 handling

---

## 6. Authorization changes

Endpoint classification:

| Class | Paths |
|---|---|
| PUBLIC | `/api/v1/auth/register`, `/login`, `/refresh`; `/actuator/health`, `/info`; swagger; webhook health |
| AUTHENTICATED | conversations, tasks, memory, tools, approvals, agent, metrics, system, executions |
| VAPI | `POST /api/webhooks/vapi`, `/api/v1/webhooks/vapi` (shared secret, not JWT) |
| ADMIN | none enforced yet (role exists, unused) |
| INTERNAL | none added |

Behavior:

- Authenticated request → that user
- Unauthenticated request → 401 (not another user)
- User A cannot read user B’s conversation (404)
- Tool execute injects JWT `userId` and overwrites any client-supplied `userId`
- Task/memory tools fail without a real `userId` (no first-user fallback)
- PolicyEngine is **not** wired (out of scope)

---

## 7. Vapi webhook security

Vapi remains the voice platform. Verification uses the existing `x-vapi-secret` header (the project’s actual mechanism). Logic lives only in `VapiWebhookSecurityService`.

| Condition | Result |
|---|---|
| Configured secret + matching header | ACCEPT |
| Configured secret + invalid/missing header | 401 |
| Missing secret, secure mode (default) | 401 fail-closed |
| Missing secret + `VAPI_WEBHOOK_ALLOW_INSECURE_LOCAL=true` | Allowed, with a warn log (documented local-only) |

The secret is never logged or returned in API bodies.

---

## 8. Idempotency implementation

Old key `callId + "_" + currentTimeMillis()` was not idempotent.

Stable key, in order, from the real event model:

1. `vapi:msg:{message.id}` when Vapi supplies `message.id`
2. `vapi:call:{callId}:tools:{sorted toolCall.ids}`
3. transcript: `vapi:call:{callId}:transcript:{sha256(role|transcript)}` (transcript text is not logged)
4. `vapi:call:{callId}:{type}:{status}` or `{timestamp}`
5. otherwise 400 — no invented event id

Storage: existing `webhook_events` table (unique `idempotency_key`) plus V2 columns `response_json`, `call_id`, `event_id`, `request_id`.

Concurrency: insert in a dedicated transaction. Unique-constraint races are treated as duplicates. The loser waits briefly for the stored result or returns 409. Duplicate HTTP responses replay the first JSON result without re-executing the tool.

---

## 9. Database migrations

| Migration | Purpose |
|---|---|
| `V1__init_schema.sql` | Unchanged (already applied) |
| `V2__webhook_event_idempotency_result.sql` | `response_json`, `call_id`, `event_id`, `request_id` + indexes |

Tests use H2 `ddl-auto=create-drop` with Flyway disabled (same as Phase 0B). PostgreSQL/Redis/Qdrant are **not required** for Phase 1 tests.

- PostgreSQL: required for real runtime
- Redis: configured, unused by Java in this phase
- Qdrant: configured, unused by Java in this phase

---

## 10. Secret/configuration changes

Environment (or equivalent) required:

- `VAPI_API_KEY`
- `VAPI_WEBHOOK_SECRET`
- `JWT_SECRET`
- `DATABASE_USERNAME` (local default username `voiceos` only)
- `DATABASE_PASSWORD` (empty default — must be supplied for Postgres)

Removed insecure YAML defaults: JWT fallback secret, Groq `mock_groq_key`, Redis password default.

`.env.example` no longer contains placeholder secrets that look real. Local `.env` is gitignored.

**CSRF decision:** VoiceOS uses stateless JWT Bearer tokens in the `Authorization` header and does not issue authentication cookies. Browser CSRF against cookie sessions does not apply. CSRF protection remains disabled. If cookie session auth is added later, CSRF must be re-enabled for that flow.

Dummy approval tokens:

- `MOCK` — only when `MOCK_EXTERNAL_APIS=true`; never a real OAuth/payment grant
- `STUB` — credential recorded, not provider-verified
- `REAL` — not issued in this phase

---

## 11. CORS changes

REST: configured origins from `FRONTEND_URL` plus localhost defaults. Wildcard `*` is stripped.

WebSocket: same origin list via `setAllowedOriginPatterns`. No `*`.

Allowed headers include `Authorization`, `Content-Type`, `X-Request-Id`, `x-vapi-secret`. `X-Request-Id` is exposed.

---

## 12. Tests

Existing tests kept (not weakened):

- `ToolRegistryTest` (8)
- `AgentRegistryTest` (3)
- `VapiWebhookServiceTest` (8, including duplicate non-execution)
- `VoiceOsApplicationTests` (1)

New:

- `JwtTokenProviderTest` — round-trip, expiry, malformed, blank/placeholder secret
- `VapiWebhookSecurityServiceTest` — valid / invalid / missing / insecure-local
- `VapiIdempotencyKeyBuilderTest` — stable keys, no clock
- `SecurityApiTest` — login, 401s, user isolation, tool execute, webhook accept/reject, duplicate event, concurrent duplicate
- `SourceSecretHygieneTest` — no JWT/Vapi fallback secrets in `application.yml`; no hardcoded `VAPI_*` / `JWT_SECRET=` in Java sources

---

## 13. Test results

**Before Phase 1 (Phase 0B):** Tests run: 18, Failures: 0, Errors: 0, Skipped: 0

**After Phase 1:**

```
mvnw.cmd clean compile   BUILD SUCCESS  Finished at: 2026-09-17T22:12:24+05:30
mvnw.cmd test            BUILD SUCCESS  Tests run: 46, Failures: 0, Errors: 0, Skipped: 0
mvnw.cmd package         BUILD SUCCESS  Finished at: 2026-09-17T22:19:30+05:30
                                        jar: voiceos-backend-1.0.0-SNAPSHOT.jar
npm run build            SUCCESS        vite v8.2.1, 3.06s
```

No tests skipped. No assertions weakened.

---

## 14. Remaining security risks

These are **not** production-ready claims. Remaining:

1. **Rotate previously committed Vapi API key and webhook secret** in the Vapi dashboard. This cannot be done from git.
2. Vapi inbound tool-calls authenticate as Vapi (shared secret), not as a JWT user. Mapping `customer.email` / call → VoiceOS user is not implemented.
3. PolicyEngine / ApprovalEngine are not the tool-execution gate. Authenticated users can still invoke registered tools over HTTP.
4. RBAC (`USER`/`ADMIN`) is unused.
5. OpenAPI/Swagger remains publicly reachable.
6. Rate-limit properties exist without an enforcing filter.
7. EmailAgent still contains a personal Gmail address (integrity/privacy leftover).
8. `livekit.version` Maven property leftover — LiveKit was **not** introduced.
9. Metrics still invent some quality scores.
10. Local `.env` must stay untracked.
11. WebSocket handshake is still unauthenticated (origins restricted only).
12. ActionEngine / Orchestrator pipeline still not a single fail-closed execution path.

---

## 15. Files changed

### New

- `voiceos-backend/src/main/java/com/voiceos/security/AuthenticatedUserService.java`
- `voiceos-backend/src/main/java/com/voiceos/security/VoiceOsRequestContext.java`
- `voiceos-backend/src/main/java/com/voiceos/security/RequestCorrelationFilter.java`
- `voiceos-backend/src/main/java/com/voiceos/security/RequestAuditFilter.java`
- `voiceos-backend/src/main/java/com/voiceos/security/JsonAuthEntryPoint.java`
- `voiceos-backend/src/main/java/com/voiceos/security/JsonAccessDeniedHandler.java`
- `voiceos-backend/src/main/java/com/voiceos/vapi/service/VapiWebhookSecurityService.java`
- `voiceos-backend/src/main/java/com/voiceos/vapi/service/VapiIdempotencyKeyBuilder.java`
- `voiceos-backend/src/main/java/com/voiceos/vapi/service/VapiIdempotencyService.java`
- `voiceos-backend/src/main/resources/db/migration/V2__webhook_event_idempotency_result.sql`
- `voiceos-backend/src/test/resources/application-test.yml`
- `voiceos-backend/src/test/java/com/voiceos/security/JwtTokenProviderTest.java`
- `voiceos-backend/src/test/java/com/voiceos/security/SecurityApiTest.java`
- `voiceos-backend/src/test/java/com/voiceos/security/SourceSecretHygieneTest.java`
- `voiceos-backend/src/test/java/com/voiceos/vapi/service/VapiWebhookSecurityServiceTest.java`
- `voiceos-backend/src/test/java/com/voiceos/vapi/service/VapiIdempotencyKeyBuilderTest.java`
- `voiceos-frontend/src/services/api.js`
- `docs/reports/2026-09-17-phase-1-security.md`

### Modified (Phase 1)

- `voiceos-backend/src/main/java/com/voiceos/config/SecurityConfig.java`
- `voiceos-backend/src/main/java/com/voiceos/config/WebSocketConfig.java`
- `voiceos-backend/src/main/java/com/voiceos/config/VoiceOsProperties.java`
- `voiceos-backend/src/main/java/com/voiceos/security/JwtTokenProvider.java`
- `voiceos-backend/src/main/java/com/voiceos/security/JwtAuthenticationFilter.java`
- `voiceos-backend/src/main/java/com/voiceos/exception/VoiceOsException.java`
- `voiceos-backend/src/main/java/com/voiceos/api/controller/AgentController.java`
- `voiceos-backend/src/main/java/com/voiceos/api/controller/ApprovalController.java`
- `voiceos-backend/src/main/java/com/voiceos/api/controller/ConversationController.java`
- `voiceos-backend/src/main/java/com/voiceos/api/controller/ExecutionController.java`
- `voiceos-backend/src/main/java/com/voiceos/api/controller/MemoryController.java`
- `voiceos-backend/src/main/java/com/voiceos/api/controller/MetricsController.java`
- `voiceos-backend/src/main/java/com/voiceos/api/controller/TaskController.java`
- `voiceos-backend/src/main/java/com/voiceos/api/controller/ToolController.java`
- `voiceos-backend/src/main/java/com/voiceos/service/ApprovalService.java`
- `voiceos-backend/src/main/java/com/voiceos/tool/impl/TaskTool.java`
- `voiceos-backend/src/main/java/com/voiceos/tool/impl/MemorySaveTool.java`
- `voiceos-backend/src/main/java/com/voiceos/domain/entity/WebhookEvent.java`
- `voiceos-backend/src/main/java/com/voiceos/vapi/controller/VapiWebhookController.java`
- `voiceos-backend/src/main/java/com/voiceos/vapi/service/VapiWebhookService.java`
- `voiceos-backend/src/main/java/com/voiceos/vapi/dto/VapiWebhookDTOs.java`
- `voiceos-backend/src/main/resources/application.yml`
- `voiceos-backend/src/main/resources/application-dev.yml`
- `voiceos-backend/src/test/java/com/voiceos/VapiWebhookServiceTest.java`
- `voiceos-backend/src/test/java/com/voiceos/VoiceOsApplicationTests.java`
- `voiceos-frontend/src/components/Header.jsx`
- `voiceos-frontend/src/pages/ConsolePage.jsx`
- `voiceos-frontend/src/pages/ApprovalsPage.jsx`
- `.env.example`
- `docs/current-state.md`
- `docs/implementation-roadmap.md`

Phase 0B files remain in the working tree (agents, SchedulingConfig, patch_vapi.py, etc.) and were not redone as Phase 1 work.

No LiveKit code was added. Vapi was not replaced.

---

## 16. Commands executed

```
git status
git log -5 --oneline
git diff --stat
mvnw.cmd clean compile     (voiceos-backend)  BUILD SUCCESS  2026-09-17T22:12:24+05:30
mvnw.cmd test              (voiceos-backend)  BUILD SUCCESS  Tests run: 46, Failures: 0, Errors: 0, Skipped: 0
mvnw.cmd package           (voiceos-backend)  BUILD SUCCESS  2026-09-17T22:19:30+05:30
npm run build              (voiceos-frontend) SUCCESS        vite v8.2.1, 3.06s
```

Git was not committed (not requested).

Secret scan of tracked sources: no live `VAPI_API_KEY=`, `VAPI_WEBHOOK_SECRET=`, or `JWT_SECRET=` values in configuration defaults. Actual secret values are not recorded in this report.

---

## 17. Final completion gate

- [x] Backend compiles
- [x] Existing tests still pass
- [x] New security tests pass
- [x] No tests skipped
- [x] No assertions weakened
- [x] No first-user fallback remains for authenticated operations
- [x] Protected API endpoints require authentication
- [x] Tool execution is not publicly accessible
- [x] Vapi webhook does not fail open
- [x] Stable idempotency exists
- [x] Duplicate webhook events cannot execute twice
- [x] Concurrent duplicate events are handled safely
- [x] JWT validation is enforced
- [x] Secrets are externalized
- [x] No real credentials remain in source/configuration
- [x] CORS is not unnecessarily unrestricted
- [x] Security errors are handled consistently
- [x] No sensitive information is logged (passwords, JWT, API keys, webhook secrets)
- [x] Vapi remains the voice platform
- [x] No LiveKit introduced
- [x] No business feature expansion occurred
- [x] Documentation updated
- [x] Phase 1 report created
- [x] Git diff reviewed

**Phase 1 is a secure foundation, not a complete VoiceOS product.**

**STOP. Do not start Phase 2 until this report is reviewed.**
