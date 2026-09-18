# VoiceOS Phase 6 — Universal Query Routing + GeneralQueryAgent

**Date:** 2026-09-18  
**Status:** DONE (code + tests). Live human Vapi microphone loop remains **PARTIAL / UNVERIFIED**.  
**Evidence standard:** Implemented and tested. A generated sentence is not execution evidence. MOCK is labeled MOCK. Missing capability is UNAVAILABLE, not a fabricated answer.

---

## 1. Objective

Every user utterance must enter a real VoiceOS agent path. Specialized requests stay on specialized agents. General questions, explanations, and writing requests that do not match a specialized domain go to `GeneralQueryAgent`, which uses the existing `LLMProvider` abstraction. There is no hardcoded Q&A dictionary and no claim that every question can always be answered correctly.

## 2. Why universal routing was required

Phase 5 routing was deterministic but incomplete for natural speech: unmatched utterances had no honest general-intelligence path, and a keyword chatbot would have been a fake product. Users should not need agent, tool, provider, or API names. VoiceOS must choose the agent.

## 3. Existing agent routing

Selection remains deterministic in `RegistryAgentSelector`:

1. Requested tool ownership (fallback agents excluded)
2. Specialized `canHandle` via `AgentRegistry.findHandler` (priority then name; `GeneralQueryAgent` excluded)
3. `GeneralQueryAgent` fallback
4. If no general agent is registered: honest failure (`No agent available`)

Client-supplied `agent` / `agentName` / `provider` parameters are stripped in `AgentRequest` and ignored.

## 4. GeneralQueryAgent design

`ConversationAgent` was replaced, not duplicated. `GeneralQueryAgent` is the single universal fallback (`priority=999`, `canHandle=true`, capabilities `GENERAL_QUERY` + `CONVERSATION`).

It supports:

- general questions, explanations, writing, conversation, clarification
- optional last-8 conversation messages from existing `MessageRepository` (same user conversation only)
- structured `AgentResult` (`outcome`, answer, missing fields, LLM metadata, error code)

It does **not**:

- execute domain tools
- honor model-selected provider/agent/URL
- invent live weather/news/prices
- claim email/booking/payment/calendar side effects

Intent is stored separately from `rawUserInput` (`GENERAL_QUESTION` / `EXPLANATION` / `WRITING` / `REALTIME` / `UNKNOWN`). Raw input is not rewritten.

## 5. LLMProvider boundary

Existing contract reused: `LLMProvider` → `LLMRequest` / `LLMResponse`.

| Bean | When | Mode |
|---|---|---|
| `MockLLMProvider` | `voiceos.ai.mock=true` or `provider=mock` | MOCK, labeled `[MOCK DATA]` |
| `GeminiLLMProvider` / `GroqLLMProvider` | mock=false and a Spring AI `ChatModel` exists | REAL |
| `UnavailableLLMProvider` | mock=false and no ChatModel / unknown provider | UNAVAILABLE (no silent MOCK fallback) |

API keys stay in environment / Spring config. Agents do not hold credentials. Vapi is voice only.

**Verified in this phase's test environment:** MOCK (`application-test.yml` sets `voiceos.ai.mock=true`). Real Gemini/Groq was not executed in CI tests.

## 6. Specialized vs general routing

Verified examples:

| Utterance | Agent |
|---|---|
| Calculate 125 multiplied by 24 | UtilityAgent |
| Book me a flight / Book me a flight from Delhi to London | TravelAgent |
| Send that email to my professor | EmailAgent |
| Write a professional email asking my professor for an extension | GeneralQueryAgent |
| What is dependency injection in Spring Boot? | GeneralQueryAgent |
| What is the weather in London right now? | GeneralQueryAgent (honest UNAVAILABLE) |

Email drafting does not match `EmailAgent` (`write`/`draft`/`compose` without `send`). SupportAgent only matches VoiceOS-help phrases so it does not steal general questions.

## 7. Clarification behavior

- Travel: missing origin / destination / travelDate → `NEEDS_INFORMATION`, no provider execution
- Email: missing recipient/subject/body → `NEEDS_INFORMATION` (or policy `REQUIRES_APPROVAL` if `send_email` is already selected as the owned high-risk tool)
- GeneralQueryAgent: blank input or ambiguous “What’s the best one?” without history → `NEEDS_INFORMATION` (`utterance` / `referent`)

## 8. Tool-use behavior

GeneralQueryAgent owns no tools. A requested tool on that agent returns `UNSUPPORTED_REQUEST`. LLM output cannot set `providerClass`, HTTP endpoints, or agent names. `ActionEngine` ignores `selectedTool` unless the agent owns it and it matches the policy-checked tool. Domain operations still go ActionEngine → PolicyEngine → ToolRegistry → Provider → Verification.

## 9. Security

- JWT identity required for HTTP execute
- User A cannot read User B action / timeline / model metadata
- Agent/provider injection parameters stripped
- PolicyEngine still gates high-risk tools (`send_email` → REQUIRE_APPROVAL)
- No invented confidence scores
- Secrets are not stored in action payload or LLM metadata

## 10. Failure handling

| Condition | Outcome |
|---|---|
| LLM not configured / `isAvailable=false` | `UNAVAILABLE` → Action `FAILED`, honest “reasoning service is unavailable” |
| LLM throws | `FAILED`, same honest voice text (not “I couldn’t understand you”) |
| Empty model content | `FAILED` |
| Real-time weather/news/market with no provider | `UNAVAILABLE`, no fabricated facts |
| REAL LLM missing ChatModel | `UnavailableLLMProvider`, no silent MOCK |

## 11. Vapi integration

Vapi is unchanged as the voice layer. Assistant tools: `calculator` plus `voiceos_request` (utterance). `voiceos_request` / `general_query` / `answer_question` map to `requestedTool=null` so AgentSelector can choose. Calculator tool-calls still bind UtilityAgent.

Verified Vapi-like E2E (simulated webhook, not a human microphone):

VoiceSession → ActionEngine → GeneralQueryAgent → MockLLMProvider → COMPLETED timeline.

Live bound calculator webhook from Track A remains: UtilityAgent → RealLocalCalculatorProvider → `3000.00`.

**LIVE HUMAN MICROPHONE LOOP = PARTIAL / UNVERIFIED.** Backend tool-call evidence is not a spoken loop.

## 12. Tests

Before this phase’s local baseline (after Track A): 106 tests.  
After Phase 6: **128 tests, 0 failures, 0 errors, 0 skipped.**

Added coverage includes routing, mock labeling, unavailable LLM, LLM failure, tool/provider/agent injection rejection, clarification, weather UNAVAILABLE, calculator `3000.00` REAL, general-question E2E, Vapi-like `voiceos_request`, user isolation, email-send path, malformed calculator arguments, catalog without confidence.

## 13. Real vs Mock vs Stub

| Path | Classification |
|---|---|
| Calculator `125 * 24` = `3000.00` | REAL (`RealLocalCalculatorProvider`) |
| General-question answers in tests/dev default | MOCK (`MockLLMProvider`, `[MOCK DATA]`) |
| Gemini/Groq when ChatModel + keys present | REAL (not verified in this test run) |
| Travel / Email / WhatsApp / Calendar / Payment providers | MOCK or NOT_IMPLEMENTED (unchanged Phase 5 honesty) |
| Weather / live news / live stocks | UNAVAILABLE (no fake provider added) |
| Live human Vapi STT→TTS loop | UNVERIFIED |

## 14. Limitations

- Default `voiceos.ai.mock` follows `MOCK_EXTERNAL_APIS` (defaults true). Local/dev answers are MOCK unless mock is disabled and a real ChatModel is configured.
- GeneralQueryAgent does not execute tools even if a future model recommends one; that remains a later wiring task through ActionEngine/PolicyEngine.
- Conversation context is existing messages only. No new long-term memory, Qdrant, or Redis orchestration.
- No multi-agent loops.
- Real-time web/weather/news tools were not built.
- Support/Research/Developer/Task/Memory still use English `contains()` routing.
- VoiceOS cannot answer everything. It routes, uses a model or tool when available, and otherwise says so.

## 15. Live voice status

**LIVE HUMAN VAPI LOOP = PARTIAL / UNVERIFIED**

Track A bound public webhook calculator remains REAL `3000.00`. No new human microphone confirmation was performed in Phase 6.

## 16. Next product milestone

Do not auto-start. Separately authorized later work may include: live human Vapi microphone pass, real Email/Travel/WhatsApp/Calendar, Tasks, Memory, RAG/Qdrant, multilingual production support, or multi-agent collaboration.

---

## Build evidence (2026-09-18)

```
mvnw.cmd clean compile   BUILD SUCCESS
mvnw.cmd test            BUILD SUCCESS  Tests run: 128, Failures: 0, Errors: 0, Skipped: 0
mvnw.cmd package         BUILD SUCCESS  jar: voiceos-backend-1.0.0-SNAPSHOT.jar
npm run build            SUCCESS        vite v8.2.1, 3.45s
```
