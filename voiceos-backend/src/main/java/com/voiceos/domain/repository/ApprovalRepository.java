package com.voiceos.domain.repository;

import com.voiceos.domain.entity.Approval;
import com.voiceos.domain.entity.Approval.ApprovalStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ApprovalRepository extends JpaRepository<Approval, UUID> {

    Page<Approval> findByUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);

    List<Approval> findByUserIdAndStatus(UUID userId, ApprovalStatus status);

    Optional<Approval> findByIdAndUserId(UUID id, UUID userId);

    List<Approval> findByConversationIdAndStatus(UUID conversationId, ApprovalStatus status);

    @Modifying
    @Query("UPDATE Approval a SET a.status = 'EXPIRED' WHERE a.status = 'PENDING' AND a.expiresAt < :now")
    int expireOverdueApprovals(Instant now);

    long countByUserIdAndStatus(UUID userId, ApprovalStatus status);
}
