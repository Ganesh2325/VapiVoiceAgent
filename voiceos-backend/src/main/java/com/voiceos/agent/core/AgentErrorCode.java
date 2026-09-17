package com.voiceos.agent.core;

/**
 * Safe agent error categories. No stack traces, no secrets.
 */
public enum AgentErrorCode {
    UNSUPPORTED_REQUEST,
    MISSING_INFORMATION,
    NO_AGENT_AVAILABLE,
    AGENT_EXECUTION_FAILED,
    CANCELLED,
    REJECTED,
    NOT_IMPLEMENTED
}
