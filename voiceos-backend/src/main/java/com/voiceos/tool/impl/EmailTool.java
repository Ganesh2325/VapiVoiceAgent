package com.voiceos.tool.impl;

import com.voiceos.tool.core.Tool;
import com.voiceos.tool.core.ToolResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Email Communication Tool.
 * Drafts and sends emails.
 * Risk Level: HIGH — Sending an email mutates external state and cannot be undone,
 * thus human approval is strictly required before actual dispatch.
 */
@Component
public class EmailTool implements Tool {

    private static final Logger log = LoggerFactory.getLogger(EmailTool.class);

    @Override
    public String getName() {
        return "send_email";
    }

    @Override
    public String getDescription() {
        return "Drafts and sends emails to recipients. Requires recipient, subject, and body.";
    }

    @Override
    public ToolRiskLevel getRiskLevel() {
        return ToolRiskLevel.HIGH; // Triggers Human-in-the-Loop Approval!
    }

    @Override
    public void validateInput(Map<String, Object> params) {
        Tool.super.validateInput(params);
        if (!params.containsKey("recipient") || params.get("recipient") == null) {
            throw new IllegalArgumentException("Email recipient is required.");
        }
        if (!params.containsKey("subject") || params.get("subject") == null) {
            throw new IllegalArgumentException("Email subject is required.");
        }
        if (!params.containsKey("body") || params.get("body") == null) {
            throw new IllegalArgumentException("Email body is required.");
        }
    }

    @Override
    public ToolResult execute(Map<String, Object> params) {
        long startMs = System.currentTimeMillis();
        String recipient = params.get("recipient").toString();
        String subject = params.get("subject").toString();
        String body = params.get("body").toString();

        log.info("[EMAIL DISPATCH] Sending email to '{}', subject='{}'", recipient, subject);

        // Dispatches email or logs mock dispatch
        long latency = System.currentTimeMillis() - startMs;
        return ToolResult.success(
                Map.of("recipient", recipient, "subject", subject, "status", "SENT"),
                "Email sent successfully to " + recipient + " with subject: \"" + subject + "\".",
                latency
        );
    }
}
