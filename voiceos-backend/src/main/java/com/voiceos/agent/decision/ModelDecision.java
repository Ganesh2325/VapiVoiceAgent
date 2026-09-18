package com.voiceos.agent.decision;

import java.util.List;
import java.util.Map;

/**
 * Structured, untrusted model output. Parsed server-side; never executed as code.
 */
public record ModelDecision(
        String outcome,
        String answer,
        String intent,
        String toolName,
        Map<String, Object> arguments,
        List<String> missingFields
) {
    public ModelDecision {
        if (arguments == null) {
            arguments = Map.of();
        }
        if (missingFields == null) {
            missingFields = List.of();
        }
    }

    public boolean isTool() {
        return "TOOL".equalsIgnoreCase(outcome) && toolName != null && !toolName.isBlank();
    }

    public boolean isClarify() {
        return "CLARIFY".equalsIgnoreCase(outcome)
                || "NEEDS_INFORMATION".equalsIgnoreCase(outcome);
    }

    public boolean isUnavailable() {
        return "UNAVAILABLE".equalsIgnoreCase(outcome);
    }
}
