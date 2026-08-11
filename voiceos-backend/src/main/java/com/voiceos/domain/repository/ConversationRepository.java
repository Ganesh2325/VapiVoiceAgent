package com.voiceos.domain.repository;

import com.voiceos.domain.entity.Conversation;
import com.voiceos.domain.entity.Conversation.ConversationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ConversationRepository extends JpaRepository<Conversation, UUID> {

    Page<Conversation> findByUserIdOrderByUpdatedAtDesc(UUID userId, Pageable pageable);

    List<Conversation> findByUserIdAndStatus(UUID userId, ConversationStatus status);

    Optional<Conversation> findByIdAndUserId(UUID id, UUID userId);

    long countByUserId(UUID userId);
}
