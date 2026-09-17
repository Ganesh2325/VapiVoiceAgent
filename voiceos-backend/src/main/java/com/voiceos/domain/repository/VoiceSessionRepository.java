package com.voiceos.domain.repository;

import com.voiceos.domain.entity.VoiceSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface VoiceSessionRepository extends JpaRepository<VoiceSession, UUID> {

    Optional<VoiceSession> findByCallId(String callId);
}
