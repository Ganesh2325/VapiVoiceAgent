package com.voiceos.domain.repository;

import com.voiceos.domain.entity.Document;
import com.voiceos.domain.entity.Document.DocumentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DocumentRepository extends JpaRepository<Document, UUID> {

    Page<Document> findByUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);

    List<Document> findByUserIdAndStatus(UUID userId, DocumentStatus status);

    Optional<Document> findByIdAndUserId(UUID id, UUID userId);
}
