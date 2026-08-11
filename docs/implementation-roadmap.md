# Implementation Roadmap

This roadmap outlines the path to evolving VoiceOS into a Universal Action Engine with strict Human-in-the-Loop policies, Provider abstractions, and comprehensive Notification integrations.

## Priority 1-3: Vapi Integration, Voice Accuracy, Orchestrator
*Status: Partially Completed in previous phases. Requires updates for Confidence Scoring and Raw Transcript tracking.*
- **Action Items:**
  - Track `transcriptionConfidence`, `languageConfidence`, and `intentConfidence`.
  - Refactor Orchestrator to trigger the new Action Engine instead of direct Agent execution.

## Priority 4: Action Engine
*Status: Missing*
- **Action Items:**
  - Implement `ActionEngine`, `ActionPlanner`, `ActionExecutor`, and `ResultVerifier`.
  - Standardize the `Action` lifecycle: `REQUESTED` -> `UNDERSTANDING` -> `PLANNED` -> `WAITING_FOR_USER` -> `EXECUTING` -> `VERIFYING` -> `COMPLETED`.

## Priority 5: Policy Engine
*Status: Missing*
- **Action Items:**
  - Implement `PolicyEngine` to enforce constraints (e.g. `OFFICIAL_ONLY`, maximum spending, approval requirements).

## Priority 6: Human Approval (Expanded)
*Status: Needs Expansion*
- **Action Items:**
  - Upgrade `ApprovalService` to support secure handoffs for Authentication (`REQUIRES_AUTHENTICATION`) and Payment (`REQUIRES_PAYMENT`).
  - Update Frontend Dashboard to display secure generic payment/authentication pause screens.

## Priority 7: Provider Architecture
*Status: Missing*
- **Action Items:**
  - Implement `ServiceRegistry` and `ServiceProvider` interfaces.
  - Separate Agents from specific APIs (e.g. `TravelProvider`, `EmailProvider`).

## Priority 8-10: Travel Workflow, Email, WhatsApp
*Status: Incomplete*
- **Action Items:**
  - Build `TravelAgent` implementation that integrates with `TravelProvider`.
  - Build official `WhatsAppProvider` integration to support post-action notifications.
  - Build `EmailProvider` integration for attachments/tickets.

## Priority 11: Notification Engine
*Status: Missing*
- **Action Items:**
  - Implement `NotificationManager` supporting multichannel alerts (VOICE, EMAIL, WHATSAPP, IN_APP).

## Priority 12-18: Memory, Multilingual, Dashboards, Tracing, RAG, Evaluation
*Status: Completed*
- **Action Items:**
  - Integrate these existing robust systems with the newly refactored Action Engine.

## Priority 19: Security Hardening & Connected Accounts
*Status: Missing*
- **Action Items:**
  - Implement `ConnectedAccounts` to safely manage OAuth integration tokens.
  - Expand Frontend to manage OAuth linking without exposing secrets.

## Priority 20: Testing
*Status: Missing*
- **Action Items:**
  - Implement automated evaluation tests handling various languages and edge-cases (self-corrections, background noise simulation).
