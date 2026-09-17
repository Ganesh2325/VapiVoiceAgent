package com.voiceos.action.store;

import com.voiceos.action.model.Action;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory ActionStore for unit tests. Not a Spring bean.
 */
public class InMemoryActionStore implements ActionStore {

    private final Map<UUID, Action> byId = new ConcurrentHashMap<>();
    private final Map<String, UUID> byIdempotency = new ConcurrentHashMap<>();

    @Override
    public Action save(Action action) {
        byId.put(action.getId(), action);
        if (action.getIdempotencyKey() != null && !action.getIdempotencyKey().isBlank()) {
            byIdempotency.put(action.getIdempotencyKey(), action.getId());
        }
        return action;
    }

    @Override
    public Optional<Action> findById(UUID id) {
        return Optional.ofNullable(byId.get(id));
    }

    @Override
    public Optional<Action> findByIdempotencyKey(String idempotencyKey) {
        if (idempotencyKey == null) {
            return Optional.empty();
        }
        UUID id = byIdempotency.get(idempotencyKey);
        return id == null ? Optional.empty() : findById(id);
    }

    @Override
    public List<Action> findByUserId(UUID userId) {
        return byId.values().stream()
                .filter(action -> userId != null && userId.equals(action.getUserId()))
                .toList();
    }

    @Override
    public Optional<Action> findByIdAndUserId(UUID id, UUID userId) {
        return findById(id).filter(action -> userId != null && userId.equals(action.getUserId()));
    }
}
