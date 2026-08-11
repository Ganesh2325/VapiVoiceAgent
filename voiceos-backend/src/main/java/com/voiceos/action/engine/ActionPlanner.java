package com.voiceos.action.engine;

import com.voiceos.action.model.Action;
import com.voiceos.action.model.ActionStatus;
import com.voiceos.service.PolicyEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class ActionPlanner {
    private static final Logger log = LoggerFactory.getLogger(ActionPlanner.class);
    
    private final PolicyEngine policyEngine;

    public ActionPlanner(PolicyEngine policyEngine) {
        this.policyEngine = policyEngine;
    }

    public void plan(Action action) {
        action.setStatus(ActionStatus.UNDERSTANDING);
        log.info("ActionPlanner: Understanding intent {}", action.getIntent());
        
        // Mocking logic to determine provider and agent based on intent
        if (action.getIntent().toLowerCase().contains("book") || action.getIntent().toLowerCase().contains("flight")) {
            action.setTargetAgent("TravelAgent");
            action.setTargetProvider("TravelProvider");
        } else if (action.getIntent().toLowerCase().contains("whatsapp") || action.getIntent().toLowerCase().contains("message")) {
            action.setTargetAgent("WhatsAppAgent");
            action.setTargetProvider("WhatsAppProvider");
        } else {
            action.setTargetAgent("ConversationAgent");
        }
        
        action.setStatus(ActionStatus.PLANNED);
        log.info("ActionPlanner: Plan generated. Target Agent: {}, Target Provider: {}", action.getTargetAgent(), action.getTargetProvider());
        
        // Let policy engine assess
        ActionStatus requiredPause = policyEngine.evaluatePolicy(action);
        if (requiredPause != null) {
            action.setStatus(requiredPause);
        } else {
            action.setStatus(ActionStatus.READY);
        }
    }
}
