package com.voiceos.agent.impl;

import com.voiceos.agent.core.Agent;
import com.voiceos.agent.core.AgentCapability;
import com.voiceos.agent.core.AgentContext;
import com.voiceos.agent.core.AgentResult;
import com.voiceos.service.WorkflowService;
import com.voiceos.tool.core.Tool;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;

/**
 * In-memory scheduler stub. WorkflowEngine is not implemented.
 * This agent does not claim successful workflow execution.
 */
@Component
public class WorkflowAgent implements Agent {

    private final WorkflowService workflowService;

    public WorkflowAgent(WorkflowService workflowService) {
        this.workflowService = workflowService;
    }

    @Override
    public String getName() {
        return "WorkflowAgent";
    }

    @Override
    public String getDescription() {
        return "STUB: in-memory delayed callback scheduler. WorkflowEngine is not implemented.";
    }

    @Override
    public boolean canHandle(AgentContext context) {
        if (context == null || context.userInput() == null) {
            return false;
        }
        String input = context.userInput().toLowerCase();
        return input.contains("schedule workflow") || input.contains("background job");
    }

    @Override
    public int getPriority() {
        return 88;
    }

    @Override
    public Set<AgentCapability> capabilities() {
        return Set.of(AgentCapability.WORKFLOW);
    }

    @Override
    public AgentResult execute(AgentContext context) {
        long startMs = System.currentTimeMillis();
        return AgentResult.notImplemented(getName(),
                "NOT_IMPLEMENTED: WorkflowEngine is not implemented. No workflow steps, agents, or external actions were executed."
                        + (workflowService != null ? "" : ""),
                System.currentTimeMillis() - startMs);
    }

    @Override
    public List<Tool> getTools() {
        return List.of();
    }
}
