package com.voiceos.api.controller;

import com.voiceos.domain.entity.AgentExecution;
import com.voiceos.domain.repository.AgentExecutionRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/executions")
public class ExecutionController {

    private final AgentExecutionRepository executionRepository;

    public ExecutionController(AgentExecutionRepository executionRepository) {
        this.executionRepository = executionRepository;
    }

    @GetMapping
    public ResponseEntity<List<AgentExecution>> getAllExecutions() {
        return ResponseEntity.ok(executionRepository.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<AgentExecution> getExecution(@PathVariable UUID id) {
        return executionRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/conversation/{conversationId}")
    public ResponseEntity<List<AgentExecution>> getExecutionsByConversation(@PathVariable UUID conversationId) {
        // Assuming we could filter by conversationId. In a real app we'd add this to the repository.
        List<AgentExecution> executions = executionRepository.findAll().stream()
                .filter(e -> e.getConversation() != null && e.getConversation().getId().equals(conversationId))
                .toList();
        return ResponseEntity.ok(executions);
    }
}
