package com.voiceos.action.model;

import com.voiceos.tool.core.ToolResult;

import java.util.List;
import java.util.UUID;

/**
 * Result of the canonical ActionEngine pipeline.
 * Field names {@code responseText}, {@code activeAgent}, {@code toolsExecuted},
 * {@code awaitingApproval} are kept so the existing console can consume them.
 */
public record ActionExecutionResult(
        UUID actionId,
        UUID conversationId,
        String status,
        String agent,
        String tool,
        String provider,
        String providerMode,
        String result,
        String error,
        boolean success,
        boolean awaitingApproval,
        List<ToolResult> toolsExecuted,
        long totalLatencyMs,
        String responseText,
        String activeAgent
) {
    public static ActionExecutionResult from(Action action, List<ToolResult> tools, long latencyMs) {
        boolean success = action.getStatus() == ActionStatus.COMPLETED;
        boolean approval = action.getStatus() == ActionStatus.REQUIRES_APPROVAL;
        String text = action.getResult() != null ? action.getResult() : action.getErrorMessage();
        return new ActionExecutionResult(
                action.getId(),
                action.getConversationId(),
                action.getStatus() != null ? action.getStatus().name() : null,
                action.getTargetAgent(),
                action.getToolName(),
                action.getTargetProvider(),
                action.getProviderMode(),
                action.getResult(),
                action.getErrorMessage(),
                success,
                approval,
                tools != null ? tools : List.of(),
                latencyMs,
                text,
                action.getTargetAgent()
        );
    }
}
