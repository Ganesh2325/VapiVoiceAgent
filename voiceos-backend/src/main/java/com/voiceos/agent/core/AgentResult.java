package com.voiceos.agent.core;

import com.voiceos.tool.core.ToolResult;

import java.util.List;
import java.util.Map;

/**
 * Structured result produced by an Agent execution.
 *
 * @param agentName        the agent that generated this result
 * @param responseText     the text response generated for the user / next agent
 * @param toolExecutions   list of tools called during this execution
 * @param requiresApproval whether execution is paused waiting for human approval
 * @param approvalAction   details of approval request if required
 * @param nextAgentName    suggested next agent if delegating / handoff
 * @param success          whether the agent completed its task
 * @param stateUpdates     variables to persist back into conversation state
 * @param latencyMs        duration of execution in milliseconds
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
        long latencyMs
) {

    public static AgentResult success(String agentName, String responseText, long latencyMs) {
        return new AgentResult(
                agentName, responseText, List.of(), false, null, null, true, Map.of(), latencyMs
        );
    }

    public static AgentResult withTools(String agentName, String responseText,
                                        List<ToolResult> toolExecutions, long latencyMs) {
        return new AgentResult(
                agentName, responseText, toolExecutions, false, null, null, true, Map.of(), latencyMs
        );
    }

    public static AgentResult approvalRequired(String agentName, String explanation,
                                              ApprovalRequestData approvalData, long latencyMs) {
        return new AgentResult(
                agentName, explanation, List.of(), true, approvalData, null, true, Map.of(), latencyMs
        );
    }

    public static AgentResult handoff(String agentName, String partialResponse,
                                     String nextAgentName, Map<String, Object> state, long latencyMs) {
        return new AgentResult(
                agentName, partialResponse, List.of(), false, null, nextAgentName, true, state, latencyMs
        );
    }

    public static AgentResult failure(String agentName, String errorMessage, long latencyMs) {
        return new AgentResult(
                agentName, errorMessage, List.of(), false, null, null, false, Map.of(), latencyMs
        );
    }

    public record ApprovalRequestData(
            String actionType,
            String actionDescription,
            String riskLevel,
            Map<String, Object> payload
    ) {}
}
