package com.voiceos.domain.repository;

import com.voiceos.domain.entity.AgentExecution;
import com.voiceos.domain.entity.AgentExecution.ExecutionStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface AgentExecutionRepository extends JpaRepository<AgentExecution, UUID> {

    Page<AgentExecution> findByUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);

    List<AgentExecution> findByConversationIdOrderByCreatedAtAsc(UUID conversationId);

    List<AgentExecution> findByUserIdAndStatus(UUID userId, ExecutionStatus status);

    long countByUserIdAndStatus(UUID userId, ExecutionStatus status);
}
