package com.voiceos.api.controller;

import com.voiceos.domain.entity.Memory;
import com.voiceos.domain.entity.User;
import com.voiceos.domain.repository.MemoryRepository;
import com.voiceos.exception.VoiceOsException;
import com.voiceos.security.AuthenticatedUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Long-Term Memory REST API.
 */
@RestController
@RequestMapping("/api/v1/memory")
@Tag(name = "Memory", description = "User preferences and long-term memory")
@SecurityRequirement(name = "bearerAuth")
public class MemoryController {

    private final MemoryRepository memoryRepository;
    private final AuthenticatedUserService authenticatedUserService;

    public MemoryController(MemoryRepository memoryRepository, AuthenticatedUserService authenticatedUserService) {
        this.memoryRepository = memoryRepository;
        this.authenticatedUserService = authenticatedUserService;
    }

    @Operation(summary = "List all user memories")
    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> listMemories() {
        User user = authenticatedUserService.requireUser();
        List<Memory> memories = memoryRepository.findByUserIdOrderByUpdatedAtDesc(user.getId());

        List<Map<String, Object>> response = memories.stream()
                .map(m -> Map.<String, Object>of(
                        "id", m.getId().toString(),
                        "key", m.getKey() != null ? m.getKey() : "",
                        "content", m.getContent(),
                        "type", m.getMemoryType().name(),
                        "updatedAt", m.getUpdatedAt().toString()
                ))
                .toList();

        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Save a new long-term memory")
    @PostMapping
    public ResponseEntity<Map<String, Object>> saveMemory(@RequestBody Map<String, String> body) {
        User user = authenticatedUserService.requireUser();
        String content = body.get("content");
        String key = body.getOrDefault("key", "preference");

        if (content == null || content.isBlank()) {
            throw VoiceOsException.badRequest("Memory content cannot be empty.");
        }

        Memory memory = new Memory(user, Memory.MemoryType.LONG_TERM, key, content);
        memory = memoryRepository.save(memory);

        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                "id", memory.getId().toString(),
                "key", memory.getKey(),
                "content", memory.getContent(),
                "type", memory.getMemoryType().name()
        ));
    }
}
