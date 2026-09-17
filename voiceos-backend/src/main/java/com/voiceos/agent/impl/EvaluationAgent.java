package com.voiceos.agent.impl;

import com.voiceos.agent.core.Agent;
import com.voiceos.agent.core.AgentCapability;
import com.voiceos.agent.core.AgentContext;
import com.voiceos.agent.core.AgentResult;
import com.voiceos.domain.repository.EvaluationRepository;
import com.voiceos.tool.core.Tool;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;

/**
 * Quality evaluation is not implemented. This agent does not invent scores.
 */
@Component
public class EvaluationAgent implements Agent {

    private final EvaluationRepository evaluationRepository;

    public EvaluationAgent(EvaluationRepository evaluationRepository) {
        this.evaluationRepository = evaluationRepository;
    }

    @Override
    public String getName() {
        return "EvaluationAgent";
    }

    @Override
    public String getDescription() {
        return "Reserved for measured quality evaluation. Invented accuracy scores are forbidden.";
    }

    @Override
    public boolean canHandle(AgentContext context) {
        String input = context.userInput() != null ? context.userInput().toLowerCase() : "";
        return input.contains("evaluate") || input.contains("quality score") || input.contains("metrics")
                || input.contains("benchmark") || input.contains("accuracy");
    }

    @Override
    public int getPriority() {
        return 50;
    }

    @Override
    public Set<AgentCapability> capabilities() {
        return Set.of(AgentCapability.EVALUATION);
    }

    @Override
    public AgentResult execute(AgentContext context) {
        long startMs = System.currentTimeMillis();
        return AgentResult.notImplemented(getName(),
                "NOT_IMPLEMENTED: EvaluationAgent has no measured quality source. "
                        + "It does not invent completion rates or accuracy scores."
                        + (evaluationRepository != null ? "" : ""),
                System.currentTimeMillis() - startMs);
    }

    @Override
    public List<Tool> getTools() {
        return List.of();
    }
}
