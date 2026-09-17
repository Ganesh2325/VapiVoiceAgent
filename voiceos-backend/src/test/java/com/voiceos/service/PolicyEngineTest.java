package com.voiceos.service;

import com.voiceos.tool.core.Tool.ToolRiskLevel;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PolicyEngineTest {

    private final PolicyEngine policyEngine = new PolicyEngine();
    private final UUID userId = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");

    @Test
    void calculatorIsAllowed() {
        PolicyDecision decision = policyEngine.evaluate(new PolicyRequest(
                userId, "calculator", "calculator", ToolRiskLevel.LOW, "RealLocalCalculatorProvider", Map.of()
        ));
        assertTrue(decision.isAllow());
    }

    @Test
    void forbiddenToolIsDenied() {
        PolicyDecision decision = policyEngine.evaluate(new PolicyRequest(
                userId, "forbidden", "forbidden", ToolRiskLevel.LOW, "none", Map.of()
        ));
        assertTrue(decision.isDeny());
        assertEquals("FORBIDDEN_TOOL", decision.reasonCode());
    }

    @Test
    void highRiskRequiresApproval() {
        PolicyDecision decision = policyEngine.evaluate(new PolicyRequest(
                userId, "send_email", "send_email", ToolRiskLevel.HIGH, "none", Map.of()
        ));
        assertTrue(decision.isRequireApproval());
    }

    @Test
    void missingUserIsDenied() {
        PolicyDecision decision = policyEngine.evaluate(new PolicyRequest(
                null, "calculator", "calculator", ToolRiskLevel.LOW, "local", Map.of()
        ));
        assertTrue(decision.isDeny());
    }
}
