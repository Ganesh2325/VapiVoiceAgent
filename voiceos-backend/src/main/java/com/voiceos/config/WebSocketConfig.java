package com.voiceos.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

import java.util.ArrayList;
import java.util.List;

/**
 * WebSocket configuration using STOMP over SockJS.
 *
 * <p>Allowed origins come from the same CORS list as REST. Wildcard {@code *}
 * is never used for authenticated application traffic.
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final VoiceOsProperties voiceOsProperties;

    public WebSocketConfig(VoiceOsProperties voiceOsProperties) {
        this.voiceOsProperties = voiceOsProperties;
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        config.enableSimpleBroker("/topic", "/queue");
        config.setApplicationDestinationPrefixes("/app");
        config.setUserDestinationPrefix("/user");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        List<String> origins = resolveAllowedOrigins();
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns(origins.toArray(String[]::new))
                .withSockJS();
    }

    private List<String> resolveAllowedOrigins() {
        List<String> configured = new ArrayList<>();
        if (voiceOsProperties.security() != null
                && voiceOsProperties.security().cors() != null
                && voiceOsProperties.security().cors().allowedOrigins() != null) {
            configured.addAll(voiceOsProperties.security().cors().allowedOrigins());
        }
        List<String> allowed = configured.stream()
                .filter(origin -> origin != null && !origin.isBlank())
                .map(String::trim)
                .filter(origin -> !"*".equals(origin))
                .distinct()
                .toList();
        if (allowed.isEmpty()) {
            return List.of("http://localhost:3000", "http://localhost:5173");
        }
        return allowed;
    }
}
