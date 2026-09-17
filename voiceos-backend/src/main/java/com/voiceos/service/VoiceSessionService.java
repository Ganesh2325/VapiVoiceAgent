package com.voiceos.service;

import com.voiceos.domain.entity.User;
import com.voiceos.domain.entity.VoiceSession;
import com.voiceos.domain.repository.VoiceSessionRepository;
import com.voiceos.exception.VoiceOsException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
 * Binds a Vapi callId to an authenticated VoiceOS user.
 *
 * <p>Does not trust {@code customer.email}. Does not fall back to a default user.
 * Unbound callIds resolve to empty — ActionEngine then rejects the action.
 */
@Service
public class VoiceSessionService {

    private static final Logger log = LoggerFactory.getLogger(VoiceSessionService.class);
    static final Duration DEFAULT_TTL = Duration.ofHours(4);

    private final VoiceSessionRepository voiceSessionRepository;

    public VoiceSessionService(VoiceSessionRepository voiceSessionRepository) {
        this.voiceSessionRepository = voiceSessionRepository;
    }

    @Transactional
    public VoiceSession bind(User user, String callId, UUID conversationId) {
        if (user == null) {
            throw VoiceOsException.unauthorized("Authentication required to bind a voice session");
        }
        if (callId == null || callId.isBlank()) {
            throw VoiceOsException.badRequest("callId is required to bind a voice session");
        }

        Optional<VoiceSession> existing = voiceSessionRepository.findByCallId(callId.trim());
        if (existing.isPresent()) {
            VoiceSession session = existing.get();
            if (session.getUser() != null && !session.getUser().getId().equals(user.getId())) {
                throw VoiceOsException.forbidden("Voice session is bound to a different user");
            }
            session.setUser(user);
            session.setStatus(VoiceSession.Status.BOUND);
            session.setBoundAt(Instant.now());
            session.setExpiresAt(Instant.now().plus(DEFAULT_TTL));
            if (conversationId != null) {
                session.setConversationId(conversationId);
            }
            VoiceSession saved = voiceSessionRepository.save(session);
            log.info("Rebound voice session sessionId={} callId={} userId={}",
                    saved.getId(), callId, user.getId());
            return saved;
        }

        VoiceSession session = new VoiceSession();
        session.setCallId(callId.trim());
        session.setUser(user);
        session.setConversationId(conversationId);
        session.setStatus(VoiceSession.Status.BOUND);
        session.setBoundAt(Instant.now());
        session.setExpiresAt(Instant.now().plus(DEFAULT_TTL));
        VoiceSession saved = voiceSessionRepository.save(session);
        log.info("Bound voice session sessionId={} callId={} userId={}",
                saved.getId(), callId, user.getId());
        return saved;
    }

    @Transactional(readOnly = true)
    public Optional<VoiceSession> findByCallId(String callId) {
        if (callId == null || callId.isBlank()) {
            return Optional.empty();
        }
        return voiceSessionRepository.findByCallId(callId.trim());
    }

    /**
     * @return the bound user id for this Vapi call, or empty when unbound/expired/unknown
     */
    @Transactional(readOnly = true)
    public Optional<UUID> resolveBoundUserId(String callId) {
        return findByCallId(callId)
                .filter(VoiceSession::isBoundAndActive)
                .map(session -> session.getUser().getId());
    }

    @Transactional(readOnly = true)
    public Optional<UUID> resolveConversationId(String callId) {
        return findByCallId(callId)
                .filter(VoiceSession::isBoundAndActive)
                .map(VoiceSession::getConversationId);
    }
}
