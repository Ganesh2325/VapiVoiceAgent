package com.voiceos.agent.core;

import java.util.Optional;

/**
 * Selects an agent for an action. Deterministic: owned tool, then canHandle,
 * then GeneralQueryAgent fallback. IntentClassifier labels requests; it does not pick agents.
 */
public interface AgentSelector {

    Optional<Agent> select(AgentContext context, String requestedTool);

    default Optional<Agent> select(AgentRequest request) {
        String tool = request != null ? request.requestedTool() : null;
        return select(request != null ? request.toContext() : null, tool);
    }
}
