# Live Vapi Verification — 2026-09-18

**Status:** `PARTIAL`  
**Product milestone:** Track A — Item 1 (live calculator voice loop)  
**Evidence standard:** Actual microphone → Vapi → public webhook → VoiceOS → spoken result. Unit/MockMvc tests are not live evidence. A spoken `3000` was not heard.

---

## 1. Objective

Prove one real voice interaction:

User speaks “Calculate 125 multiplied by 24.”  
→ Vapi transcribes and calls VoiceOS  
→ webhook secret validated  
→ VoiceSession bound to the JWT user  
→ ActionEngine → PolicyEngine → UtilityAgent → calculator → RealLocalCalculatorProvider  
→ result `3000` spoken back.

---

## 2. Environment

Values are not printed.

| Item | State |
|---|---|
| PostgreSQL 16 (`voiceos-postgres` :5433) | SET / running |
| Redis | running (unused by this pipeline) |
| Qdrant | running (unused by this pipeline) |
| Local backend :8080 | SET / running |
| Frontend Vite :5173 | SET / running |
| Public HTTPS tunnel (`ngrok` → :8080) | SET (host matches `VAPI_SERVER_URL`) |
| JWT_SECRET | SET |
| DATABASE_* | SET |
| VAPI_WEBHOOK_SECRET | SET |
| VAPI_API_KEY | SET (len 36) |
| VAPI_ASSISTANT_ID | SET (len 36) |
| VAPI_PUBLIC_KEY | SET (len 36) |
| VAPI_SERVER_URL | SET (`https`, webhook path `/api/webhooks/vapi`) |
| ngrok authtoken (local ngrok.yml) | SET |
| Browser microphone in automated session | MISSING (Daily send transport disconnects; calls end without user speech) |

Live Vapi `x-vapi-secret` did not initially match `.env`. VoiceOS `.env` was aligned to the header Vapi actually sends (gitignored; value not printed). After restart, public webhooks returned HTTP 200 with exact secret match.

---

## 3. Vapi configuration

- Webhook: `POST /api/webhooks/vapi` (alias `/api/v1/webhooks/vapi`)
- Secret: `x-vapi-secret` (fail-closed)
- Assistant GET HTTP 200: `gpt-4o`, one `function:calculator` tool, assistant server URL matches env host
- `patch_vapi.py` PATCH HTTP 200: calculator tool + tool-level server URL/secret
- `GET /api/v1/voice/config` after login: `configured=true`; JSON does not include `apiKey` or `webhookSecret`

---

## 4. VoiceSession identity flow (live-proven bind)

1. User logs in (JWT).
2. Console loads `GET /api/v1/voice/config` (public key + assistant id only).
3. `vapi.start(assistantId)` returns a real call id.
4. Frontend `POST /api/v1/voice/sessions` binds that call id to the JWT user.

Live bind evidence (latest successful call):

- `callId=01a0b5a1-cd35-7555-986d-7208d7cf08d7`
- `sessionId=e25957ad-0b2c-4805-b009-9da11a74d1ca`
- `userId=4e833504-3d47-4292-a305-3af420c691d8`
- Console status: `Live Vapi Call Connected (Listening...)`
- Public webhooks accepted: `assistant.started`, `status-update in-progress`, `speech-update`, `conversation-update`, `end-of-call-report` (duration 37.9s)

Not used as authentication: `customer.email`, phone, transcript, or a client-supplied userId.

---

## 5. Webhook flow

`VapiWebhookController` → secret verify → parse payload → idempotency claim → `VapiToolService` → `ActionEngine`.

Live `toolCallList` shape is deserialized onto `VapiToolCall`. Empty `toolCalls: []` no longer hides `toolCallList`.

Frontend `@vapi-ai/web` CJS default export is resolved (`Vapi is not a constructor` on Vite 8 is fixed).

---

## 6. Actual spoken request

Not performed. Automated browser cannot grant a working microphone. Live calls connect, then end on silence / customer-ended-call with no user STT and no Vapi-originated `tool-calls` event.

An earlier connected call where a control `add-message` succeeded produced assistant speech that **invented** `3000` after tool access failed (webhooks were still 401). That invented number is **not** ActionEngine evidence.

---

## 7. Actual result (bound public HTTPS calculator)

After secret alignment, a live-shaped `tool-calls` POST was sent to the **public** `VAPI_SERVER_URL` using the bound call id from the real WebRTC session.

| Field | Value |
|---|---|
| HTTP | 200 |
| `userBound` | true |
| `callId` | `01a0b5a1-cd35-7555-986d-7208d7cf08d7` |
| `toolCallId` | `tool-bound-live-1` |
| agent | UtilityAgent |
| tool | calculator |
| provider | RealLocalCalculatorProvider |
| mode | REAL |
| status | COMPLETED |
| result | `3000.00` |
| `actionId` | `5ed02a7d-7457-4729-9c9d-e5c7cb4e711e` |
| durationMs | 2177 |

This proves the bound public webhook pipeline. It does **not** prove microphone STT or spoken TTS.

---

## 8–10. Action / ActionStep / AuditEvent evidence

Action `5ed02a7d-7457-4729-9c9d-e5c7cb4e711e` completed through ActionEngine as above. This is a public-HTTPS bound tool-call, not a spoken turn.

---

## 11. Idempotency test

Replay of the same public `tool-calls` body (`toolCallId=tool-bound-live-1`) returned HTTP 200. Backend: `Duplicate Vapi webhook ignored eventType=tool-calls callId=01a0b5a1-...`.

---

## 12. Unbound session test

Public HTTPS POST with `callId=call-unbound-live-1` (correct secret) routed calculator with `userBound=false` and ActionEngine `REJECTED`. Missing/wrong `x-vapi-secret` still returns 401.

---

## 13. User-isolation test

Covered by `ExecutionPipelineE2ETest.userCannotReadAnotherUsersAction` (non-owner 404). Not a live two-user voice test.

---

## 14. Failure test

Covered by `probe_fail` through ActionEngine: FAILED, not COMPLETED. Label: REAL pipeline + MOCK failure provider. Not spoken through Vapi.

---

## 15. Problems encountered

1. Vite 8: `@vapi-ai/web` default export was not a constructor (`TypeError: Vapi is not a constructor`).
2. Live Vapi `x-vapi-secret` did not match the original `.env` webhook secret (all public events 401 until aligned).
3. Automated browser has no usable microphone; Daily send transport disconnects; `silence-timed-out` / `customer-ended-call`.
4. Vapi REST `/call/{id}/control` is 404; live `controlUrl` POST often races to `Call Not Active`.
5. Assistant can invent `3000` if the calculator tool webhook is 401.

---

## 16. Root causes

Vapi server tools require the public webhook secret to match VoiceOS exactly. Automated Cursor browser cannot complete a microphone utterance. Vite CJS interop broke the Web SDK constructor.

---

## 17. Fixes (necessary live-path only)

- Resolve `@vapi-ai/web` constructor under Vite 8
- Bind `call.id` from `vapi.start()` then `POST /api/v1/voice/sessions`
- `patch_vapi.py` loads `.env`, patches calculator tool **and** tool-level server URL/secret
- Align gitignored `VAPI_WEBHOOK_SECRET` to the secret Vapi actually sends (not committed)

Security was not weakened: webhook secret still required; JWT APIs still authenticated; voice config does not leak `apiKey` / webhook secret; no email-as-auth.

---

## 18. Build results (from this item’s contract work)

```
mvnw.cmd test      BUILD SUCCESS  Tests run: 106, Failures: 0, Errors: 0, Skipped: 0
mvnw.cmd package   BUILD SUCCESS  voiceos-backend-1.0.0-SNAPSHOT.jar
npm run build      SUCCESS        vite v8.2.1
```

---

## 19. Live verification status

`PARTIAL`

Checklist:

- [x] PostgreSQL running
- [x] Required JWT/DB env present
- [x] Required Vapi env present
- [x] Public HTTPS endpoint available
- [x] Vapi server URL configured
- [x] Calculator tool configured on a live assistant
- [x] Actual Vapi WebRTC call placed and VoiceSession bound
- [ ] User actually spoke
- [ ] Vapi transcribed / invoked calculator from speech
- [x] Backend received actual Vapi webhooks on the public URL (status/speech/end-of-call)
- [x] Bound public tool-call executed calculator REAL `3000.00`
- [ ] Spoken result heard

---

## 20. Remaining limitations

- Track A Item 1 is not PASS until a human hears `3000`.
- Dev profile still uses MockLLMProvider; calculator itself is REAL local arithmetic.
- Travel / email / WhatsApp / calendar / payment remain MOCK.
- Keep `.env` webhook secret identical to the Vapi dashboard/assistant secret; do not commit `.env` or tunnel URLs.

---

## 21. Next product milestone

Remain on Track A Item 1 until a real human hears `3000` from Vapi.

On the running local app (`http://localhost:5173/`): log in, click **Start Vapi Voice Call**, allow the microphone, speak: “Calculate 125 multiplied by 24.”

Do not start Tasks, multilingual routing, Travel, Email, WhatsApp, Calendar, Payment, RAG, or LLM orchestration until this live loop PASSes.
