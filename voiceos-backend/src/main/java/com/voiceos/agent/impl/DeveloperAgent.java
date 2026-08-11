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
 * Specialized Developer & Engineering Agent.
 * Analyzes code architecture, reviews GitHub repository status, inspects PRs and issues.
 * Does not allow unrestricted code execution — all changes require human review.
 */
@Component
public class DeveloperAgent implements Agent {

    private static final Logger log = LoggerFactory.getLogger(DeveloperAgent.class);
    private final ToolRegistry toolRegistry;
    private final LLMProvider llmProvider;

    public DeveloperAgent(ToolRegistry toolRegistry, LLMProvider llmProvider) {
        this.toolRegistry = toolRegistry;
        this.llmProvider = llmProvider;
    }

    @Override
    public String getName() {
        return "DeveloperAgent";
    }

    @Override
    public String getDescription() {
        return "Analyzes GitHub repositories, reviews architecture, inspects PRs, and provides engineering recommendations.";
    }

    @Override
    public boolean canHandle(AgentContext context) {
        String input = context.userInput().toLowerCase();
        return input.contains("github") || input.contains("repository") || input.contains("repo")
                || input.contains("pull request") || input.contains("architecture") || input.contains("code review");
    }

    @Override
    public int getPriority() {
        return 20;
    }

    @Override
    public AgentResult execute(AgentContext context) {
        long startMs = System.currentTimeMillis();
        String input = context.userInput();
        log.info("DeveloperAgent analyzing: '{}'", input);

        List<ToolResult> tools = new ArrayList<>();
        ToolResult githubResult = toolRegistry.executeTool("github_inspector", Map.of("repo", "voiceos/core"));
        tools.add(githubResult);

        String systemPrompt = """
        You are the DeveloperAgent of VoiceOS.
        Provide a sharp, senior architect-level summary of repository status and engineering recommendations.
        Use markdown code blocks and clear bullet points.
        """;

        String userPrompt = input + "\n\nRepo Inspector Output:\n" + githubResult.rawOutput();
        LLMRequest request = LLMRequest.simple(systemPrompt, userPrompt);
        LLMResponse response = llmProvider.chat(request);

        long latency = System.currentTimeMillis() - startMs;
        return AgentResult.withTools(getName(), response.content(), tools, latency);
    }

    @Override
    public List<Tool> getTools() {
        return toolRegistry.getAllTools().stream()
                .filter(t -> t.getName().equals("github_inspector"))
                .toList();
    }
}
