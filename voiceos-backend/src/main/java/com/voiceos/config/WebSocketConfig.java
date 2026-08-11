package com.voiceos.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * WebSocket configuration using STOMP over SockJS.
 *
 * <p>Clients connect to /ws and subscribe to topic destinations to
 * receive real-time execution events:
 * <ul>
 *   <li>/topic/executions/{conversationId} — execution events for a conversation</li>
 *   <li>/topic/approvals/{userId} — approval requests for a user</li>
 *   <li>/topic/system — system-wide status events</li>
 * </ul>
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // Enable simple in-memory broker for topics and queue destinations
        config.enableSimpleBroker("/topic", "/queue");
        // Prefix for messages bound for @MessageMapping methods
        config.setApplicationDestinationPrefixes("/app");
        // Prefix for user-specific messages
        config.setUserDestinationPrefix("/user");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*")  // CORS for WebSocket — tightened in production
                .withSockJS();                   // SockJS fallback for non-WebSocket browsers
    }
}
