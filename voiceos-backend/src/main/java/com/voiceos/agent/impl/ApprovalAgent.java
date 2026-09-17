package com.voiceos.agent.impl;

import com.voiceos.agent.core.Agent;
import com.voiceos.agent.core.AgentCapability;
import com.voiceos.agent.core.AgentContext;
import com.voiceos.agent.core.AgentResult;
import com.voiceos.tool.core.Tool;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;

/**
 * Placeholder agent for explicit human-in-the-loop language.
 * Approvals are processed by {@code ApprovalService}, not by this agent.
 */
@Component
public class ApprovalAgent implements Agent {

    @Override
    public String getName() {
        return "ApprovalAgent";
    }

    @Override
    public String getDescription() {
        return "Describes human-in-the-loop approval. Actual approve/reject is handled by ApprovalService.";
    }

    @Override
    public boolean canHandle(AgentContext context) {
        if (context == null || context.userInput() == null) {
            return false;
        }
        String input = context.userInput().toLowerCase();
        return input.contains("approval queue") || input.contains("pending approval");
    }

    @Override
    public int getPriority() {
        return 80;
    }

    @Override
    public Set<AgentCapability> capabilities() {
        return Set.of(AgentCapability.APPROVAL);
    }

    @Override
    public AgentResult execute(AgentContext context) {
        long startMs = System.currentTimeMillis();
        return AgentResult.notImplemented(getName(),
                "NOT_IMPLEMENTED: ApprovalAgent does not approve or reject actions. "
                        + "Use ApprovalService via POST /api/v1/approvals/{id}/approve or /reject.",
                System.currentTimeMillis() - startMs);
    }

    @Override
    public List<Tool> getTools() {
        return List.of();
    }
}
