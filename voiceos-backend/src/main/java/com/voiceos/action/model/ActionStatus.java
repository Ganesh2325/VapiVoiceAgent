package com.voiceos.action.model;

/**
 * Lifecycle states for any real-world action orchestrated by VoiceOS.
 *
 * <p>Canonical happy path:
 * RECEIVED → VALIDATED → AUTHORIZED → PLANNED → EXECUTING → VERIFYING → COMPLETED
 *
 * <p>Failure / pause paths: REJECTED, FAILED, CANCELLED, REQUIRES_APPROVAL.
 */
public enum ActionStatus {
    REQUESTED,
    RECEIVED,
    VALIDATED,
    AUTHORIZED,
    UNDERSTANDING,
    PLANNED,
    READY,
    WAITING_FOR_USER,
    REQUIRES_AUTHENTICATION,
    REQUIRES_PAYMENT,
    REQUIRES_APPROVAL,
    AUTHENTICATING,
    EXECUTING,
    VERIFYING,
    COMPLETED,
    FAILED,
    REJECTED,
    CANCELLED,
    EXPIRED,
    REQUIRES_CLARIFICATION
}
