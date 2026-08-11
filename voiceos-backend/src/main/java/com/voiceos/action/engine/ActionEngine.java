package com.voiceos.action.engine;

import com.voiceos.action.model.Action;
import com.voiceos.action.model.ActionStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Universal Action Engine.
 * Manages the generic lifecycle of all real-world actions.
 */
@Service
public class ActionEngine {
    private static final Logger log = LoggerFactory.getLogger(ActionEngine.class);

    private final ActionPlanner actionPlanner;
    private final ActionExecutor actionExecutor;
    private final Map<UUID, Action> actionStore = new ConcurrentHashMap<>();

    public ActionEngine(ActionPlanner actionPlanner, ActionExecutor actionExecutor) {
        this.actionPlanner = actionPlanner;
        this.actionExecutor = actionExecutor;
    }

    public Action initiateAction(UUID userId, String intent, Map<String, Object> initialData) {
        Action action = new Action(userId, intent);
        action.setPayload(initialData);
        action.setStatus(ActionStatus.REQUESTED);
        actionStore.put(action.getId(), action);
        log.info("ActionEngine: Action {} initiated for intent '{}'", action.getId(), intent);

        processAction(action);
        return action;
    }

    public void processAction(Action action) {
        switch (action.getStatus()) {
            case REQUESTED:
            case UNDERSTANDING:
                actionPlanner.plan(action);
                actionStore.put(action.getId(), action);
                processAction(action);
                break;
            case PLANNED:
            case READY:
            case REQUIRES_APPROVAL:
            case REQUIRES_AUTHENTICATION:
            case REQUIRES_PAYMENT:
                // Typically waits for external user input or approval service callback.
                log.info("Action {} is pausing in state {}", action.getId(), action.getStatus());
                break;
            case AUTHENTICATING:
            case EXECUTING:
                actionExecutor.execute(action);
                actionStore.put(action.getId(), action);
                processAction(action);
                break;
            case VERIFYING:
                // Handled in Executor/Verifier
                break;
            case COMPLETED:
            case FAILED:
            case CANCELLED:
                log.info("Action {} finished with state {}", action.getId(), action.getStatus());
                break;
            default:
                break;
        }
    }

    public Action getAction(UUID actionId) {
        return actionStore.get(actionId);
    }

    public void resumeAction(UUID actionId, ActionStatus newStatus, Map<String, Object> newPayload) {
        Action action = actionStore.get(actionId);
        if (action != null) {
            action.setStatus(newStatus);
            if (newPayload != null) {
                action.getPayload().putAll(newPayload);
            }
            log.info("Resuming Action {} from status {}", action.getId(), newStatus);
            processAction(action);
        }
    }
}
