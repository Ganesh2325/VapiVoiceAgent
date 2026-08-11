package com.voiceos.agent.core;

import com.voiceos.tool.core.Tool;
import java.util.List;

/**
 * Core Agent interface for VoiceOS.
 *
 * <p>All specialized AI agents implement this interface:
 * <ul>
 *   <li>{@code getName()} — unique logical name of the agent</li>
 *   <li>{@code getDescription()} — system description used by Orchestrator to route intents</li>
 *   <li>{@code canHandle(context)} — deterministic / heuristic check before delegating to LLM</li>
 *   <li>{@code execute(context)} — main execution loop (tool calls, LLM reasoning, state updates)</li>
 *   <li>{@code getTools()} — tools available to this agent</li>
 * </ul>
 */
public interface Agent {

    /**
     * @return the unique name of the agent (e.g. "TravelAgent", "TaskAgent")
     */
    String getName();

    /**
     * @return human and LLM-readable description of what this agent can do
     */
    String getDescription();

    /**
     * Fast deterministic or keyword-based check to evaluate if this agent is relevant.
     * Used by Orchestrator for low-latency intent routing.
     *
     * @param context the current agent execution context
     * @return true if this agent can handle or contribute to the request
     */
    boolean canHandle(AgentContext context);

    /**
     * Executes the agent's logic for the given context.
     *
     * @param context execution context including user input, state, memory, and tools
     * @return structured agent result
     */
    AgentResult execute(AgentContext context);

    /**
     * @return list of tools registered to this agent
     */
    List<Tool> getTools();

    /**
     * @return priority order when multiple agents can handle the same intent (lower = higher priority)
     */
    default int getPriority() {
        return 100;
    }
}
