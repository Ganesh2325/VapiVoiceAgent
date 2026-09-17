package com.voiceos.api.controller;

import com.voiceos.domain.entity.AgentExecution;
import com.voiceos.domain.entity.User;
import com.voiceos.domain.repository.AgentExecutionRepository;
import com.voiceos.domain.repository.ConversationRepository;
import com.voiceos.exception.VoiceOsException;
import com.voiceos.security.AuthenticatedUserService;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/executions")
public class ExecutionController {

    private final AgentExecutionRepository executionRepository;
    private final ConversationRepository conversationRepository;
    private final AuthenticatedUserService authenticatedUserService;

    public ExecutionController(AgentExecutionRepository executionRepository,
                               ConversationRepository conversationRepository,
                               AuthenticatedUserService authenticatedUserService) {
        this.executionRepository = executionRepository;
        this.conversationRepository = conversationRepository;
        this.authenticatedUserService = authenticatedUserService;
    }

    @GetMapping
    public ResponseEntity<List<AgentExecution>> getAllExecutions() {
        User user = authenticatedUserService.requireUser();
        return ResponseEntity.ok(
                executionRepository.findByUserIdOrderByCreatedAtDesc(user.getId(), Pageable.unpaged()).getContent()
        );
    }

    @GetMapping("/{id}")
    public ResponseEntity<AgentExecution> getExecution(@PathVariable UUID id) {
        User user = authenticatedUserService.requireUser();
        return executionRepository.findById(id)
                .filter(execution -> execution.getUser() != null
                        && user.getId().equals(execution.getUser().getId()))
                .map(ResponseEntity::ok)
                .orElseThrow(() -> VoiceOsException.notFound("Execution", id));
    }

    @GetMapping("/conversation/{conversationId}")
    public ResponseEntity<List<AgentExecution>> getExecutionsByConversation(@PathVariable UUID conversationId) {
        User user = authenticatedUserService.requireUser();
        conversationRepository.findByIdAndUserId(conversationId, user.getId())
                .orElseThrow(() -> VoiceOsException.forbidden(
                        "Conversation does not belong to the authenticated user"));
        return ResponseEntity.ok(executionRepository.findByConversationIdOrderByCreatedAtAsc(conversationId));
    }
}
