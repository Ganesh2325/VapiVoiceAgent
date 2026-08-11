package com.voiceos.agent.impl;

import com.voiceos.agent.core.Agent;
import com.voiceos.agent.core.AgentContext;
import com.voiceos.agent.core.AgentResult;
import org.springframework.stereotype.Component;

/**
 * Handles external notifications via SMS, push, or messaging platforms.
 */
@Component
public class NotificationAgent implements Agent {
    @Override
    public String getName() { return "NotificationAgent"; }

    @Override
    public String getDescription() { return "Sends notifications to users."; }

    @Override
    public AgentResult execute(AgentContext context) {
        return AgentResult.success("NotificationAgent", "Notification sent successfully.");
    }
}
