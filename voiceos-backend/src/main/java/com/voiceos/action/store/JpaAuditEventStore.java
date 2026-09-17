package com.voiceos.action.store;

import com.voiceos.action.model.AuditEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public class JpaAuditEventStore implements AuditEventStore {

    private static final Logger log = LoggerFactory.getLogger(JpaAuditEventStore.class);
    private final AuditEventRepository repository;

    public JpaAuditEventStore(AuditEventRepository repository) {
        this.repository = repository;
    }

    @Override
    public AuditEvent append(AuditEvent event) {
        try {
            return repository.save(event);
        } catch (DataIntegrityViolationException duplicate) {
            log.info("Ignoring duplicate audit event key={}", event.getEventKey());
            return repository.findByEventKey(event.getEventKey()).orElse(event);
        }
    }

    @Override
    public List<AuditEvent> findByActionId(UUID actionId) {
        return repository.findByActionIdOrderByOccurredAtAscCreatedAtAsc(actionId);
    }
}
