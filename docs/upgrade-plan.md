# VoiceOS Upgrade Plan

This document outlines the phased implementation plan to transform VoiceOS into a Multilingual AI Voice Operating System, as per the product vision.

## Phase 1: Codebase Audit & Preparation
- **Completed:** Repository inspection, agent/tool mapping, and identification of Vapi integration points.
- **Next:** Deprecate and remove any LiveKit logic to ensure Vapi is the exclusive voice provider.

## Phase 2: Fix Vapi Integration (Current Issue)
- **Goal:** Resolve the `"header name must be a non-empty"` webhook error.
- **Action:** Inspect `VapiWebhookController` and `VapiToolService` responses to ensure Spring Boot is returning standard JSON without null headers. Verify ngrok interaction.
- **Action:** Ensure `VAPI_API_KEY` is exclusively managed server-side and not exposed to the frontend.

## Phase 3: Transcription Accuracy & Entity Protection
- **Goal:** Ensure accurate entity capture (names, dates, emails, locations).
- **Action:** Configure Vapi transcriber settings (e.g., deepgram) for high accuracy.
- **Action:** Update agent prompts to request explicit clarification when confidence on critical entities is low.
- **Action:** Implement structured tool schema constraints.

## Phase 4: Multilingual Architecture
- **Goal:** Support dynamic language switching and detection.
- **Action:** Create `LanguageService`, `LanguageConfiguration`, and `LanguagePreferenceService`.
- **Action:** Configure Vapi's language auto-detection and appropriate TTS voices for Hindi, Telugu, Tamil, Kannada, Malayalam, Bengali, Marathi.

## Phase 5: Upgrade Orchestrator
- **Goal:** Build the central brain for multi-step execution.
- **Action:** Implement `OrchestratorAgent` to delegate tasks to specialized agents (e.g., Travel -> Finance -> Calendar).
- **Action:** Implement `PlannerAgent` for task breakdown.

## Phase 6: Upgrade Existing & Add New Agents
- **Goal:** Refine responsibilities of the 11 existing agents.
- **Action:** Add `ApprovalAgent`, `NotificationAgent`, `WorkflowAgent`, `SecurityAgent`.
- **Action:** Ensure agents return structured actions and maintain strict boundaries.

## Phase 7: Upgrade Tool System
- **Goal:** Secure, predictable tool execution.
- **Action:** Enforce strict input/output schemas.
- **Action:** Implement robust error handling, retries, and timeouts.

## Phase 8: Implement Memory
- **Goal:** Persistent context across sessions.
- **Action:** Finalize PostgreSQL schema for long-term facts/preferences.
- **Action:** Use Redis for short-term session state.

## Phase 9: Implement Human Approvals (Human-in-the-Loop)
- **Goal:** Prevent unauthorized high-risk actions (e.g., sending emails, financial transactions).
- **Action:** Implement `ApprovalService` in the backend.
- **Action:** Build UI components to accept/reject pending actions.

## Phase 10: Execution Tracing & Observability
- **Goal:** Real-time visibility into agent decisions.
- **Action:** Implement structured logging (timestamps, tool calls, latency).
- **Action:** Expose an Execution Timeline API for the frontend.

## Phase 11: Upgrade React AI Operations Center
- **Goal:** Transform the monolithic `App.jsx` into a modular dashboard.
- **Action:** Implement routing (React Router) with pages: Dashboard, Voice, Agents, Tasks, Conversations, Memory, Documents, Workflows, Approvals, Executions, Analytics, Settings.
- **Action:** Build the "Voice Page" showing live transcription, current agent, and execution status.

## Phase 12: Implement RAG & Document Intelligence
- **Goal:** Allow agents to search uploaded documents.
- **Action:** Build document ingestion pipeline (Upload -> Extract -> Chunk -> Embed -> Qdrant).
- **Action:** Connect `ResearchAgent` to the document retrieval tool.

## Phase 13: Implement Workflows
- **Goal:** Scheduled, multi-step automated tasks.
- **Action:** Create scheduling infrastructure (Quartz/Spring Scheduler).
- **Action:** Build visual workflow display in the frontend.

## Phase 14: Evaluation & Testing
- **Goal:** Ensure high accuracy and low hallucination rates.
- **Action:** Implement `EvaluationAgent` with deterministic metrics.
- **Action:** Create test scenarios covering edge cases, multilingual input, and entity protection.

## Phase 15: Production Hardening
- **Goal:** Final polish for deployment.
- **Action:** Complete API documentation (Swagger/OpenAPI).
- **Action:** Security audit (rate limiting, webhook validation).
