package com.voiceos.agent.impl;

import com.voiceos.agent.core.Agent;
import com.voiceos.agent.core.AgentCapability;
import com.voiceos.agent.core.AgentContext;
import com.voiceos.agent.core.AgentResult;
import com.voiceos.ai.LLMProvider;
import com.voiceos.ai.LLMRequest;
import com.voiceos.ai.LLMResponse;
import com.voiceos.tool.core.Tool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;

/**
 * Customer Support and Platform Guide Agent.
 * Answers questions about VoiceOS capabilities, setup, agents, and configuration.
 */
@Component
public class SupportAgent implements Agent {

    private static final Logger log = LoggerFactory.getLogger(SupportAgent.class);
    private final LLMProvider llmProvider;

    public SupportAgent(LLMProvider llmProvider) {
        this.llmProvider = llmProvider;
    }

    @Override
    public String getName() {
        return "SupportAgent";
    }

    @Override
    public String getDescription() {
        return "Provides platform guidance, help, documentation, and troubleshooting assistance for VoiceOS.";
    }

    @Override
    public boolean canHandle(AgentContext context) {
        String input = context.userInput() != null ? context.userInput().toLowerCase() : "";
        return input.contains("what can you do")
                || input.contains("how do i use voiceos")
                || input.contains("voiceos support")
                || input.contains("voiceos features")
                || (input.contains("help") && input.contains("voiceos"));
    }

    @Override
    public int getPriority() {
        return 45;
    }

    @Override
    public Set<AgentCapability> capabilities() {
        return Set.of(AgentCapability.SUPPORT);
    }

    @Override
    public AgentResult execute(AgentContext context) {
        long startMs = System.currentTimeMillis();
        String input = context.userInput();
        log.info("SupportAgent processing: '{}'", input);

        String systemPrompt = """
        You are the SupportAgent for VoiceOS.
        VoiceOS is a multi-agent AI voice operations platform that allows users to:
        - Manage tasks, check schedules, and research companies
        - Plan travel with flight/hotel search and budgeting
        - Save and recall long-term preferences
        - Draft emails with strict human approval before dispatch
        - Ingest documents (PDF/TXT) for semantic RAG search
        Provide clear, concise guidance with step-by-step instructions.
        """;

        LLMRequest request = LLMRequest.simple(systemPrompt, input);
        LLMResponse response = llmProvider.chat(request);

        long latency = System.currentTimeMillis() - startMs;
        return AgentResult.success(getName(), response.content(), latency);
    }

    @Override
    public List<Tool> getTools() {
        return List.of();
    }
}
