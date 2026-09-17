# VoiceOS Phase 4 — Provider Abstraction and Honest Mocks

**Date:** 2026-09-17  
**Status:** COMPLETE (local architecture + tests). Live Vapi: **NOT VERIFIED**.  
**Not production-ready.** No real travel/email/WhatsApp/calendar/payment integrations. No LiveKit.

---

## 1. Objective

Make the Tool → Provider layer explicit, extensible, and honest.

- REAL means the operation actually ran
- MOCK means simulation, labeled as such
- Missing REAL configuration is UNAVAILABLE / MISCONFIGURED
- REAL never silently falls back to MOCK

ActionEngine remains the only business execution coordinator.

---

## 2. Current provider architecture (before)

| Piece | State |
|---|---|
| `ServiceProvider` | Mutated `Action` directly; default mode MOCK |
| `CalculatorProvider` | Separate interface; REAL local arithmetic |
| `TravelProvider` | MOCK class that said "Booking confirmed. Ticket XYZ-123 generated." |
| `WhatsAppProvider` | MOCK, mostly honest after Phase 2 |
| `EmailTool` / `CalendarTool` | Claimed SENT / invented meetings without a provider |
| `ProbeFailTool` | Hardcoded failure, no provider object |
| `ActionExecutor` | Private `ConcurrentHashMap` of ServiceProviders |
| `ProviderMode` | REAL / MOCK (kept) |

Problems: dishonest booking language, 0.00 treated as calculator success, no structured timeout/unavailable, tools could instantiate providers, HTTP params could theoretically carry a provider name (ignored in practice, now stripped).

---

## 3. Problems found

1. Travel mock claimed a real booking.
2. EmailTool returned `status: SENT` without sending.
3. CalendarTool invented a real-looking schedule.
4. Calculator returned `0.00` success for non-numeric input.
5. No shared ProviderResult (success/failure/unavailable/timeout).
6. No provider registry / capability lookup.
7. REAL vs MOCK was not a configuration policy with fail-closed fallback rules.

---

## 4. New provider contract

```
Tool
  → ProviderRequest
  → ServiceProvider.execute()
  → ProviderResult (SUCCESS | FAILURE | UNAVAILABLE | TIMEOUT)
  → ToolResult
  → ResultVerifier
  → Action / ActionStep / AuditEvent
```

Shared types: `ProviderRequest`, `ProviderResult`, `ProviderErrorCode`, `ProviderCapability`, `ProviderAvailability`, `ProviderBinding`.

`ServiceProvider` no longer mutates Action. `ActionExecutor` (legacy pause/resume) now uses `ProviderRegistry` + `ProviderResult`.

---

## 5. Provider mode model

`ProviderMode.REAL` | `ProviderMode.MOCK` (unchanged enum).

Mode is taken from the provider implementation / binding, not from the provider class name string. It is stored on Action, ActionStep, AuditEvent metadata, ToolResult data, and the Console.

---

## 6. Registry / selection

`ProviderRegistry` registers beans by name and by capability. Duplicate names fail. Duplicate capabilities log and keep the first owner.

Selection is server-side:

- CalculatorTool → CalculatorProvider (RealLocalCalculatorProvider)
- EmailTool → EmailProvider (MockEmailProvider)
- CalendarTool → CalendarProvider (MockCalendarProvider)
- ProbeFailTool → FailingProbeProvider

HTTP `provider` / `providerName` / `providerClass` params are stripped. There is no provider execute API.

---

## 7. Mock semantics

Mocks identify `mode=MOCK` and `status=SIMULATED`. Messages start with `MOCK:`.

| Provider | Honest result |
|---|---|
| MockTravelProvider | "MOCK: simulated flight search result" / "MOCK: simulated booking request — not a real ticket" |
| MockWhatsAppProvider | "MOCK: WhatsApp message simulated" (`delivered=false`) |
| MockEmailProvider | "MOCK: email send simulated …" (`sent=false`) |
| MockCalendarProvider | "MOCK: simulated calendar read/write" |
| MockPaymentProvider | Quote simulated; capture/authorize/refund `UNSUPPORTED_OPERATION`; cards rejected |
| FailingProbeProvider | MOCK failure (or simulated TIMEOUT) |

---

## 8. Real semantics

Only `RealLocalCalculatorProvider` is REAL. It computes the submitted expression.

If calculator binding is set to MOCK, availability is MISCONFIGURED and execute returns UNAVAILABLE — it does not invent a mock calculator.

If travel/whatsapp/email/calendar/payment binding is REAL, availability is MISCONFIGURED (no real implementation in this phase) and execute returns CONFIGURATION_ERROR. No silent mock search/send.

---

## 9. Error model

Codes: CONFIGURATION_ERROR, AUTHENTICATION_ERROR, AUTHORIZATION_ERROR, RATE_LIMITED, NETWORK_ERROR, TIMEOUT, REMOTE_ERROR, INVALID_REQUEST, UNAVAILABLE, UNSUPPORTED_OPERATION, SIMULATED_FAILURE.

ToolResult failure messages are `CODE: safe message`. Credentials are redacted via `SensitiveDataRedactor`.

---

## 10. Timeout model

No new thread pool. `ProviderResult.timeout()` exists. `ToolRegistry` converts a successful tool result whose reported or wall duration exceeds `getTimeoutMs()` into `TIMEOUT` failure. ActionEngine still fails the action if tool duration exceeds agent timeout.

---

## 11. Security

Phase 1/3 guarantees unchanged. Provider credentials stay server-side. Catalog `GET /api/v1/providers` is JWT-authenticated and returns name/mode/availability/capabilities only. Payment provider refuses card/OTP params and does not persist them.

---

## 12. Audit integration

Existing ActionTraceService events: PROVIDER_STARTED / PROVIDER_SUCCEEDED / PROVIDER_FAILED. ActionEngine copies `provider` and `providerMode` from ToolResult data onto the Action so timeline/API match the provider that actually ran.

---

## 13. Tests

Existing 67 tests retained (invalid calculator assertion updated from fake `0.00` success to honest FAILURE). New: ProviderHonestyTest (13), ToolRegistry timeout, E2E provider catalog + ignored client provider name.

---

## 14. Build results

```
mvnw.cmd clean compile   BUILD SUCCESS  (2026-09-17T23:42:58+05:30)
mvnw.cmd test            BUILD SUCCESS  Tests run: 83, Failures: 0, Errors: 0, Skipped: 0
mvnw.cmd package         BUILD SUCCESS  (2026-09-17T23:47:17+05:30)  jar: voiceos-backend-1.0.0-SNAPSHOT.jar
npm run build (frontend) SUCCESS        vite v8.2.1, 2.03s
```

---

## 15. Known limitations

- Live Vapi **UNVERIFIED**
- SearchTool / GitHubTool / DocumentSearchTool are not yet on this provider contract
- Memory/Task tools talk to local stores, not ServiceProvider
- LLM and Vapi voice providers are a separate package (`ai/`, `voice/`)
- No real airline/hotel/email/WhatsApp/calendar/payment APIs
- Product is **not production-ready**

---

## 16. Deferred provider integrations

Real FlightProvider, HotelProvider, Email SMTP, WhatsApp Cloud, Google Calendar, payment capture. Browser automation. Live Vapi verification.

---

## 17. Next phase recommendation

**Phase 5 — Agent contracts** (existing roadmap). Do not start real external providers until a later authorized phase. Do not introduce LiveKit.
