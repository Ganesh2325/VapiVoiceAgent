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
 * Placeholder for a future policy explanation agent.
 * Does not grant permissions and does not claim that a security check passed.
 */
@Component
public class SecurityAgent implements Agent {

    @Override
    public String getName() {
        return "SecurityAgent";
    }

    @Override
    public String getDescription() {
        return "Reserved for explaining security boundaries. PolicyEngine is not invoked from this agent.";
    }

    @Override
    public boolean canHandle(AgentContext context) {
        if (context == null || context.userInput() == null) {
            return false;
        }
        String input = context.userInput().toLowerCase();
        return input.contains("security policy") || input.contains("am i authorized");
    }

    @Override
    public int getPriority() {
        return 70;
    }

    @Override
    public Set<AgentCapability> capabilities() {
        return Set.of(AgentCapability.SECURITY);
    }

    @Override
    public AgentResult execute(AgentContext context) {
        long startMs = System.currentTimeMillis();
        return AgentResult.notImplemented(getName(),
                "NOT_IMPLEMENTED: SecurityAgent does not evaluate or approve access. "
                        + "It does not report a passed security check.",
                System.currentTimeMillis() - startMs);
    }

    @Override
    public List<Tool> getTools() {
        return List.of();
    }
}
