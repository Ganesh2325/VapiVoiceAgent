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
 * PlannerAgent dynamically determines the optimal sequence of agent actions
 * required to fulfill a multi-step user request.
 *
 * <p>Invoked by {@code OrchestratorService} by name. It does not claim that a
 * workflow executed — it only returns a plan JSON string or a failure.
 */
@Component
public class PlannerAgent implements Agent {

    private static final Logger log = LoggerFactory.getLogger(PlannerAgent.class);

    private static final String SYSTEM_PROMPT = """
            You are the PlannerAgent. Break down the user's request into a JSON array of steps.
            Available agents: TravelAgent, TaskAgent, EmailAgent, FinanceAgent, ResearchAgent, CalendarAgent, MemoryAgent, DeveloperAgent, SupportAgent, ConversationAgent.
            Output strictly a JSON array of objects with keys: 'agentName', 'action', 'description'.
            """;

    private final LLMProvider llmProvider;

    public PlannerAgent(LLMProvider llmProvider) {
        this.llmProvider = llmProvider;
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
    public boolean canHandle(AgentContext context) {
        if (context == null || context.userInput() == null) {
            return false;
        }
        String input = context.userInput().toLowerCase();
        return input.contains("multi-step") || input.contains("break down") || input.contains("execution plan");
    }

    @Override
    public int getPriority() {
        return 90;
    }

    @Override
    public Set<AgentCapability> capabilities() {
        return Set.of(AgentCapability.PLANNING);
    }

    @Override
    public AgentResult execute(AgentContext context) {
        long startMs = System.currentTimeMillis();
        String userInput = context != null && context.userInput() != null ? context.userInput() : "";
        log.info("PlannerAgent generating plan for: '{}'", userInput);

        try {
            LLMRequest request = LLMRequest.simple(SYSTEM_PROMPT, userInput);
            LLMResponse response = llmProvider.chat(request);
            String json = response.content() != null ? response.content().trim() : "";
            if (json.startsWith("```json")) {
                json = json.substring(7).trim();
                if (json.endsWith("```")) {
                    json = json.substring(0, json.length() - 3).trim();
                }
            } else if (json.startsWith("```")) {
                json = json.substring(3).trim();
                if (json.endsWith("```")) {
                    json = json.substring(0, json.length() - 3).trim();
                }
            }

            long latency = System.currentTimeMillis() - startMs;
            if (!json.startsWith("[")) {
                log.warn("PlannerAgent did not receive a JSON array plan");
                return AgentResult.failure(getName(),
                        "PLAN_NOT_GENERATED: model output was not a JSON array. Orchestrator should fall back.",
                        latency);
            }

            return AgentResult.success(getName(), json, latency);
        } catch (Exception e) {
            long latency = System.currentTimeMillis() - startMs;
            log.error("PlannerAgent failed: {}", e.getMessage());
            return AgentResult.failure(getName(), "Planning failed: " + e.getMessage(), latency);
        }
    }

    @Override
    public List<Tool> getTools() {
        return List.of();
    }
}
