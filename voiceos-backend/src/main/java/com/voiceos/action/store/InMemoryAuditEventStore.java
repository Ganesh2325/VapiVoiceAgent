package com.voiceos.action.store;

import com.voiceos.action.model.AuditEvent;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory append-only audit store for unit tests. Not a Spring bean.
 */
public class InMemoryAuditEventStore implements AuditEventStore {

    private final Map<String, AuditEvent> byKey = new ConcurrentHashMap<>();

    @Override
    public AuditEvent append(AuditEvent event) {
        AuditEvent previous = byKey.putIfAbsent(event.getEventKey(), event);
        return previous != null ? previous : event;
    }

    @Override
    public List<AuditEvent> findByActionId(UUID actionId) {
        return byKey.values().stream()
                .filter(event -> actionId.equals(event.getActionId()))
                .sorted(Comparator
                        .comparing(AuditEvent::getOccurredAt, Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(AuditEvent::getCreatedAt, Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();
    }
}
