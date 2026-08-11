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
 * Specialized Deep Research & Synthesis Agent.
 * Conducts multi-step research on companies, topics, technologies, and competitors.
 */
@Component
public class ResearchAgent implements Agent {

    private static final Logger log = LoggerFactory.getLogger(ResearchAgent.class);
    private final LLMProvider llmProvider;
    private final ToolRegistry toolRegistry;

    public ResearchAgent(LLMProvider llmProvider, ToolRegistry toolRegistry) {
        this.llmProvider = llmProvider;
        this.toolRegistry = toolRegistry;
    }

    @Override
    public String getName() {
        return "ResearchAgent";
    }

    @Override
    public String getDescription() {
        return "Researches companies, technologies, competitors, and topics using web search and document RAG.";
    }

    @Override
    public boolean canHandle(AgentContext context) {
        String input = context.userInput().toLowerCase();
        return input.contains("research") || input.contains("competitor") || input.contains("analyze company")
                || input.contains("market overview") || input.contains("investigate");
    }

    @Override
    public int getPriority() {
        return 15;
    }

    @Override
    public AgentResult execute(AgentContext context) {
        long startMs = System.currentTimeMillis();
        String input = context.userInput();
        log.info("ResearchAgent executing research: '{}'", input);

        List<ToolResult> tools = new ArrayList<>();

        // 1. Web search
        ToolResult webResult = toolRegistry.executeTool("search_web", Map.of("query", input));
        tools.add(webResult);

        // 2. Document search
        ToolResult docResult = toolRegistry.executeTool("document_search", Map.of("query", input));
        tools.add(docResult);

        // 3. Synthesize structured briefing
        String systemPrompt = """
        You are the ResearchAgent for VoiceOS.
        Analyze the search findings and document passages to create an executive-level research briefing.
        Structure with:
        1. Executive Summary
        2. Key Findings & Competitor Analysis
        3. Strategic Recommendations
        """;

        String userPrompt = "Research Topic: " + input + "\n\nWeb Data:\n" + webResult.rawOutput() + "\n\nDoc Data:\n" + docResult.rawOutput();
        LLMRequest request = LLMRequest.simple(systemPrompt, userPrompt);
        LLMResponse response = llmProvider.chat(request);

        long latency = System.currentTimeMillis() - startMs;
        return AgentResult.withTools(getName(), response.content(), tools, latency);
    }

    @Override
    public List<Tool> getTools() {
        return toolRegistry.getAllTools().stream()
                .filter(t -> t.getName().equals("search_web") || t.getName().equals("document_search"))
                .toList();
    }
}
