package com.voiceos.agent.core;

/**
 * Agent decision/result. Distinct from {@code ActionStatus}, which is the Action lifecycle.
 */
public enum AgentOutcome {
    EXECUTE,
    COMPLETED,
    NEEDS_INFORMATION,
    NEEDS_USER_CONFIRMATION,
    WAITING_USER,
    REJECTED,
    FAILED,
    CANCELLED,
    NOT_IMPLEMENTED
}
