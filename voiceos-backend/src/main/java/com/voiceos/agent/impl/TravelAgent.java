package com.voiceos.agent.impl;

import com.voiceos.agent.core.Agent;
import com.voiceos.agent.core.AgentCapability;
import com.voiceos.agent.core.AgentContext;
import com.voiceos.agent.core.AgentRequest;
import com.voiceos.agent.core.AgentResult;
import com.voiceos.tool.core.Tool;
import com.voiceos.tool.core.ToolRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Travel planning agent. Does not book flights or invent itineraries.
 */
@Component
public class TravelAgent implements Agent {

    private static final Logger log = LoggerFactory.getLogger(TravelAgent.class);
    private final ToolRegistry toolRegistry;

    public TravelAgent(ToolRegistry toolRegistry) {
        this.toolRegistry = toolRegistry;
    }

    @Override
    public String getName() {
        return "TravelAgent";
    }

    @Override
    public String getDescription() {
        return "Identifies travel requests and required fields. Does not execute real or fake bookings.";
    }

    @Override
    public boolean canHandle(AgentContext context) {
        String input = context.userInput() != null ? context.userInput().toLowerCase() : "";
        return input.contains("trip") || input.contains("flight") || input.contains("hotel")
                || input.contains("travel") || input.contains("bangalore") || input.contains("vacation")
                || input.contains("itinerary");
    }

    @Override
    public int getPriority() {
        return 20;
    }

    @Override
    public Set<AgentCapability> capabilities() {
        return Set.of(AgentCapability.TRAVEL);
    }

    @Override
    public AgentResult execute(AgentRequest request) {
        long startMs = System.currentTimeMillis();
        if (Thread.currentThread().isInterrupted()) {
            return AgentResult.cancelled(getName(), "Cancelled", System.currentTimeMillis() - startMs);
        }
        log.info("TravelAgent evaluating travel request; no provider will run until required fields exist");
        List<String> missing = new ArrayList<>();
        if (blank(request, "origin")) {
            missing.add("origin");
        }
        if (blank(request, "destination")) {
            missing.add("destination");
        }
        if (blank(request, "travelDate") && blank(request, "date")) {
            missing.add("travelDate");
        }
        if (!missing.isEmpty()) {
            return AgentResult.needsInformation(getName(),
                    "Missing required information: " + String.join(", ", missing)
                            + ". TravelAgent will not invent a booking.",
                    missing,
                    System.currentTimeMillis() - startMs);
        }
        return AgentResult.notImplemented(getName(),
                "NOT_IMPLEMENTED: Flight/hotel booking is not implemented. No provider was invoked.",
                System.currentTimeMillis() - startMs);
    }

    @Override
    public AgentResult execute(AgentContext context) {
        return execute(AgentRequest.from(context, null));
    }

    @Override
    public List<Tool> getTools() {
        List<Tool> tools = new ArrayList<>();
        toolRegistry.getTool("search_web").ifPresent(tools::add);
        return tools;
    }

    private static boolean blank(AgentRequest request, String key) {
        if (request == null) {
            return true;
        }
        String value = request.parameterAsString(key);
        return value == null || value.isBlank() || "null".equalsIgnoreCase(value);
    }
}
