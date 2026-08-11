package com.voiceos.domain.repository;

import com.voiceos.domain.entity.Memory;
import com.voiceos.domain.entity.Memory.MemoryType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MemoryRepository extends JpaRepository<Memory, UUID> {

    List<Memory> findByUserIdAndMemoryType(UUID userId, MemoryType memoryType);

    Optional<Memory> findByUserIdAndKey(UUID userId, String key);

    List<Memory> findByUserIdOrderByUpdatedAtDesc(UUID userId);

    @Query("SELECT m FROM Memory m WHERE m.user.id = :userId AND m.memoryType = :type AND (m.expiresAt IS NULL OR m.expiresAt > :now)")
    List<Memory> findActiveByUserIdAndType(UUID userId, MemoryType type, Instant now);

    @Modifying
    @Query("DELETE FROM Memory m WHERE m.expiresAt IS NOT NULL AND m.expiresAt < :now")
    int deleteExpiredMemories(Instant now);
}
