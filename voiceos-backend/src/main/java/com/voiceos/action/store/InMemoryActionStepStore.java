package com.voiceos.action.store;

import com.voiceos.action.model.ActionStep;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * In-memory steps for unit tests. Not a Spring bean.
 */
public class InMemoryActionStepStore implements ActionStepStore {

    private final Map<UUID, ActionStep> byId = new ConcurrentHashMap<>();
    private final Map<UUID, AtomicInteger> sequences = new ConcurrentHashMap<>();

    @Override
    public ActionStep save(ActionStep step) {
        byId.put(step.getId(), step);
        return step;
    }

    @Override
    public List<ActionStep> findByActionId(UUID actionId) {
        return byId.values().stream()
                .filter(step -> actionId.equals(step.getActionId()))
                .sorted(Comparator.comparingInt(ActionStep::getSequenceNo))
                .toList();
    }

    @Override
    public int nextSequence(UUID actionId) {
        return sequences.computeIfAbsent(actionId, id -> new AtomicInteger(0)).incrementAndGet();
    }
}
