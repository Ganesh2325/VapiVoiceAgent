package com.voiceos.action.engine;

import com.voiceos.action.model.Action;
import com.voiceos.action.model.ActionStatus;
import com.voiceos.agent.core.Agent;
import com.voiceos.agent.core.AgentContext;
import com.voiceos.agent.core.AgentSelector;
import com.voiceos.service.PolicyDecision;
import com.voiceos.service.PolicyEngine;
import com.voiceos.service.PolicyRequest;
import com.voiceos.tool.core.Tool.ToolRiskLevel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class ActionPlanner {
    private static final Logger log = LoggerFactory.getLogger(ActionPlanner.class);

    private final PolicyEngine policyEngine;
    private final AgentSelector agentSelector;

    public ActionPlanner(PolicyEngine policyEngine, AgentSelector agentSelector) {
        this.policyEngine = policyEngine;
        this.agentSelector = agentSelector;
    }

    public void plan(Action action) {
        action.setStatus(ActionStatus.UNDERSTANDING);
        log.info("ActionPlanner: Planning action {}", action.getId());

        AgentContext context = AgentContext.of(action.getConversationId(), action.getUserId(), action.getIntent());
        String requestedTool = action.getToolName();
        agentSelector.select(context, requestedTool).ifPresentOrElse(
                (Agent agent) -> action.setTargetAgent(agent.getName()),
                () -> action.setTargetAgent("GeneralQueryAgent")
        );

        action.setStatus(ActionStatus.PLANNED);

        PolicyDecision decision = policyEngine.evaluate(new PolicyRequest(
                action.getUserId(),
                action.getActionType() != null ? action.getActionType() : action.getIntent(),
                action.getToolName(),
                ToolRiskLevel.LOW,
                action.getTargetProvider(),
                action.getPayload() != null ? action.getPayload() : Map.of()
        ));
        if (decision.isDeny()) {
            action.setStatus(ActionStatus.REJECTED);
            action.setErrorMessage(decision.reason());
        } else if (decision.isRequireApproval()) {
            action.setStatus(ActionStatus.REQUIRES_APPROVAL);
            action.setErrorMessage(decision.reason());
        } else {
            action.setStatus(ActionStatus.READY);
        }
    }
}
