package com.voiceos.agent.core;

import com.voiceos.tool.core.Tool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Startup-time agent catalog. Duplicate names fail fast.
 * Lookup order: priority ascending (lower wins), then case-insensitive name.
 */
@Service
public class AgentRegistry {

    private static final Logger log = LoggerFactory.getLogger(AgentRegistry.class);

    static final Comparator<Agent> ORDER = Comparator
            .comparingInt(Agent::getPriority)
            .thenComparing(agent -> agent.getName() != null ? agent.getName() : "", String.CASE_INSENSITIVE_ORDER);

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
        if (agent == null || agent.getName() == null || agent.getName().isBlank()) {
            throw new IllegalArgumentException("Agent name is required");
        }
        String key = agent.getName().toLowerCase(Locale.ROOT);
        Agent previous = agents.putIfAbsent(key, agent);
        if (previous != null && previous != agent) {
            throw new IllegalStateException("Duplicate agent name: " + agent.getName());
        }
        log.info("Registered agent: '{}' (priority={})", agent.getName(), agent.getPriority());
    }

    public Optional<Agent> getAgent(String name) {
        if (name == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(agents.get(name.toLowerCase(Locale.ROOT)));
    }

    public List<Agent> getAllAgents() {
        return agents.values().stream().sorted(ORDER).toList();
    }

    /**
     * Specialized handler excluding GeneralQueryAgent fallback. Priority then name.
     */
    public Optional<Agent> findHandler(AgentContext context) {
        return findHandler(AgentRequest.from(context, null));
    }

    public Optional<Agent> findHandler(AgentRequest request) {
        return getAllAgents().stream()
                .filter(agent -> !isFallback(agent))
                .filter(agent -> agent.canHandle(request))
                .findFirst();
    }

    public Optional<Agent> findByOwnedTool(String toolName) {
        if (toolName == null || toolName.isBlank()) {
            return Optional.empty();
        }
        return getAllAgents().stream()
                .filter(agent -> !isFallback(agent))
                .filter(agent -> agent.ownsTool(toolName))
                .findFirst();
    }

    public List<Agent> findByCapability(AgentCapability capability) {
        if (capability == null) {
            return List.of();
        }
        return getAllAgents().stream()
                .filter(agent -> agent.capabilities() != null && agent.capabilities().contains(capability))
                .toList();
    }

    public boolean hasAgent(String name) {
        return name != null && agents.containsKey(name.toLowerCase(Locale.ROOT));
    }

    public Map<String, Object> catalogEntry(Agent agent) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("name", agent.getName());
        row.put("description", agent.getDescription());
        row.put("priority", agent.getPriority());
        row.put("capabilities", agent.capabilities() == null
                ? List.of()
                : agent.capabilities().stream().map(Enum::name).toList());
        row.put("ownedTools", agent.ownedTools() == null
                ? List.of()
                : agent.ownedTools().stream().map(Tool::getName).toList());
        row.put("toolsCount", agent.ownedTools() != null ? agent.ownedTools().size() : 0);
        return row;
    }

    public List<Map<String, Object>> catalog() {
        List<Map<String, Object>> rows = new ArrayList<>();
        for (Agent agent : getAllAgents()) {
            rows.add(catalogEntry(agent));
        }
        return rows;
    }

    private static boolean isFallback(Agent agent) {
        String name = agent.getName();
        return name != null && (name.equalsIgnoreCase("GeneralQueryAgent")
                || name.equalsIgnoreCase("ConversationAgent")
                || name.equalsIgnoreCase("OrchestratorAgent"));
    }
}
