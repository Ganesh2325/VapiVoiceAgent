package com.voiceos.agent.impl;

import com.voiceos.agent.core.Agent;
import com.voiceos.agent.core.AgentContext;
import com.voiceos.agent.core.AgentResult;
import com.voiceos.service.WorkflowService;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

/**
 * Manages background workflows, recurring jobs, and cron scheduling.
 */
@Component
public class WorkflowAgent implements Agent {

    private final WorkflowService workflowService;

    public WorkflowAgent(WorkflowService workflowService) {
        this.workflowService = workflowService;
    }

    @Override
    public String getName() { return "WorkflowAgent"; }

    @Override
    public String getDescription() { return "Schedules and manages automated background workflows."; }

    @Override
    public AgentResult execute(AgentContext context) {
        // Simple heuristic for MVP scheduling
        String input = context.userInput().toLowerCase();
        int minutesDelay = 5; // Default

        if (input.contains("tomorrow")) {
            minutesDelay = 24 * 60;
        } else if (input.contains("hour")) {
            minutesDelay = 60;
        }

        workflowService.scheduleTask(
            () -> System.out.println("Executing scheduled workflow for user: " + context.userId()),
            Instant.now().plus(minutesDelay, ChronoUnit.MINUTES)
        );

        return AgentResult.success("WorkflowAgent", "Workflow scheduled successfully for " + minutesDelay + " minutes from now.");
    }
}
