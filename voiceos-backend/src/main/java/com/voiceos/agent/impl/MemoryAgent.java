package com.voiceos.agent.impl;

import com.voiceos.agent.core.Agent;
import com.voiceos.agent.core.AgentCapability;
import com.voiceos.agent.core.AgentContext;
import com.voiceos.agent.core.AgentResult;
import com.voiceos.ai.LLMProvider;
import com.voiceos.tool.core.Tool;
import com.voiceos.tool.core.ToolRegistry;
import com.voiceos.tool.core.ToolResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Long-Term Memory Agent.
 * Explicitly manages user preferences, memories, facts, and profile settings.
 */
@Component
public class MemoryAgent implements Agent {

    private static final Logger log = LoggerFactory.getLogger(MemoryAgent.class);
    private final ToolRegistry toolRegistry;
    private final LLMProvider llmProvider;

    public MemoryAgent(ToolRegistry toolRegistry, LLMProvider llmProvider) {
        this.toolRegistry = toolRegistry;
        this.llmProvider = llmProvider;
    }

    @Override
    public String getName() {
        return "MemoryAgent";
    }

    @Override
    public String getDescription() {
        return "Manages long-term user memory, preferences, past facts, and profile knowledge.";
    }

    @Override
    public boolean canHandle(AgentContext context) {
        String input = context.userInput().toLowerCase();
        return input.contains("remember that") || input.contains("remember i") || input.contains("my preference")
                || input.contains("what do you remember") || input.contains("forget that");
    }

    @Override
    public int getPriority() {
        return 10; // High priority for explicit memory commands
    }

    @Override
    public Set<AgentCapability> capabilities() {
        return Set.of(AgentCapability.MEMORY);
    }

    @Override
    public AgentResult execute(AgentContext context) {
        long startMs = System.currentTimeMillis();
        String input = context.userInput();
        log.info("MemoryAgent processing: '{}'", input);

        List<ToolResult> tools = new ArrayList<>();
        String inputLower = input.toLowerCase();

        if (inputLower.contains("forget")) {
            String keywordToForget = input.replaceAll("(?i)(forget that|forget i|forget)", "").trim();
            if (keywordToForget.isBlank()) keywordToForget = input;

            Map<String, Object> params = new java.util.HashMap<>();
            params.put("keyword", keywordToForget);
            if (context.userId() != null) {
                params.put("userId", context.userId().toString());
            }

            ToolResult deleteResult = toolRegistry.executeTool("memory_delete", params);
            tools.add(deleteResult);
            long latency = System.currentTimeMillis() - startMs;
            return AgentResult.withTools(getName(), deleteResult.rawOutput(), tools, latency);
        } else if (inputLower.contains("remember")) {
            String factToRemember = input.replaceAll("(?i)(remember that|remember i|remember)", "").trim();
            if (factToRemember.isBlank()) factToRemember = input;

            Map<String, Object> params = new java.util.HashMap<>();
            params.put("content", factToRemember);
            params.put("key", "user_preference");
            if (context.userId() != null) {
                params.put("userId", context.userId().toString());
            }

            ToolResult saveResult = toolRegistry.executeTool("memory_save", params);
            tools.add(saveResult);

            long latency = System.currentTimeMillis() - startMs;
            return AgentResult.withTools(getName(), "Got it. I have committed this preference to your long-term memory: \"" + factToRemember + "\"", tools, latency);
        } else {
            Map<String, Object> params = new java.util.HashMap<>();
            if (context.userId() != null) {
                params.put("userId", context.userId().toString());
            }
            ToolResult searchResult = toolRegistry.executeTool("memory_search", params);
            tools.add(searchResult);

            long latency = System.currentTimeMillis() - startMs;
            return AgentResult.withTools(getName(), searchResult.rawOutput(), tools, latency);
        }
    }

    @Override
    public List<Tool> getTools() {
        return toolRegistry.getAllTools().stream()
                .filter(t -> t.getName().equals("memory_save") || t.getName().equals("memory_search") || t.getName().equals("memory_delete"))
                .toList();
    }
}
