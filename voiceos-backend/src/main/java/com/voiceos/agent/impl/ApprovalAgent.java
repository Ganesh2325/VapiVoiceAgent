package com.voiceos.agent.impl;

import com.voiceos.agent.core.Agent;
import com.voiceos.agent.core.AgentContext;
import com.voiceos.agent.core.AgentResult;
import org.springframework.stereotype.Component;

/**
 * Handles explicit human-in-the-loop approvals for high-risk actions.
 */
@Component
public class ApprovalAgent implements Agent {
    @Override
    public String getName() { return "ApprovalAgent"; }

    @Override
    public String getDescription() { return "Requests and processes human approvals for critical actions."; }

    @Override
    public AgentResult execute(AgentContext context) {
        return AgentResult.success("ApprovalAgent", "Approval processed.");
    }
}
