package com.voiceos.service;

import com.voiceos.action.model.Action;
import com.voiceos.action.model.ActionStatus;
import com.voiceos.tool.core.Tool;
import com.voiceos.tool.core.Tool.ToolRiskLevel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Locale;
import java.util.Set;

/**
 * Execution gate for the canonical pipeline.
 *
 * <p>Decisions use structured action type, tool name, and declared risk — not
 * free-text keyword matching. This is the Phase 2 policy architecture, not a
 * complete authorization product.
 */
@Service
public class PolicyEngine {

    private static final Logger log = LoggerFactory.getLogger(PolicyEngine.class);

    static final Set<String> DENIED_TOOLS = Set.of("forbidden", "payment_charge");

    public PolicyDecision evaluate(PolicyRequest request) {
        if (request == null || request.userId() == null) {
            return PolicyDecision.deny("UNAUTHENTICATED", "Authenticated user is required");
        }

        String toolName = normalize(request.toolName());
        if (DENIED_TOOLS.contains(toolName)) {
            log.info("Policy DENY tool={} reasonCode=FORBIDDEN_TOOL userId={}", toolName, request.userId());
            return PolicyDecision.deny("FORBIDDEN_TOOL", "Tool is not permitted: " + toolName);
        }

        ToolRiskLevel risk = request.riskLevel() != null ? request.riskLevel() : inferRisk(toolName, request.actionType());
        if (risk == ToolRiskLevel.HIGH || risk == ToolRiskLevel.CRITICAL) {
            log.info("Policy REQUIRE_APPROVAL tool={} risk={} userId={}", toolName, risk, request.userId());
            return PolicyDecision.requireApproval(
                    "RISK_" + risk.name(),
                    "Tool risk " + risk.name() + " requires human approval"
            );
        }

        log.info("Policy ALLOW tool={} risk={} userId={}", toolName, risk, request.userId());
        return PolicyDecision.allow();
    }

    /**
     * Adapter for the legacy ActionPlanner pause-state contract.
     */
    public ActionStatus evaluatePolicy(Action action) {
        if (action == null) {
            return ActionStatus.REJECTED;
        }
        PolicyDecision decision = evaluate(new PolicyRequest(
                action.getUserId(),
                action.getActionType() != null ? action.getActionType() : action.getIntent(),
                action.getToolName(),
                inferRisk(action.getToolName(), action.getIntent()),
                action.getTargetProvider(),
                action.getPayload()
        ));
        return switch (decision.outcome()) {
            case ALLOW -> null;
            case DENY -> ActionStatus.REJECTED;
            case REQUIRE_APPROVAL -> ActionStatus.REQUIRES_APPROVAL;
        };
    }

    public static ToolRiskLevel inferRisk(String toolName, String actionType) {
        String tool = normalize(toolName);
        String type = normalize(actionType);
        if (DENIED_TOOLS.contains(tool) || type.contains("payment") || type.contains("charge")) {
            return ToolRiskLevel.CRITICAL;
        }
        if (tool.contains("email") || tool.contains("whatsapp") || type.contains("email") || type.contains("whatsapp")) {
            return ToolRiskLevel.HIGH;
        }
        if (tool.contains("book") || type.contains("book") || type.contains("travel")) {
            return ToolRiskLevel.HIGH;
        }
        if (tool.contains("task") || tool.contains("memory_save") || tool.contains("memory_delete")) {
            return ToolRiskLevel.MEDIUM;
        }
        return ToolRiskLevel.LOW;
    }

    private static String normalize(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT).trim();
    }
}
