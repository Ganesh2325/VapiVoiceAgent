package com.voiceos.action.audit;

import com.voiceos.action.model.Action;
import com.voiceos.action.model.ActionStep;
import com.voiceos.action.model.AuditEvent;
import com.voiceos.action.model.AuditEventType;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Frontend-friendly reconstruction of an action. Missing values are null, never invented.
 */
public record ActionTimelineDto(
        UUID actionId,
        String status,
        Instant createdAt,
        Instant completedAt,
        Long durationMs,
        String agent,
        String tool,
        String provider,
        String providerMode,
        String policyDecision,
        Boolean verificationPassed,
        String result,
        String error,
        List<StepView> steps,
        List<EventView> events,
        List<TimelineItem> timeline,
        String agentOutcome,
        List<String> missingFields
) {
    public record StepView(
            UUID stepId,
            int sequenceNo,
            String stepType,
            String status,
            Instant startedAt,
            Instant completedAt,
            Long durationMs,
            String agent,
            String tool,
            String provider,
            String providerMode,
            String errorCode,
            String errorMessage
    ) {}

    public record EventView(
            UUID eventId,
            String type,
            Instant timestamp,
            UUID stepId,
            String actorType,
            String actorId,
            String requestId,
            String message
    ) {}

    public record TimelineItem(
            Instant timestamp,
            String type,
            String stepType,
            String status,
            String message,
            String tool,
            String agent
    ) {}

    public static ActionTimelineDto from(Action action, List<ActionStep> steps, List<AuditEvent> events) {
        List<StepView> stepViews = steps.stream().map(ActionTimelineDto::toStep).toList();
        List<EventView> eventViews = events.stream().map(ActionTimelineDto::toEvent).toList();
        List<TimelineItem> timeline = new ArrayList<>();
        for (AuditEvent event : events) {
            ActionStep related = steps.stream()
                    .filter(step -> Objects.equals(step.getId(), event.getActionStepId()))
                    .findFirst()
                    .orElse(null);
            timeline.add(new TimelineItem(
                    event.getOccurredAt(),
                    event.getEventType() != null ? event.getEventType().name() : null,
                    related != null && related.getStepType() != null ? related.getStepType().name() : null,
                    related != null && related.getStatus() != null ? related.getStatus().name() : null,
                    messageFor(event),
                    related != null ? related.getToolName() : action.getToolName(),
                    related != null ? related.getAgentName() : action.getTargetAgent()
            ));
        }
        timeline.sort(Comparator.comparing(TimelineItem::timestamp, Comparator.nullsLast(Comparator.naturalOrder())));
        return new ActionTimelineDto(
                action.getId(),
                action.getStatus() != null ? action.getStatus().name() : null,
                action.getCreatedAt(),
                action.getCompletedAt(),
                action.getDurationMs(),
                action.getTargetAgent(),
                action.getToolName(),
                action.getTargetProvider(),
                action.getProviderMode(),
                policyDecision(events),
                action.getVerificationPassed(),
                action.getResult(),
                action.getErrorMessage(),
                stepViews,
                eventViews,
                timeline,
                payloadString(action, "agentOutcome"),
                payloadStringList(action, "missingFields")
        );
    }

    private static StepView toStep(ActionStep step) {
        return new StepView(
                step.getId(),
                step.getSequenceNo(),
                step.getStepType() != null ? step.getStepType().name() : null,
                step.getStatus() != null ? step.getStatus().name() : null,
                step.getStartedAt(),
                step.getCompletedAt(),
                step.getDurationMs(),
                step.getAgentName(),
                step.getToolName(),
                step.getProviderName(),
                step.getProviderMode(),
                step.getErrorCode(),
                step.getErrorMessage()
        );
    }

    private static EventView toEvent(AuditEvent event) {
        return new EventView(
                event.getId(),
                event.getEventType() != null ? event.getEventType().name() : null,
                event.getOccurredAt(),
                event.getActionStepId(),
                event.getActorType() != null ? event.getActorType().name() : null,
                event.getActorId(),
                event.getRequestId(),
                messageFor(event)
        );
    }

    private static String policyDecision(List<AuditEvent> events) {
        boolean denied = events.stream().anyMatch(e -> e.getEventType() == AuditEventType.POLICY_DENIED);
        if (denied) {
            return "DENY";
        }
        boolean approval = events.stream().anyMatch(e -> e.getEventType() == AuditEventType.APPROVAL_REQUIRED);
        if (approval) {
            return "REQUIRE_APPROVAL";
        }
        boolean evaluated = events.stream().anyMatch(e -> e.getEventType() == AuditEventType.POLICY_EVALUATED);
        if (evaluated) {
            Object outcome = events.stream()
                    .filter(e -> e.getEventType() == AuditEventType.POLICY_EVALUATED)
                    .map(e -> e.getMetadata().get("outcome"))
                    .filter(v -> v != null)
                    .findFirst()
                    .orElse(null);
            return outcome != null ? String.valueOf(outcome) : "ALLOW";
        }
        return null;
    }

    private static String messageFor(AuditEvent event) {
        if (event.getMetadata() != null && event.getMetadata().get("message") instanceof String message) {
            return message;
        }
        return event.getEventType() != null ? event.getEventType().name() : null;
    }

    private static String payloadString(Action action, String key) {
        if (action.getPayload() == null || action.getPayload().get(key) == null) {
            return null;
        }
        return String.valueOf(action.getPayload().get(key));
    }

    @SuppressWarnings("unchecked")
    private static List<String> payloadStringList(Action action, String key) {
        if (action.getPayload() == null) {
            return List.of();
        }
        Object value = action.getPayload().get(key);
        if (value instanceof List<?> list) {
            return list.stream().map(String::valueOf).toList();
        }
        return List.of();
    }
}
