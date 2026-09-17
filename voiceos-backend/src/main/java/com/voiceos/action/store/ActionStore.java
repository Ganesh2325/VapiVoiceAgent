package com.voiceos.action.store;

import com.voiceos.action.model.Action;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Persistence port for canonical actions. Implementations may be JPA or in-memory.
 */
public interface ActionStore {

    Action save(Action action);

    Optional<Action> findById(UUID id);

    Optional<Action> findByIdempotencyKey(String idempotencyKey);

    List<Action> findByUserId(UUID userId);

    Optional<Action> findByIdAndUserId(UUID id, UUID userId);
}
