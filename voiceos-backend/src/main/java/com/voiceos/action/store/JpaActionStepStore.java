package com.voiceos.action.store;

import com.voiceos.action.model.ActionStep;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public class JpaActionStepStore implements ActionStepStore {

    private final ActionStepRepository repository;

    public JpaActionStepStore(ActionStepRepository repository) {
        this.repository = repository;
    }

    @Override
    public ActionStep save(ActionStep step) {
        return repository.save(step);
    }

    @Override
    public List<ActionStep> findByActionId(UUID actionId) {
        return repository.findByActionIdOrderBySequenceNoAsc(actionId);
    }

    @Override
    public int nextSequence(UUID actionId) {
        return repository.findMaxSequenceNo(actionId) + 1;
    }
}
