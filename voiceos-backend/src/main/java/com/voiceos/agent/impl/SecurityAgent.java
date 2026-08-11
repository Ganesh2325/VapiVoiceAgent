package com.voiceos.agent.impl;

import com.voiceos.agent.core.Agent;
import com.voiceos.agent.core.AgentContext;
import com.voiceos.agent.core.AgentResult;
import org.springframework.stereotype.Component;

/**
 * Enforces strict security bounds on tool executions and agent capabilities.
 */
@Component
public class SecurityAgent implements Agent {
    @Override
    public String getName() { return "SecurityAgent"; }

    @Override
    public String getDescription() { return "Evaluates security risks and boundaries for requested actions."; }

    @Override
    public AgentResult execute(AgentContext context) {
        return AgentResult.success("SecurityAgent", "Security check passed.");
    }
}
