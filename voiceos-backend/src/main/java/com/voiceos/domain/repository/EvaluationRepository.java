package com.voiceos.domain.repository;

import com.voiceos.domain.entity.Evaluation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface EvaluationRepository extends JpaRepository<Evaluation, UUID> {

    Page<Evaluation> findByConversationIdOrderByCreatedAtDesc(UUID conversationId, Pageable pageable);

    @Query("SELECT AVG(e.toolSuccessRate) FROM Evaluation e WHERE e.conversation.user.id = :userId")
    Double findAverageToolSuccessRateByUserId(UUID userId);

    @Query("SELECT AVG(e.latencyMs) FROM Evaluation e WHERE e.conversation.user.id = :userId")
    Double findAverageLatencyByUserId(UUID userId);

    @Query("SELECT COUNT(e) FROM Evaluation e WHERE e.conversation.user.id = :userId AND e.taskSuccess = true")
    long countSuccessfulTasksByUserId(UUID userId);
}
