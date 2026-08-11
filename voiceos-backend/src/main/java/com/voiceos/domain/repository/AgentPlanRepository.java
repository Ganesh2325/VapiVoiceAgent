package com.voiceos.domain.repository;

import com.voiceos.domain.entity.AgentPlan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface AgentPlanRepository extends JpaRepository<AgentPlan, UUID> {

    List<AgentPlan> findByConversationIdOrderByCreatedAtDesc(UUID conversationId);

    List<AgentPlan> findByUserIdOrderByCreatedAtDesc(UUID userId);
}
