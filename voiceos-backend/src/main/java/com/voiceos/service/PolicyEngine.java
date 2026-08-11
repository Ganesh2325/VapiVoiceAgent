package com.voiceos.service;

import com.voiceos.action.model.Action;
import com.voiceos.action.model.ActionStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class PolicyEngine {
    private static final Logger log = LoggerFactory.getLogger(PolicyEngine.class);

    /**
     * Evaluates action against system policies and returns a required pause state if applicable,
     * or null if the action can proceed to READY/EXECUTING.
     */
    public ActionStatus evaluatePolicy(Action action) {
        log.info("PolicyEngine: Evaluating action {}", action.getId());
        
        String intent = action.getIntent().toLowerCase();
        
        // Simulating policy rules
        if (intent.contains("pay") || intent.contains("book") || intent.contains("buy")) {
            log.warn("PolicyEngine: High risk action detected. Requires Payment.");
            return ActionStatus.REQUIRES_PAYMENT;
        }
        
        if (intent.contains("login") || intent.contains("authenticate")) {
            log.warn("PolicyEngine: Identity verification required.");
            return ActionStatus.REQUIRES_AUTHENTICATION;
        }

        if (intent.contains("email") || intent.contains("whatsapp") || intent.contains("message")) {
            log.info("PolicyEngine: Medium risk action. Requires Approval.");
            return ActionStatus.REQUIRES_APPROVAL;
        }

        return null;
    }
}
