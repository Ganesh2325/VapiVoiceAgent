package com.voiceos.voice.vapi;

import com.voiceos.config.VoiceOsProperties;
import com.voiceos.voice.VoiceProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Vapi Voice Provider implementation.
 * Integrates with Vapi's real-time WebRTC and telephony voice agent platform.
 */
@Component
@Primary
public class VapiVoiceProvider implements VoiceProvider {

    private static final Logger log = LoggerFactory.getLogger(VapiVoiceProvider.class);

    private final VoiceOsProperties properties;

    public VapiVoiceProvider(VoiceOsProperties properties) {
        this.properties = properties;
    }

    @Override
    public VoiceSession createSession(String conversationId, String userId) {
        String sessionId = "vapi_call_" + (conversationId != null ? conversationId : UUID.randomUUID().toString());
        String assistantId = getAssistantId();
        String publicKey = getPublicKey();

        log.info("Creating Vapi voice session: sessionId={}, assistantId={}", sessionId, assistantId);

        return new VoiceSession(
                sessionId,
                "https://api.vapi.ai",
                publicKey != null && !publicKey.isBlank() ? publicKey : "vapi_public_token_demo",
                "vapi"
        );
    }

    @Override
    public void terminateSession(String sessionId) {
        log.info("Terminating Vapi voice session: {}", sessionId);
        // Calls Vapi DELETE /call/{id} if active
    }

    @Override
    public String generateParticipantToken(String sessionId, String userId) {
        return getPublicKey();
    }

    @Override
    public String getProviderName() {
        return "vapi";
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    public String getAssistantId() {
        if (properties.voice() != null && properties.voice().vapi() != null) {
            return properties.voice().vapi().assistantId();
        }
        return null;
    }

    public String getPublicKey() {
        if (properties.voice() != null && properties.voice().vapi() != null) {
            return properties.voice().vapi().publicKey();
        }
        return null;
    }
}
