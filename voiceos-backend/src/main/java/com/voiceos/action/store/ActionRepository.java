package com.voiceos.action.store;

import com.voiceos.action.model.Action;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ActionRepository extends JpaRepository<Action, UUID> {

    Optional<Action> findByIdempotencyKey(String idempotencyKey);

    List<Action> findByUserIdOrderByCreatedAtDesc(UUID userId);

    Optional<Action> findByIdAndUserId(UUID id, UUID userId);
}
