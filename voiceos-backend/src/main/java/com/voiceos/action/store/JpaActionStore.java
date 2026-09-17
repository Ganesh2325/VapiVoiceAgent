package com.voiceos.action.store;

import com.voiceos.action.model.Action;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class JpaActionStore implements ActionStore {

    private final ActionRepository actionRepository;

    public JpaActionStore(ActionRepository actionRepository) {
        this.actionRepository = actionRepository;
    }

    @Override
    public Action save(Action action) {
        return actionRepository.save(action);
    }

    @Override
    public Optional<Action> findById(UUID id) {
        return actionRepository.findById(id);
    }

    @Override
    public Optional<Action> findByIdempotencyKey(String idempotencyKey) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            return Optional.empty();
        }
        return actionRepository.findByIdempotencyKey(idempotencyKey);
    }

    @Override
    public List<Action> findByUserId(UUID userId) {
        return actionRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    @Override
    public Optional<Action> findByIdAndUserId(UUID id, UUID userId) {
        return actionRepository.findByIdAndUserId(id, userId);
    }
}
