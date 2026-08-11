package com.voiceos.agent.impl;

import com.voiceos.agent.core.Agent;
import com.voiceos.agent.core.AgentContext;
import com.voiceos.agent.core.AgentResult;
import com.voiceos.agent.core.AgentResult.ApprovalRequestData;
import com.voiceos.tool.core.Tool;
import com.voiceos.tool.core.ToolRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * Email Communication Agent.
 * Drafts emails and requests human approval before sending (Risk Level: HIGH).
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
        return "Prepares and sends emails. Requires explicit user approval before sending.";
    }

    @Override
    public boolean canHandle(AgentContext context) {
        String input = context.userInput().toLowerCase();
        return input.contains("email") || input.contains("mail to") || input.contains("send a message to");
    }

    @Override
    public int getPriority() {
        return 25;
    }

    @Override
    public AgentResult execute(AgentContext context) {
        long startMs = System.currentTimeMillis();
        String input = context.userInput();
        log.info("EmailAgent preparing email for: '{}'", input);

        // Extract recipient from input or fallback to demo contact
        String recipient = "john@example.com";
        String subject = "Project Update & Follow-up";
        String body = "Hi John,\n\nI wanted to follow up on our previous discussion regarding the VoiceOS architecture and roadmap.\n\nBest regards,\nVoiceOS Assistant";

        if (input.toLowerCase().contains("ganesh")) {
            recipient = "ganeshmaheshwaram@gmail.com";
        }

        ApprovalRequestData approvalData = new ApprovalRequestData(
                "SEND_EMAIL",
                "Send email to " + recipient + " with subject: \"" + subject + "\"",
                "HIGH",
                Map.of(
                        "recipient", recipient,
                        "subject", subject,
                        "body", body
                )
        );

        String explanation = """
        ⚠️ **Action Requires Approval**
        
        - **Action:** Send Email
        - **Recipient:** %s
        - **Subject:** %s
        - **Body Preview:**
        > %s
        
        *Please click Approve in the Approvals queue or reply with 'Approve' to send this email.*
        """.formatted(recipient, subject, body.replace("\n", "\n> "));

        long latency = System.currentTimeMillis() - startMs;
        return AgentResult.approvalRequired(getName(), explanation, approvalData, latency);
    }

    @Override
    public List<Tool> getTools() {
        return toolRegistry.getAllTools().stream()
                .filter(t -> t.getName().equals("send_email"))
                .toList();
    }
}
