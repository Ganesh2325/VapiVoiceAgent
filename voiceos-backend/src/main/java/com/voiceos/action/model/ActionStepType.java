package com.voiceos.action.model;

/**
 * Meaningful stages of the canonical ActionEngine pipeline.
 * Not every internal line of code is a step — only reconstructable phases.
 */
public enum ActionStepType {
    RECEIVED,
    VALIDATING,
    AUTHORIZING,
    PLANNING,
    POLICY_CHECK,
    TOOL_EXECUTION,
    VERIFICATION,
    APPROVAL_WAIT,
    COMPLETION
}
