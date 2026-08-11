package com.voiceos.agent.impl;

import com.voiceos.agent.core.Agent;
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

/**
 * Baseline Conversation Agent.
 * Handles general conversational queries, questions, and chitchat.
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
        return "Handles general conversational chat, questions, greetings, and queries not covered by specialized agents.";
    }

    @Override
    public boolean canHandle(AgentContext context) {
        return true; // Fallback agent can handle everything
    }

    @Override
    public int getPriority() {
        return 999; // Lowest priority so specialized agents match first
    }

    @Override
    public AgentResult execute(AgentContext context) {
        long startMs = System.currentTimeMillis();
        log.info("ConversationAgent processing: '{}'", context.userInput());

        String systemPrompt = """
        You are VoiceOS, an advanced multi-agent voice AI assistant.
        Be concise, professional, warm, and helpful.
        Speak directly and clearly. Never ramble.
        """;

        LLMRequest request = LLMRequest.simple(systemPrompt, context.userInput());
        LLMResponse response = llmProvider.chat(request);

        long latency = System.currentTimeMillis() - startMs;
        return AgentResult.success(getName(), response.content(), latency);
    }

    @Override
    public List<Tool> getTools() {
        return List.of();
    }
}
