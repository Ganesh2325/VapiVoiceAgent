package com.voiceos.agent.core;

import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Deterministic routing:
 * <ol>
 *   <li>requested tool ownership (excluding GeneralQueryAgent)</li>
 *   <li>deterministic specialized {@code canHandle}</li>
 *   <li>structured intent labels via {@link IntentClassifier} (routing hint only; does not pick the agent)</li>
 *   <li>GeneralQueryAgent fallback</li>
 * </ol>
 * Client-supplied agent names are ignored.
 */
@Component
public class RegistryAgentSelector implements AgentSelector {

    private final AgentRegistry agentRegistry;

    public RegistryAgentSelector(AgentRegistry agentRegistry) {
        this.agentRegistry = agentRegistry;
    }

    @Override
    public Optional<Agent> select(AgentContext context, String requestedTool) {
        return select(AgentRequest.from(context, requestedTool));
    }

    @Override
    public Optional<Agent> select(AgentRequest request) {
        if (request != null && request.requestedTool() != null && !request.requestedTool().isBlank()) {
            Optional<Agent> byTool = agentRegistry.findByOwnedTool(request.requestedTool());
            if (byTool.isPresent()) {
                return byTool;
            }
        }
        Optional<Agent> handler = agentRegistry.findHandler(request);
        if (handler.isPresent()) {
            return handler;
        }
        return agentRegistry.getAgent("GeneralQueryAgent")
                .or(() -> agentRegistry.getAgent("ConversationAgent"));
    }
}
