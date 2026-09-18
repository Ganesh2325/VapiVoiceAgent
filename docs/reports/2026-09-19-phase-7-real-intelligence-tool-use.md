# VoiceOS Phase 7 — Real Intelligence + Safe Tool Calling

**Date:** 2026-09-19  
**Status:** DONE (code + tests). Real Gemini/Groq generation in this run = **UNVERIFIED**. Live human Vapi microphone loop remains **PARTIAL / UNVERIFIED**.  
**Evidence standard:** Structured model output is untrusted. Tools run only through ActionEngine → PolicyEngine → ToolRegistry → Provider → Verification. MOCK is labeled MOCK. Missing capability is UNAVAILABLE.

---

## 1. Objective

Turn Phase 6 universal routing into a usable general-intelligence path: a real LLM when configured, structured tool requests, a bounded server-side tool loop, and honest failure. This does not mean VoiceOS can do everything.

## 2. Starting architecture

Phase 6: deterministic selector, GeneralQueryAgent fallback, LLMProvider (tests/dev MOCK), no GeneralQueryAgent tool execution. Calculator REAL via UtilityAgent. ActionEngine already coordinated specialized tool execution.

## 3. LLM architecture

Unchanged abstraction: `LLMProvider` / `LLMRequest` / `LLMResponse`.

| Mode | When |
|---|---|
| MOCK | `voiceos.ai.mock=true` or `provider=mock` |
| REAL | mock=false and a Spring AI ChatModel exists (Gemini or Groq wrapper) |
| UNAVAILABLE | mock=false and no ChatModel / unknown provider |

No silent REAL → MOCK. `GeminiLLMProvider` / `GroqLLMProvider` `isAvailable()` no longer pings the network on every request.

## 4. Real / Mock / Unavailable behavior

Local `.env` in this workspace (values not printed):

| Item | Status |
|---|---|
| AI_PROVIDER | SET |
| GEMINI_API_KEY | MISSING |
| GROQ_API_KEY | SET |
| MOCK_EXTERNAL_APIS | SET |
| VOICEOS_REAL_LLM_TEST | MISSING |

Normal tests used MockLLMProvider. Optional `RealLlmIntegrationTest` is skipped unless `VOICEOS_REAL_LLM_TEST=true`. **REAL LLM EXECUTED: no. MOCK LLM EXECUTED: yes.**

## 5. GeneralQueryAgent

Still the universal fallback. It now:

- answers via structured `ModelDecision` JSON
- may **request** server-allowlisted tools (`calculator` by default)
- does not own tools for routing and does not call providers
- continues after ActionEngine returns a real `ToolResult`
- still rejects client `requestedTool` injection

## 6. Hybrid routing

1. Explicit requested tool ownership (fallback agents excluded)
2. Deterministic specialized `canHandle`
3. `IntentClassifier` / `KeywordIntentClassifier` labels intent only (not agent selection)
4. GeneralQueryAgent

Specialized agents still win. “Calculate 125 multiplied by 24” remains UtilityAgent. “What is the product of 125 and 24?” is GeneralQueryAgent + calculator tool loop.

## 7. Model output contract

`ModelDecision`: outcome `ANSWER|CLARIFY|TOOL|UNAVAILABLE`, answer, intent, toolName, arguments, missingFields.

Prose such as “I think we should use weather…” is not a tool call.

## 8. Tool selection

The model may only name allowlisted tools. Unknown names fail closed. `provider`, `providerClass`, URL, jwt, agent, SQL, and similar keys are stripped server-side (`ToolArgumentSanitizer`). Tool names must match `[a-z][a-z0-9_]*`.

## 9. Tool security

GeneralQueryAgent default allowlist: `calculator` (LOW). Not exposed: `send_email`, `forbidden`, payment, booking, delete. High-risk tools still hit PolicyEngine if requested. No arbitrary class loading, HTTP, or shell from model output.

## 10. Policy integration

When the model requests a tool, ActionEngine evaluates **that tool name** with the tool’s declared risk. `send_email` → REQUIRE_APPROVAL. `forbidden` → DENY. Conversation-only first pass remains the existing policy on the planned tool (often none).

## 11. Tool loop

Configurable `voiceos.agents.max-tool-calls-per-action` (default 2).

LLM → structured ToolCall → PolicyEngine → ToolRegistry → Provider → Verification → LLM continuation → final answer.

If the limit is reached: FAILED, honest message. No agent spawning.

## 12. Failure handling

| Case | Result |
|---|---|
| LLM unavailable / throws | UNAVAILABLE / FAILED, no mock fallback |
| Unknown tool | FAILED, no provider |
| Policy deny | REJECTED, tool not executed |
| High-risk send_email | REQUIRES_APPROVAL, not sent |
| Tool/provider failure | FAILED after continuation; not COMPLETED |
| Live weather without provider | UNAVAILABLE (unchanged) |

## 13. Prompt-injection handling

Server-side sanitizer and allowlist, not prompt wording alone. Injected `providerClass` / jwt / URL do not change `RealLocalCalculatorProvider`.

## 14. Vapi integration

Unchanged. Vapi is voice I/O. VoiceOS remains ActionEngine intelligence. `voiceos_request` still routes through AgentSelector.

## 15. Frontend

Console Last Action still shows agent, intent, tool, provider, mode, status, result, timeline. Suggested prompts include a general question and a GeneralQueryAgent calculator phrasing. REAL is shown only when the backend reports REAL.

## 16. Tests

Before: 128. After: **142 run, 0 failures, 1 skipped** (optional real LLM).

Coverage: specialized vs general calculator routes, tool loop 3000.00 REAL, unknown tool, policy deny, high-risk approval, probe_fail, prompt injection, malformed args, sanitizer, client requestedTool rejection, max loop, E2E product-of calculator.

## 17. Real LLM verification

**UNVERIFIED** in this run. `RealLlmIntegrationTest` skipped (`VOICEOS_REAL_LLM_TEST` missing). Do not treat MockLLM tests as AI verification.

## 18. Live human Vapi status

**PARTIAL / UNVERIFIED.** No new spoken microphone confirmation.

## 19. Limitations

- Default local/dev LLM is still MOCK.
- Real Groq/Gemini path exists but was not executed here.
- GeneralQueryAgent may request calculator only (unless a test allowlist is passed).
- No weather/web/news providers.
- Specialized routing is still English `contains()`.
- No multi-agent loops, RAG, real email/travel/WhatsApp/calendar/payment.

## 20. Next product milestone

Do not auto-start. Later authorized work may include live human Vapi, real Email/Travel/WhatsApp/Calendar, Tasks, Memory, RAG, or multilingual support.

---

## Build evidence (2026-09-19)

```
mvnw.cmd clean compile   BUILD SUCCESS
mvnw.cmd test            BUILD SUCCESS  Tests run: 142, Failures: 0, Errors: 0, Skipped: 1
mvnw.cmd package         BUILD SUCCESS  jar: voiceos-backend-1.0.0-SNAPSHOT.jar
npm run build            SUCCESS        vite v8.2.1, 4.25s
```
