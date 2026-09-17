package com.voiceos.action.store;

import com.voiceos.action.model.ActionStep;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ActionStepRepository extends JpaRepository<ActionStep, UUID> {

    List<ActionStep> findByActionIdOrderBySequenceNoAsc(UUID actionId);

    @Query("select coalesce(max(s.sequenceNo), 0) from ActionStep s where s.actionId = :actionId")
    int findMaxSequenceNo(UUID actionId);
}
