package com.voiceos.action;

import com.voiceos.action.model.ActionStep;
import com.voiceos.action.model.ActionStepStatus;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ActionStepTransitionTest {

    @Test
    void pendingToRunningToSucceeded() {
        ActionStep step = new ActionStep();
        assertEquals(ActionStepStatus.PENDING, step.getStatus());
        step.transitionTo(ActionStepStatus.RUNNING);
        step.transitionTo(ActionStepStatus.SUCCEEDED);
        assertEquals(ActionStepStatus.SUCCEEDED, step.getStatus());
    }

    @Test
    void runningToFailed() {
        ActionStep step = new ActionStep();
        step.transitionTo(ActionStepStatus.RUNNING);
        step.transitionTo(ActionStepStatus.FAILED);
        assertEquals(ActionStepStatus.FAILED, step.getStatus());
    }

    @Test
    void terminalCannotReturnToRunning() {
        ActionStep step = new ActionStep();
        step.transitionTo(ActionStepStatus.RUNNING);
        step.transitionTo(ActionStepStatus.SUCCEEDED);
        assertFalse(ActionStepStatus.SUCCEEDED.canTransitionTo(ActionStepStatus.RUNNING));
        assertThrows(IllegalStateException.class, () -> step.transitionTo(ActionStepStatus.RUNNING));
    }

    @Test
    void pendingMayWaitOrSkip() {
        assertTrue(ActionStepStatus.PENDING.canTransitionTo(ActionStepStatus.WAITING_USER));
        assertTrue(ActionStepStatus.PENDING.canTransitionTo(ActionStepStatus.SKIPPED));
        assertTrue(ActionStepStatus.PENDING.canTransitionTo(ActionStepStatus.CANCELLED));
    }
}
