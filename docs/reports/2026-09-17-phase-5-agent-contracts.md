# VoiceOS Phase 5 — Agent Contracts and Routing Foundation

**Date:** 2026-09-17  
**Status:** COMPLETE (local architecture + tests). Live Vapi: **NOT VERIFIED**.  
**Not production-ready.** No real external providers. No LLM integration. No LiveKit.

---

## 1. Objective

Make the Agent layer an explicit, structured, deterministic capability contract inside the existing ActionEngine pipeline.

Agents decide which logical tool is appropriate. PolicyEngine decides whether it is allowed. Tools perform operations. Providers perform service-specific work. ActionEngine remains the only business execution coordinator.

This is **not** an LLM integration phase. The contract is model-agnostic so a future AI system can later feed structured `AgentResult` values.

---

## 2. Starting architecture

Canonical path (unchanged):

Vapi / HTTP JWT → VoiceSession / VoiceOsRequestContext → ActionEngine → PolicyEngine → AgentSelector / AgentRegistry → Agent → ToolRegistry → Tool → Provider → Verification → Action / ActionStep / AuditEvent → Response

Phase 4 already delivered honest REAL/MOCK providers, calculator `125 * 24` = `3000.00`, and 83 passing tests.

---

## 3. Existing agent problems

| Problem | Before Phase 5 |
|---|---|
| Inconsistent contracts | `canHandle(AgentContext)` + `AgentResult` success/approval/handoff only |
| Duplicate names | `AgentRegistry` silently overwrote |
| Tie order | Priority sort only; HashMap iteration order on ties |
| FinanceAgent | Hardcoded `68 + 140 + 50` = `$258` |
| EmailAgent | Invented `john@example.com` |
| TravelAgent | Called `search_web` + LLM and could imply a trip plan |
| EvaluationAgent | Invented 96.4% / 98.2% metrics |
| WorkflowAgent | Returned STUB success that could COMPLETE |
| Client routing | HTTP `agent` / `agentName` not stripped from tool params |
| Confidence | Previously invented scores existed in EvaluationAgent |

---

## 4. Agent contract

`Agent` remains the Spring bean interface. Added structured defaults without replacing existing methods:

- `capabilities()`
- `ownedTools()` (defaults to `getTools()`)
- `canHandle(AgentRequest)` — new routing path; legacy `canHandle(AgentContext)` remains compatibility
- `execute(AgentRequest)` — new execution path; default delegates to `execute(AgentContext)`
- `ownsTool(String)`
- `getPriority()` — **lower numeric value wins** (unchanged)

---

## 5. AgentRequest

`AgentRequest` carries only what an agent needs:

`userId`, `actionId`, `requestId`, `callId`, `conversationId`, `rawUserInput`, `requestedTool`, `parameters`, `locale`

Stripped on construction: `provider`, `providerName`, `providerClass`, `agent`, `agentName`, `jwt`, `authorization`, `password`, `otp`, `cardNumber`, `token`.

Never carries JWT, Vapi secrets, OTP, cards, or provider credentials. `toContext()` preserves compatibility.

---

## 6. AgentResult

`AgentResult` is the **agent** decision. `ActionStatus` remains the **action** lifecycle.

Outcomes: `EXECUTE`, `COMPLETED`, `NEEDS_INFORMATION`, `NEEDS_USER_CONFIRMATION`, `WAITING_USER`, `REJECTED`, `FAILED`, `CANCELLED`, `NOT_IMPLEMENTED`.

Additional fields: `selectedTool`, `missingFields`, `errorCode`. No numeric confidence field is populated.

ActionEngine mapping:

| AgentOutcome | ActionStatus |
|---|---|
| EXECUTE / COMPLETED (verified) | COMPLETED |
| NEEDS_INFORMATION / WAITING_USER | WAITING_FOR_USER |
| NEEDS_USER_CONFIRMATION | REQUIRES_APPROVAL |
| REJECTED | REJECTED |
| FAILED / NOT_IMPLEMENTED | FAILED |
| CANCELLED | CANCELLED |

Agents do not mutate final Action status; ActionEngine does.

---

## 7. Agent capabilities

Capabilities reflect this repository only:

`CALCULATOR`, `UTILITY`, `TRAVEL`, `EMAIL`, `CALENDAR`, `RESEARCH`, `TASKS`, `MEMORY`, `DOCUMENTS`, `GITHUB`, `SUPPORT`, `CONVERSATION`, `FINANCE`, `PLANNING`, `APPROVAL`, `NOTIFICATION`, `SECURITY`, `WORKFLOW`, `EVALUATION`, `DEVELOPMENT`

No SALES/BOOKING capabilities were added because those agents do not exist.

---

## 8. Tool ownership

Unchanged Phase 2 rule: a tool runs only when the selected agent owns it, PolicyEngine allows it, and ToolRegistry contains it.

`UtilityAgent` owns `calculator`, `probe_fail`, `forbidden`.  
`TravelAgent` owns `search_web` only (calculator ownership removed).  
`FinanceAgent` owns `calculator` but UtilityAgent wins calculator routing by priority 10 vs 40.

---

## 9. Agent selection

`RegistryAgentSelector` order (deterministic):

1. Requested tool ownership (ConversationAgent / OrchestratorAgent excluded)
2. `AgentRegistry.findHandler(AgentRequest)` — first `canHandle` after priority+name sort
3. ConversationAgent fallback

Client-supplied `agent` / `agentName` is ignored. POST `/api/v1/agent/execute` has no agent field and still goes through ActionEngine.

---

## 10. Priority

**Lower numeric value wins.** Existing values preserved (UtilityAgent 10, TravelAgent 20, ConversationAgent 999, etc.).

---

## 11. Deterministic tie-breaking

After priority: case-insensitive agent name.

Example: two agents at priority 50 → `AlphaAgent` before `BetaAgent`. Duplicate names fail fast at registry construction.

---

## 12. Policy separation

PolicyEngine still runs **before** `agent.execute`. High-risk / forbidden tools are denied or require approval without tool/provider execution.

Agents cannot approve their own high-risk actions and cannot invoke payment authorization.

---

## 13. Security

Preserved: JWT, VoiceSession binding, owner isolation, request correlation, secret redaction, Vapi webhook validation.

Identity is not derived from transcript text or agent/tool parameters. HTTP `agent`/`provider*` params are stripped.

---

## 14. Trace/audit integration

Existing `ActionTraceService` / `ActionStep` / `AuditEvent` remain canonical.

Added event types: `AGENT_STARTED`, `AGENT_SUCCEEDED`, `AGENT_FAILED` (plus existing `AGENT_SELECTED`).

`NEEDS_INFORMATION` does not emit `TOOL_STARTED` / `TOOL_SUCCEEDED` / provider events. PLANNING still covers agent selection; no new `ActionStepType` was added.

---

## 15. Frontend

ConsolePage was not redesigned. Last Action already showed agent/tool/status/result/timeline from backend data. It now also shows `missingFields` when the timeline/action payload includes them. No fake confidence. No mock execution rows.

---

## 16. Tests

Baseline 83 tests still pass. Added `AgentContractTest` (15) and four ExecutionPipeline E2E cases covering selection, priority/tie-break, needs-information, finance honesty, policy, isolation, client agent/provider hijack, and catalog (no confidence).

---

## 17. Build results

```
mvnw.cmd clean compile   BUILD SUCCESS  (2026-09-18T00:20:13+05:30)
mvnw.cmd test            BUILD SUCCESS  Tests run: 102, Failures: 0, Errors: 0, Skipped: 0
                         Finished at: 2026-09-18T00:30:05+05:30
mvnw.cmd package         BUILD SUCCESS  (2026-09-18T00:32:07+05:30)  jar: voiceos-backend-1.0.0-SNAPSHOT.jar
npm run build (frontend) SUCCESS        vite v8.2.1, 12.17s
```

JAVA_HOME=`C:\Program Files\Java\jdk-26.0.2`.

---

## 18. Known limitations

- SearchTool / GitHubTool / DocumentSearchTool are still not on the Phase 4 provider contract
- Memory/Task tools still use local stores
- LLM/Vapi voice providers remain separate; ConversationAgent still uses the existing `LLMProvider` for chitchat only
- CalendarAgent may still default a date to today for schedule checks
- Task/Research/Developer/Rag agents were given capabilities but not fully rewritten
- No SalesAgent / BookingAgent
- Live Vapi remains UNVERIFIED
- Product is not production-ready

---

## 19. Future AI integration boundary

A future model system should produce structured intent / tool selection / missing fields / plans and implement an `Agent` that returns `AgentResult`.

It must **not** replace ActionEngine, PolicyEngine, ToolRegistry, ProviderRegistry, Verification, or ActionTraceService.

Conceptually: Future AI → Agent implementation → AgentResult → ActionEngine → Policy → Tool → Provider.

No OpenAI/Gemini/Anthropic/local LLM/LangChain/RAG was added in this phase.

---

## 20. Deferred work

- Real Flight/Hotel/Email/WhatsApp/Calendar/Payment providers
- LLM routing, embeddings, RAG, Qdrant/Redis orchestration
- Multi-agent collaboration loops
- LiveKit
- Per-agent happy-path test class for every remaining domain agent
- Live Vapi verification

---

## 21. Next phase recommendation

**Phase 6 — Voice accuracy and multilingual** (existing roadmap), or a separately authorized travel-provider phase.

Do **not** start real Flight/Hotel/Email/WhatsApp/Calendar/Payment APIs until authorized. Do not introduce LiveKit. Do not add LLM orchestration in the next unreviewed step.
