package com.voiceos.voice.stub;

import com.voiceos.voice.VoiceProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Stub VoiceProvider for development.
 * Returns mock session data without connecting to any real voice infrastructure.
 * Used when VOICE_PROVIDER=stub (default in dev mode).
 */
@Component
public class StubVoiceProvider implements VoiceProvider {

    private static final Logger log = LoggerFactory.getLogger(StubVoiceProvider.class);

    @Override
    public VoiceSession createSession(String conversationId, String userId) {
        String sessionId = "stub-session-" + UUID.randomUUID();
        log.info("[STUB] Created voice session {} for conversation {}", sessionId, conversationId);
        return new VoiceSession(
                sessionId,
                "ws://localhost:7880",
                "stub-token-" + UUID.randomUUID(),
                "stub"
        );
    }

    @Override
    public void terminateSession(String sessionId) {
        log.info("[STUB] Terminated voice session {}", sessionId);
    }

    @Override
    public String generateParticipantToken(String sessionId, String userId) {
        log.info("[STUB] Generated participant token for session {} user {}", sessionId, userId);
        return "stub-participant-token-" + UUID.randomUUID();
    }

    @Override
    public String getProviderName() {
        return "stub";
    }

    @Override
    public boolean isAvailable() {
        return true;
    }
}
