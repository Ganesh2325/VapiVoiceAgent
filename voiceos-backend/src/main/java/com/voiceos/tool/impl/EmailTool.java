package com.voiceos.tool.impl;

import com.voiceos.provider.ProviderMode;
import com.voiceos.provider.ProviderRequest;
import com.voiceos.provider.ProviderResults;
import com.voiceos.provider.email.EmailProvider;
import com.voiceos.tool.core.Tool;
import com.voiceos.tool.core.ToolResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Objects;

/**
 * Email Communication Tool.
 * Risk Level: HIGH — sending mutates external state and requires approval.
 * Delivery is MOCK in Phase 4.
 */
@Component
public class EmailTool implements Tool {

    private static final Logger log = LoggerFactory.getLogger(EmailTool.class);
    private final EmailProvider provider;

    public EmailTool(EmailProvider provider) {
        this.provider = Objects.requireNonNull(provider, "EmailProvider is required");
    }

    @Override
    public String getName() {
        return "send_email";
    }

    @Override
    public String getDescription() {
        return "[MOCK] Drafts and simulates sending email. Does not deliver real mail.";
    }

    @Override
    public ToolRiskLevel getRiskLevel() {
        return ToolRiskLevel.HIGH;
    }

    @Override
    public String getProviderName() {
        return provider.getName();
    }

    @Override
    public ProviderMode getProviderMode() {
        return provider.getMode();
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
        log.info("EmailTool simulating send via {} mode={}", provider.getName(), provider.getMode());
        return ProviderResults.toToolResult(provider.execute(new ProviderRequest("send", params, getTimeoutMs())));
    }
}
