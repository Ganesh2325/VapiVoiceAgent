package com.voiceos.action.store;

import com.voiceos.action.model.AuditEvent;

import java.util.List;
import java.util.UUID;

public interface AuditEventStore {

    /**
     * Append-only insert. Duplicate {@code eventKey} is ignored (idempotent replay).
     */
    AuditEvent append(AuditEvent event);

    List<AuditEvent> findByActionId(UUID actionId);
}
