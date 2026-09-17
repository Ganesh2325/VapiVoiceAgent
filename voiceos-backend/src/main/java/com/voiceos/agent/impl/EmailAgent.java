package com.voiceos.agent.impl;

import com.voiceos.agent.core.Agent;
import com.voiceos.agent.core.AgentCapability;
import com.voiceos.agent.core.AgentContext;
import com.voiceos.agent.core.AgentRequest;
import com.voiceos.agent.core.AgentResult;
import com.voiceos.agent.core.AgentResult.ApprovalRequestData;
import com.voiceos.tool.core.Tool;
import com.voiceos.tool.core.ToolRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Email agent. Does not invent recipients. High-risk send still requires PolicyEngine.
 */
@Component
public class EmailAgent implements Agent {

    private static final Logger log = LoggerFactory.getLogger(EmailAgent.class);
    private final ToolRegistry toolRegistry;

    public EmailAgent(ToolRegistry toolRegistry) {
        this.toolRegistry = toolRegistry;
    }

    @Override
    public String getName() {
        return "EmailAgent";
    }

    @Override
    public String getDescription() {
        return "Prepares email sends when recipient, subject, and body are supplied. Does not invent contacts.";
    }

    @Override
    public boolean canHandle(AgentContext context) {
        String input = context.userInput() != null ? context.userInput().toLowerCase() : "";
        return input.contains("email") || input.contains("mail to") || input.contains("send a message to");
    }

    @Override
    public int getPriority() {
        return 25;
    }

    @Override
    public Set<AgentCapability> capabilities() {
        return Set.of(AgentCapability.EMAIL);
    }

    @Override
    public AgentResult execute(AgentRequest request) {
        long startMs = System.currentTimeMillis();
        log.info("EmailAgent evaluating email request without inventing recipients");
        List<String> missing = new ArrayList<>();
        String recipient = value(request, "recipient", "to");
        String subject = value(request, "subject");
        String body = value(request, "body", "message");
        if (recipient == null) {
            missing.add("recipient");
        }
        if (subject == null) {
            missing.add("subject");
        }
        if (body == null) {
            missing.add("body");
        }
        if (!missing.isEmpty()) {
            return AgentResult.needsInformation(getName(),
                    "Missing required information: " + String.join(", ", missing)
                            + ". EmailAgent does not invent recipients.",
                    missing,
                    System.currentTimeMillis() - startMs);
        }
        ApprovalRequestData approvalData = new ApprovalRequestData(
                "SEND_EMAIL",
                "Send email to " + recipient + " with subject: \"" + subject + "\"",
                "HIGH",
                Map.of("recipient", recipient, "subject", subject, "body", body)
        );
        return AgentResult.approvalRequired(getName(),
                "Email send requires user confirmation. PolicyEngine must still authorize send_email.",
                approvalData,
                System.currentTimeMillis() - startMs);
    }

    @Override
    public AgentResult execute(AgentContext context) {
        return execute(AgentRequest.from(context, "send_email"));
    }

    @Override
    public List<Tool> getTools() {
        List<Tool> tools = new ArrayList<>();
        toolRegistry.getTool("send_email").ifPresent(tools::add);
        return tools;
    }

    private static String value(AgentRequest request, String... keys) {
        if (request == null) {
            return null;
        }
        for (String key : keys) {
            String value = request.parameterAsString(key);
            if (value != null && !value.isBlank() && !"null".equalsIgnoreCase(value)) {
                return value;
            }
        }
        return null;
    }
}
