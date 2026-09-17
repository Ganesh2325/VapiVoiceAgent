package com.voiceos.action.model;

/**
 * Lifecycle of a single {@link ActionStep}. Terminal states cannot return to RUNNING.
 */
public enum ActionStepStatus {
    PENDING,
    RUNNING,
    SUCCEEDED,
    FAILED,
    SKIPPED,
    WAITING_USER,
    CANCELLED;

    public boolean isTerminal() {
        return this == SUCCEEDED || this == FAILED || this == SKIPPED || this == CANCELLED;
    }

    public boolean canTransitionTo(ActionStepStatus target) {
        if (target == null) {
            return false;
        }
        if (this == target) {
            return true;
        }
        if (isTerminal()) {
            return false;
        }
        return switch (this) {
            case PENDING -> target == RUNNING
                    || target == WAITING_USER
                    || target == SKIPPED
                    || target == CANCELLED;
            case RUNNING -> target == SUCCEEDED
                    || target == FAILED
                    || target == WAITING_USER
                    || target == CANCELLED;
            case WAITING_USER -> target == RUNNING
                    || target == SUCCEEDED
                    || target == FAILED
                    || target == SKIPPED
                    || target == CANCELLED;
            default -> false;
        };
    }
}
