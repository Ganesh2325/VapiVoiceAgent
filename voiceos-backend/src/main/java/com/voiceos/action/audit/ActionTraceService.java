package com.voiceos.action.audit;

import com.voiceos.action.model.Action;
import com.voiceos.action.model.ActionStep;
import com.voiceos.action.model.ActionStepStatus;
import com.voiceos.action.model.ActionStepType;
import com.voiceos.action.model.AuditActorType;
import com.voiceos.action.model.AuditEvent;
import com.voiceos.action.model.AuditEventType;
import com.voiceos.action.store.ActionStepStore;
import com.voiceos.action.store.AuditEventStore;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Observes ActionEngine execution. Does not decide whether a tool runs.
 */
@Service
public class ActionTraceService {

    private final ActionStepStore actionStepStore;
    private final AuditEventStore auditEventStore;

    public ActionTraceService(ActionStepStore actionStepStore, AuditEventStore auditEventStore) {
        this.actionStepStore = actionStepStore;
        this.auditEventStore = auditEventStore;
    }

    public ActionStep startStep(Action action, ActionStepType type) {
        ActionStep step = new ActionStep();
        step.setActionId(action.getId());
        step.setSequenceNo(actionStepStore.nextSequence(action.getId()));
        step.setStepType(type);
        step.setStatus(ActionStepStatus.PENDING);
        step.setRequestId(action.getRequestId());
        step.setAgentName(action.getTargetAgent());
        step.setToolName(action.getToolName());
        step.setProviderName(action.getTargetProvider());
        step.setProviderMode(action.getProviderMode());
        step.transitionTo(ActionStepStatus.RUNNING);
        step.setStartedAt(Instant.now());
        return actionStepStore.save(step);
    }

    public ActionStep succeed(ActionStep step, Map<String, Object> result) {
        if (step == null) {
            return null;
        }
        step.transitionTo(ActionStepStatus.SUCCEEDED);
        step.setResult(SensitiveDataRedactor.redact(result));
        complete(step);
        return actionStepStore.save(step);
    }

    public ActionStep fail(ActionStep step, String errorCode, String errorMessage) {
        if (step == null) {
            return null;
        }
        step.transitionTo(ActionStepStatus.FAILED);
        step.setErrorCode(errorCode);
        step.setErrorMessage(errorMessage);
        complete(step);
        return actionStepStore.save(step);
    }

    public ActionStep waitUser(ActionStep step, String reason) {
        if (step == null) {
            return null;
        }
        step.transitionTo(ActionStepStatus.WAITING_USER);
        step.setErrorMessage(reason);
        complete(step);
        return actionStepStore.save(step);
    }

    public void emit(Action action, ActionStep step, AuditEventType type, AuditActorType actor, Map<String, Object> metadata) {
        AuditEvent event = new AuditEvent();
        event.setActionId(action.getId());
        event.setActionStepId(step != null ? step.getId() : null);
        event.setEventType(type);
        event.setEventKey(eventKey(action.getId(), type, step));
        event.setOccurredAt(Instant.now());
        event.setRequestId(action.getRequestId());
        event.setCallId(action.getCallId());
        event.setUserId(action.getUserId());
        event.setActorType(actor != null ? actor : AuditActorType.SYSTEM);
        event.setActorId(actorId(action, actor));
        event.setMetadata(SensitiveDataRedactor.redact(metadata));
        auditEventStore.append(event);
    }

    public List<ActionStep> stepsFor(UUID actionId) {
        return actionStepStore.findByActionId(actionId);
    }

    public List<AuditEvent> eventsFor(UUID actionId) {
        return auditEventStore.findByActionId(actionId);
    }

    public ActionTimelineDto timeline(Action action) {
        List<ActionStep> steps = stepsFor(action.getId());
        List<AuditEvent> events = eventsFor(action.getId());
        return ActionTimelineDto.from(action, steps, events);
    }

    static String eventKey(UUID actionId, AuditEventType type, ActionStep step) {
        String stepPart = step != null ? step.getId().toString() : "action";
        return actionId + ":" + type.name() + ":" + stepPart;
    }

    private static String actorId(Action action, AuditActorType actor) {
        if (actor == null) {
            return null;
        }
        return switch (actor) {
            case USER -> action.getUserId() != null ? action.getUserId().toString() : null;
            case VAPI -> action.getCallId();
            case AGENT -> action.getTargetAgent();
            case TOOL -> action.getToolName();
            case PROVIDER -> action.getTargetProvider();
            case SYSTEM -> "ActionEngine";
        };
    }

    private static void complete(ActionStep step) {
        Instant now = Instant.now();
        step.setCompletedAt(now);
        if (step.getStartedAt() != null) {
            step.setDurationMs(Math.max(0, now.toEpochMilli() - step.getStartedAt().toEpochMilli()));
        }
        Map<String, Object> meta = new LinkedHashMap<>(step.getMetadata());
        step.setMetadata(SensitiveDataRedactor.redact(meta));
    }
}
