package com.voiceos.agent.core;

import com.voiceos.tool.core.ToolResult;

import java.util.List;
import java.util.Map;

/**
 * Structured result produced by an Agent. {@link AgentOutcome} is the agent decision;
 * ActionEngine maps it onto {@code ActionStatus}.
 *
 * <p>No confidence field is populated. Invented scores are forbidden.
 */
public record AgentResult(
        String agentName,
        String responseText,
        List<ToolResult> toolExecutions,
        boolean requiresApproval,
        ApprovalRequestData approvalAction,
        String nextAgentName,
        boolean success,
        Map<String, Object> stateUpdates,
        long latencyMs,
        AgentOutcome outcome,
        String selectedTool,
        List<String> missingFields,
        String errorCode
) {

    public AgentResult {
        if (toolExecutions == null) {
            toolExecutions = List.of();
        }
        if (stateUpdates == null) {
            stateUpdates = Map.of();
        }
        if (missingFields == null) {
            missingFields = List.of();
        }
        if (outcome == null) {
            if (requiresApproval) {
                outcome = AgentOutcome.NEEDS_USER_CONFIRMATION;
            } else if (!success) {
                outcome = AgentOutcome.FAILED;
            } else if (!toolExecutions.isEmpty()) {
                outcome = AgentOutcome.EXECUTE;
            } else {
                outcome = AgentOutcome.COMPLETED;
            }
        }
    }

    public AgentResult(
            String agentName,
            String responseText,
            List<ToolResult> toolExecutions,
            boolean requiresApproval,
            ApprovalRequestData approvalAction,
            String nextAgentName,
            boolean success,
            Map<String, Object> stateUpdates,
            long latencyMs
    ) {
        this(agentName, responseText, toolExecutions, requiresApproval, approvalAction, nextAgentName,
                success, stateUpdates, latencyMs, null, null, List.of(), null);
    }

    public static AgentResult success(String agentName, String responseText, long latencyMs) {
        return new AgentResult(
                agentName, responseText, List.of(), false, null, null, true, Map.of(), latencyMs,
                AgentOutcome.COMPLETED, null, List.of(), null
        );
    }

    public static AgentResult withTools(String agentName, String responseText,
                                        List<ToolResult> toolExecutions, long latencyMs) {
        return new AgentResult(
                agentName, responseText, toolExecutions, false, null, null, true, Map.of(), latencyMs,
                AgentOutcome.EXECUTE, null, List.of(), null
        );
    }

    public static AgentResult approvalRequired(String agentName, String explanation,
                                              ApprovalRequestData approvalData, long latencyMs) {
        return new AgentResult(
                agentName, explanation, List.of(), true, approvalData, null, true, Map.of(), latencyMs,
                AgentOutcome.NEEDS_USER_CONFIRMATION, null, List.of(), null
        );
    }

    public static AgentResult handoff(String agentName, String partialResponse,
                                     String nextAgentName, Map<String, Object> state, long latencyMs) {
        return new AgentResult(
                agentName, partialResponse, List.of(), false, null, nextAgentName, true, state, latencyMs,
                AgentOutcome.COMPLETED, null, List.of(), null
        );
    }

    public static AgentResult failure(String agentName, String errorMessage, long latencyMs) {
        return new AgentResult(
                agentName, errorMessage, List.of(), false, null, null, false, Map.of(), latencyMs,
                AgentOutcome.FAILED, null, List.of(), AgentErrorCode.AGENT_EXECUTION_FAILED.name()
        );
    }

    public static AgentResult needsInformation(String agentName, String message, List<String> missingFields, long latencyMs) {
        return new AgentResult(
                agentName, message, List.of(), false, null, null, false, Map.of(), latencyMs,
                AgentOutcome.NEEDS_INFORMATION, null,
                missingFields != null ? List.copyOf(missingFields) : List.of(),
                AgentErrorCode.MISSING_INFORMATION.name()
        );
    }

    public static AgentResult notImplemented(String agentName, String message, long latencyMs) {
        return new AgentResult(
                agentName, message, List.of(), false, null, null, false, Map.of(), latencyMs,
                AgentOutcome.NOT_IMPLEMENTED, null, List.of(), AgentErrorCode.NOT_IMPLEMENTED.name()
        );
    }

    public static AgentResult rejected(String agentName, String message, long latencyMs) {
        return new AgentResult(
                agentName, message, List.of(), false, null, null, false, Map.of(), latencyMs,
                AgentOutcome.REJECTED, null, List.of(), AgentErrorCode.REJECTED.name()
        );
    }

    public static AgentResult cancelled(String agentName, String message, long latencyMs) {
        return new AgentResult(
                agentName, message, List.of(), false, null, null, false, Map.of(), latencyMs,
                AgentOutcome.CANCELLED, null, List.of(), AgentErrorCode.CANCELLED.name()
        );
    }

    public static AgentResult unsupported(String agentName, String message, long latencyMs) {
        return new AgentResult(
                agentName, message, List.of(), false, null, null, false, Map.of(), latencyMs,
                AgentOutcome.FAILED, null, List.of(), AgentErrorCode.UNSUPPORTED_REQUEST.name()
        );
    }

    public boolean pausesForUser() {
        return outcome == AgentOutcome.NEEDS_INFORMATION
                || outcome == AgentOutcome.NEEDS_USER_CONFIRMATION
                || outcome == AgentOutcome.WAITING_USER
                || requiresApproval;
    }

    public record ApprovalRequestData(
            String actionType,
            String actionDescription,
            String riskLevel,
            Map<String, Object> payload
    ) {}
}
