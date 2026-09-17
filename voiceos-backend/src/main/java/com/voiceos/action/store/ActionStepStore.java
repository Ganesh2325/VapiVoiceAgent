package com.voiceos.action.store;

import com.voiceos.action.model.ActionStep;

import java.util.List;
import java.util.UUID;

public interface ActionStepStore {

    ActionStep save(ActionStep step);

    List<ActionStep> findByActionId(UUID actionId);

    int nextSequence(UUID actionId);
}
