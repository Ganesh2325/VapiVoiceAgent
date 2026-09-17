package com.voiceos.api.controller;

import com.voiceos.domain.entity.Conversation;
import com.voiceos.domain.entity.Message;
import com.voiceos.domain.entity.User;
import com.voiceos.domain.repository.ConversationRepository;
import com.voiceos.domain.repository.MessageRepository;
import com.voiceos.exception.VoiceOsException;
import com.voiceos.security.AuthenticatedUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * REST API for Conversation management.
 */
@RestController
@RequestMapping("/api/v1/conversations")
@Tag(name = "Conversations", description = "Conversation lifecycle and history")
@SecurityRequirement(name = "bearerAuth")
public class ConversationController {

    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;
    private final AuthenticatedUserService authenticatedUserService;

    public ConversationController(
            ConversationRepository conversationRepository,
            MessageRepository messageRepository,
            AuthenticatedUserService authenticatedUserService
    ) {
        this.conversationRepository = conversationRepository;
        this.messageRepository = messageRepository;
        this.authenticatedUserService = authenticatedUserService;
    }

    @Operation(summary = "Start a new conversation")
    @PostMapping
    public ResponseEntity<Map<String, Object>> createConversation(
            @RequestBody(required = false) Map<String, String> body
    ) {
        User user = authenticatedUserService.requireUser();
        String title = (body != null && body.containsKey("title")) ? body.get("title") : "New Conversation";

        Conversation conversation = new Conversation(user, title);
        conversation = conversationRepository.save(conversation);

        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                "id", conversation.getId().toString(),
                "title", conversation.getTitle(),
                "status", conversation.getStatus().name(),
                "createdAt", conversation.getCreatedAt().toString()
        ));
    }

    @Operation(summary = "List user conversations")
    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> listConversations(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        User user = authenticatedUserService.requireUser();
        Page<Conversation> conversations = conversationRepository.findByUserIdOrderByUpdatedAtDesc(
                user.getId(), PageRequest.of(page, size)
        );

        List<Map<String, Object>> response = conversations.getContent().stream()
                .map(c -> Map.<String, Object>of(
                        "id", c.getId().toString(),
                        "title", c.getTitle() != null ? c.getTitle() : "Untitled",
                        "status", c.getStatus().name(),
                        "activeAgent", c.getActiveAgentName() != null ? c.getActiveAgentName() : "None",
                        "messageCount", c.getMessageCount(),
                        "updatedAt", c.getUpdatedAt().toString()
                ))
                .toList();

        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Get conversation details and messages")
    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getConversation(@PathVariable UUID id) {
        User user = authenticatedUserService.requireUser();
        Conversation conversation = conversationRepository.findByIdAndUserId(id, user.getId())
                .orElseThrow(() -> VoiceOsException.notFound("Conversation", id));

        List<Message> messages = messageRepository.findByConversationIdOrderByCreatedAtAsc(id);

        List<Map<String, Object>> messageDtos = messages.stream()
                .map(m -> Map.<String, Object>of(
                        "id", m.getId().toString(),
                        "role", m.getRole().name(),
                        "content", m.getContent(),
                        "agentName", m.getAgentName() != null ? m.getAgentName() : "",
                        "createdAt", m.getCreatedAt().toString()
                ))
                .toList();

        return ResponseEntity.ok(Map.of(
                "id", conversation.getId().toString(),
                "title", conversation.getTitle() != null ? conversation.getTitle() : "",
                "status", conversation.getStatus().name(),
                "activeAgent", conversation.getActiveAgentName() != null ? conversation.getActiveAgentName() : "",
                "messageCount", conversation.getMessageCount(),
                "messages", messageDtos
        ));
    }
}
