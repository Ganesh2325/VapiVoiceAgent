package com.voiceos.agent.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Registry of all available AI Agents in the VoiceOS platform.
 * Supports dynamic registration, lookup, intent matching, and lifecycle tracking.
 */
@Service
public class AgentRegistry {

    private static final Logger log = LoggerFactory.getLogger(AgentRegistry.class);

    private final Map<String, Agent> agents = new ConcurrentHashMap<>();

    public AgentRegistry(List<Agent> discoveredAgents) {
        if (discoveredAgents != null) {
            for (Agent agent : discoveredAgents) {
                registerAgent(agent);
            }
        }
        log.info("AgentRegistry initialized with {} agent(s): {}", agents.size(), agents.keySet());
    }

    public void registerAgent(Agent agent) {
        agents.put(agent.getName().toLowerCase(), agent);
        log.info("Registered agent: '{}' (priority={})", agent.getName(), agent.getPriority());
    }

    public Optional<Agent> getAgent(String name) {
        if (name == null) return Optional.empty();
        return Optional.ofNullable(agents.get(name.toLowerCase()));
    }

    public List<Agent> getAllAgents() {
        return agents.values().stream()
                .sorted(Comparator.comparingInt(Agent::getPriority))
                .toList();
    }

    /**
     * Finds the most specific agent capable of handling the context.
     * Evaluates in priority order.
     *
     * @param context execution context
     * @return matching agent or Optional.empty()
     */
    public Optional<Agent> findHandler(AgentContext context) {
        return getAllAgents().stream()
                .filter(agent -> !agent.getName().equalsIgnoreCase("ConversationAgent"))
                .filter(agent -> !agent.getName().equalsIgnoreCase("OrchestratorAgent"))
                .filter(agent -> agent.canHandle(context))
                .findFirst();
    }

    public boolean hasAgent(String name) {
        return name != null && agents.containsKey(name.toLowerCase());
    }
}
