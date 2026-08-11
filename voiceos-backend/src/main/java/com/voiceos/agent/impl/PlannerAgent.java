package com.voiceos.agent.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.voiceos.agent.core.Agent;
import com.voiceos.agent.core.AgentContext;
import com.voiceos.agent.core.AgentResult;
import com.voiceos.ai.LLMProvider;
import com.voiceos.ai.LLMRequest;
import com.voiceos.ai.LLMResponse;
import com.voiceos.domain.entity.AgentPlan;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * PlannerAgent dynamically determines the optimal sequence of agent actions
 * required to fulfill a multi-step user request.
 */
@Component
public class PlannerAgent implements Agent {

    private final LLMProvider llmProvider;
    private final ObjectMapper objectMapper;

    public PlannerAgent(LLMProvider llmProvider, ObjectMapper objectMapper) {
        this.llmProvider = llmProvider;
        this.objectMapper = objectMapper;
    }

    @Override
    public String getName() {
        return "PlannerAgent";
    }

    @Override
    public String getDescription() {
        return "Analyzes user requests and breaks them down into a sequence of agent steps.";
    }

    @Override
    public AgentResult execute(AgentContext context) {
        String prompt = "You are the PlannerAgent. Break down the user's request into a JSON array of steps. " +
                "Available agents: TravelAgent, TaskAgent, EmailAgent, FinanceAgent, ResearchAgent, CalendarAgent, MemoryAgent, DeveloperAgent, SupportAgent, ConversationAgent. " +
                "Output strictly a JSON array of objects with keys: 'agentName', 'action', 'description'. " +
                "User Request: " + context.userInput();

        try {
            LLMResponse response = llmProvider.generate(new LLMRequest(
                    "gpt-4o",
                    prompt,
                    "system",
                    0.0
            ));

            String json = response.text().trim();
            if (json.startsWith("```json")) {
                json = json.substring(7, json.length() - 3).trim();
            }

            // Fallback for empty/invalid
            if (!json.startsWith("[")) {
                return AgentResult.success("PlannerAgent", "Failed to generate plan. Sending to ConversationAgent.");
            }

            return AgentResult.success("PlannerAgent", json);
            
        } catch (Exception e) {
            return AgentResult.failure("PlannerAgent", "Planning failed: " + e.getMessage());
        }
    }
}
