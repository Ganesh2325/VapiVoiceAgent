package com.voiceos.agent.impl;

import com.voiceos.agent.core.Agent;
import com.voiceos.agent.core.AgentContext;
import com.voiceos.agent.core.AgentResult;
import com.voiceos.ai.LLMProvider;
import com.voiceos.ai.LLMRequest;
import com.voiceos.ai.LLMResponse;
import com.voiceos.tool.core.Tool;
import com.voiceos.tool.core.ToolRegistry;
import com.voiceos.tool.core.ToolResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Specialized Travel & Trip Planning Agent.
 * Searches flights, hotels, builds itineraries, and estimates trip budgets.
 */
@Component
public class TravelAgent implements Agent {

    private static final Logger log = LoggerFactory.getLogger(TravelAgent.class);
    private final LLMProvider llmProvider;
    private final ToolRegistry toolRegistry;

    public TravelAgent(LLMProvider llmProvider, ToolRegistry toolRegistry) {
        this.llmProvider = llmProvider;
        this.toolRegistry = toolRegistry;
    }

    @Override
    public String getName() {
        return "TravelAgent";
    }

    @Override
    public String getDescription() {
        return "Specialized in planning trips, flight search, hotel recommendations, and itinerary generation.";
    }

    @Override
    public boolean canHandle(AgentContext context) {
        String input = context.userInput().toLowerCase();
        return input.contains("trip") || input.contains("flight") || input.contains("hotel")
                || input.contains("travel") || input.contains("bangalore") || input.contains("vacation")
                || input.contains("itinerary");
    }

    @Override
    public int getPriority() {
        return 20;
    }

    @Override
    public AgentResult execute(AgentContext context) {
        long startMs = System.currentTimeMillis();
        log.info("TravelAgent planning trip for: '{}'", context.userInput());

        List<ToolResult> executedTools = new ArrayList<>();

        // 1. Search options using SearchTool
        ToolResult searchResult = toolRegistry.executeTool("search_web", Map.of("query", context.userInput()));
        executedTools.add(searchResult);

        // 2. Synthesize complete trip plan using LLM
        String systemPrompt = """
        You are the TravelAgent of VoiceOS.
        Your goal is to build an organized travel plan with flight options, hotels, and estimated total budget.
        Clearly format with markdown emojis and bullet points.
        Include a disclaimer if mock data is used.
        """;

        LLMRequest request = LLMRequest.simple(systemPrompt, context.userInput() + "\nSearch findings: " + searchResult.rawOutput());
        LLMResponse response = llmProvider.chat(request);

        long latency = System.currentTimeMillis() - startMs;
        return AgentResult.withTools(getName(), response.content(), executedTools, latency);
    }

    @Override
    public List<Tool> getTools() {
        return toolRegistry.getAllTools().stream()
                .filter(t -> t.getName().equals("search_web") || t.getName().equals("calculator"))
                .toList();
    }
}
