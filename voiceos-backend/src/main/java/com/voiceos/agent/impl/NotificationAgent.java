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
 * Placeholder for a future NotificationEngine.
 * Does not send email, WhatsApp, SMS, or push notifications.
 */
@Component
public class NotificationAgent implements Agent {

    @Override
    public String getName() {
        return "NotificationAgent";
    }

    @Override
    public String getDescription() {
        return "Reserved for multi-channel notifications. NotificationEngine is not implemented.";
    }

    @Override
    public boolean canHandle(AgentContext context) {
        if (context == null || context.userInput() == null) {
            return false;
        }
        String input = context.userInput().toLowerCase();
        return input.contains("send notification") || input.contains("notify me via");
    }

    @Override
    public int getPriority() {
        return 85;
    }

    @Override
    public Set<AgentCapability> capabilities() {
        return Set.of(AgentCapability.NOTIFICATION);
    }

    @Override
    public AgentResult execute(AgentContext context) {
        long startMs = System.currentTimeMillis();
        return AgentResult.notImplemented(getName(),
                "NOT_IMPLEMENTED: NotificationAgent does not send messages. "
                        + "No NotificationEngine is wired. Channels EMAIL/WHATSAPP/VAPI/FRONTEND are not dispatched.",
                System.currentTimeMillis() - startMs);
    }

    @Override
    public List<Tool> getTools() {
        return List.of();
    }
}
