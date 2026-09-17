package com.voiceos.action.store;

import com.voiceos.action.model.AuditEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AuditEventRepository extends JpaRepository<AuditEvent, UUID> {

    List<AuditEvent> findByActionIdOrderByOccurredAtAscCreatedAtAsc(UUID actionId);

    Optional<AuditEvent> findByEventKey(String eventKey);
}
