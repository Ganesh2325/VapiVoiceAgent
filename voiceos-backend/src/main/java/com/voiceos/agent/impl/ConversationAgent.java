package com.voiceos.agent.impl;

import com.voiceos.agent.core.Agent;
import com.voiceos.agent.core.AgentCapability;
import com.voiceos.agent.core.AgentContext;
import com.voiceos.agent.core.AgentRequest;
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
 * Fallback conversational agent. Does not execute domain tools or fabricate bookings.
 */
@Component
public class ConversationAgent implements Agent {

    private static final Logger log = LoggerFactory.getLogger(ConversationAgent.class);
    private final LLMProvider llmProvider;

    public ConversationAgent(LLMProvider llmProvider) {
        this.llmProvider = llmProvider;
    }

    @Override
    public String getName() {
        return "ConversationAgent";
    }

    @Override
    public String getDescription() {
        return "Handles general conversational chat not covered by specialized agents.";
    }

    @Override
    public boolean canHandle(AgentContext context) {
        return true;
    }

    @Override
    public int getPriority() {
        return 999;
    }

    @Override
    public Set<AgentCapability> capabilities() {
        return Set.of(AgentCapability.CONVERSATION);
    }

    @Override
    public AgentResult execute(AgentRequest request) {
        long startMs = System.currentTimeMillis();
        if (request != null && request.requestedTool() != null && !request.requestedTool().isBlank()) {
            return AgentResult.unsupported(getName(),
                    "ConversationAgent does not execute tool '" + request.requestedTool()
                            + "'. Specialized agents own domain tools.",
                    System.currentTimeMillis() - startMs);
        }
        return execute(request != null ? request.toContext() : AgentContext.of(null, null, null));
    }

    @Override
    public AgentResult execute(AgentContext context) {
        long startMs = System.currentTimeMillis();
        log.info("ConversationAgent processing conversational input");

        String systemPrompt = """
        You are VoiceOS, an advanced multi-agent voice AI assistant.
        Be concise, professional, warm, and helpful.
        Speak directly and clearly. Never ramble.
        Do not claim that a booking, payment, or email was completed.
        """;

        String input = context != null && context.userInput() != null ? context.userInput() : "";
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
