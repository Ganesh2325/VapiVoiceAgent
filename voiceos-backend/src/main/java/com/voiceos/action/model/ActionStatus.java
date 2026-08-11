package com.voiceos.action.model;

/**
 * Lifecycle states for any real-world action orchestrated by VoiceOS.
 */
public enum ActionStatus {
    REQUESTED,
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
    CANCELLED,
    EXPIRED,
    REQUIRES_CLARIFICATION
}
