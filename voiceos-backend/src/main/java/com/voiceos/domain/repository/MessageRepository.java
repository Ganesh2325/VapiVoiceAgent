package com.voiceos.domain.repository;

import com.voiceos.domain.entity.Message;
import com.voiceos.domain.entity.Message.MessageRole;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface MessageRepository extends JpaRepository<Message, UUID> {

    List<Message> findByConversationIdOrderByCreatedAtAsc(UUID conversationId);

    @Query("SELECT m FROM Message m WHERE m.conversation.id = :conversationId ORDER BY m.createdAt DESC")
    List<Message> findRecentByConversationId(UUID conversationId, Pageable pageable);

    List<Message> findByConversationIdAndRole(UUID conversationId, MessageRole role);

    long countByConversationId(UUID conversationId);
}
