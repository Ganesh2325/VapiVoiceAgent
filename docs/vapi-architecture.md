# VoiceOS — Vapi-First Architecture Specification

## 1. Executive Summary

VoiceOS is an enterprise-grade Multi-Agent Voice AI Operations Platform built on a **Vapi-First** architecture.

* **Vapi** handles all real-time voice infrastructure: telephony / WebRTC, speech recognition (STT), text-to-speech (TTS), assistant lifecycle, and voice streaming.
* **Spring Boot (Java 21 + Virtual Threads)** serves as the central control plane, agent orchestrator, business logic engine, tool registry, security barrier, and persistent memory/RAG layer.

---

## 2. Core Architecture Pipeline

```
┌──────────┐
│   USER   │ (Speaks naturally via Phone or WebRTC Browser UI)
└────┬─────┘
     │ Realtime Audio
     ▼
┌─────────────────────────────────────────────────────────────┐
│                       VAPI PLATFORM                         │
│  - Realtime WebRTC / SIP Gateway                            │
│  - Speech-to-Text (STT)                                     │
│  - Assistant System Prompt & Conversation State             │
│  - Tool / Function Invocation Detector                      │
│  - Text-to-Speech (TTS)                                     │
└──────────────┬───────────────────────────────▲──────────────┘
               │                               │
               │ POST /api/webhooks/vapi       │ Structured Tool Result
               │ (Tool-Calls, Status, EOR)     │ or Assistant Payload
               ▼                               │
┌──────────────────────────────────────────────┴──────────────┐
│                    SPRING BOOT BACKEND                      │
│                                                             │
│  ┌───────────────────────────────────────────────────────┐  │
│  │ VapiWebhookController                                 │  │
│  │   ├── Secret / Signature Verification (x-vapi-secret) │  │
│  │   └── Idempotent Webhook Logger                       │  │
│  └───────────────────────────┬───────────────────────────┘  │
│                              ▼                              │
│  ┌───────────────────────────────────────────────────────┐  │
│  │ VapiEventProcessor & VapiToolService                  │  │
│  └───────────────────────────┬───────────────────────────┘  │
│                              ▼                              │
│  ┌───────────────────────────────────────────────────────┐  │
│  │ OrchestratorService                                   │  │
│  │   ├── Intent Classification & Execution Planning      │  │
│  │   ├── Short-Term (Redis) & Long-Term Memory (PG)      │  │
│  │   └── Human-in-the-Loop Approval Interceptor          │  │
│  └───────┬───────────┬───────────┬───────────┬───────────┘  │
│          ▼           ▼           ▼           ▼              │
│    ┌──────────┐┌──────────┐┌──────────┐┌──────────┐         │
│    │TravelAgnt││ TaskAgnt ││ EmailAgnt││...Other  │         │
│    └────┬─────┘└────┬─────┘└────┬─────┘└────┬─────┘         │
│         ▼           ▼           ▼           ▼               │
│  ┌───────────────────────────────────────────────────────┐  │
│  │ ToolRegistry (9 Deterministic Tools)                  │  │
│  │ Calculator, Search, Tasks, Memory, Calendar, GitHub   │  │
│  └───────────────────────────────────────────────────────┘  │
│                              ▼                              │
│  ┌───────────────────────────────────────────────────────┐  │
│  │ Data & Knowledge Layer                                │  │
│  │ PostgreSQL 16  •  Redis 7  •  Qdrant Vector DB        │  │
│  └───────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────┘
```

---

## 3. Vapi Webhook Protocol Specification

### A. Endpoint
`POST /api/webhooks/vapi` (and alias `/api/v1/webhooks/vapi`)

### B. Headers
* `x-vapi-secret`: Shared secret configured in Vapi Dashboard (`VAPI_WEBHOOK_SECRET`) for webhook authentication.
* `Content-Type`: `application/json`

### C. Supported Event Types
1. **`tool-calls`**: Vapi assistant triggers one or more registered backend tools.
   - Handled by: `VapiToolService` → `ToolRegistry` / `AgentRegistry`.
   - Response: JSON containing `{ "results": [ { "toolCallId": "...", "result": "..." } ] }`.
2. **`assistant-request`**: Dynamic assistant configuration or prompt override per inbound call.
3. **`status-update`**: Call status changes (`ringing`, `in-progress`, `ended`).
4. **`end-of-call-report`**: Call summary, recording URL, cost, transcript, and duration for quality metrics.
5. **`speech-update`**: User speech started/stopped events for live waveform telemetry.
6. **`transcript`**: Real-time transcript chunks for live agent execution timeline streaming.

---

## 4. Multi-Agent Delegation Model

When Vapi triggers a tool call (e.g. `execute_agent_action` or specific tools like `create_task`, `plan_trip`, `send_email`), Spring Boot executes:

1. **Intent Matching**: The `OrchestratorService` identifies the domain.
2. **Risk Check**: If the action has **HIGH** or **CRITICAL** risk (e.g. `EmailTool`), it triggers Human-in-the-Loop approval and prompts the user for verbal confirmation.
3. **Execution**: The specialized agent executes the deterministic tool against PostgreSQL, Redis, or Qdrant.
4. **Result Packaging**: The structured result is returned to Vapi in < 500ms so the voice assistant immediately speaks the answer to the user.

---

## 5. Security & Idempotency

* **Secret Verification**: All incoming webhook payloads are validated against `voiceos.webhooks.vapi-secret`.
* **Idempotency**: Every Vapi event with a `call.id` or `message.id` is checked against `WebhookEvent` table to prevent double-execution of tools.
* **Audit Trail**: Every tool execution is recorded in `tool_executions` with execution time and parameters.
