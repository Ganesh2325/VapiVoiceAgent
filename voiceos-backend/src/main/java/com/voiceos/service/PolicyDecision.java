package com.voiceos.service;

/**
 * Structured policy outcome used as an execution gate.
 */
public record PolicyDecision(
        Outcome outcome,
        String reasonCode,
        String reason
) {
    public enum Outcome {
        ALLOW,
        DENY,
        REQUIRE_APPROVAL
    }

    public static PolicyDecision allow() {
        return new PolicyDecision(Outcome.ALLOW, "ALLOW", "Action is permitted");
    }

    public static PolicyDecision deny(String reasonCode, String reason) {
        return new PolicyDecision(Outcome.DENY, reasonCode, reason);
    }

    public static PolicyDecision requireApproval(String reasonCode, String reason) {
        return new PolicyDecision(Outcome.REQUIRE_APPROVAL, reasonCode, reason);
    }

    public boolean isAllow() {
        return outcome == Outcome.ALLOW;
    }

    public boolean isDeny() {
        return outcome == Outcome.DENY;
    }

    public boolean isRequireApproval() {
        return outcome == Outcome.REQUIRE_APPROVAL;
    }
}
