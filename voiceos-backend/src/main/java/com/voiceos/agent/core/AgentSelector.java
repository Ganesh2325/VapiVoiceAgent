package com.voiceos.agent.core;

import java.util.Optional;

/**
 * Selects an agent for an action. Deterministic: owned tool, then capability/canHandle,
 * then ConversationAgent fallback.
 */
public interface AgentSelector {

    Optional<Agent> select(AgentContext context, String requestedTool);

    default Optional<Agent> select(AgentRequest request) {
        String tool = request != null ? request.requestedTool() : null;
        return select(request != null ? request.toContext() : null, tool);
    }
}
