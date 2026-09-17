package com.voiceos.api.controller;

import com.voiceos.domain.entity.Task;
import com.voiceos.domain.entity.User;
import com.voiceos.domain.repository.TaskRepository;
import com.voiceos.exception.VoiceOsException;
import com.voiceos.security.AuthenticatedUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Task Management REST API.
 */
@RestController
@RequestMapping("/api/v1/tasks")
@Tag(name = "Tasks", description = "User tasks and to-do items")
@SecurityRequirement(name = "bearerAuth")
public class TaskController {

    private final TaskRepository taskRepository;
    private final AuthenticatedUserService authenticatedUserService;

    public TaskController(TaskRepository taskRepository, AuthenticatedUserService authenticatedUserService) {
        this.taskRepository = taskRepository;
        this.authenticatedUserService = authenticatedUserService;
    }

    @Operation(summary = "List user tasks")
    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> listTasks(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        User user = authenticatedUserService.requireUser();
        Page<Task> tasks = taskRepository.findByUserIdOrderByCreatedAtDesc(user.getId(), PageRequest.of(page, size));

        List<Map<String, Object>> response = tasks.getContent().stream()
                .map(t -> Map.<String, Object>of(
                        "id", t.getId().toString(),
                        "title", t.getTitle(),
                        "description", t.getDescription() != null ? t.getDescription() : "",
                        "status", t.getStatus().name(),
                        "priority", t.getPriority().name(),
                        "createdAt", t.getCreatedAt().toString()
                ))
                .toList();

        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Create a new task")
    @PostMapping
    public ResponseEntity<Map<String, Object>> createTask(@RequestBody Map<String, String> body) {
        User user = authenticatedUserService.requireUser();
        String title = body.getOrDefault("title", "New Task");
        String description = body.getOrDefault("description", "");
        String priorityStr = body.getOrDefault("priority", "MEDIUM");

        Task.TaskPriority priority = Task.TaskPriority.MEDIUM;
        try {
            priority = Task.TaskPriority.valueOf(priorityStr.toUpperCase());
        } catch (Exception ignored) {}

        Task task = new Task(user, title, description, priority);
        task = taskRepository.save(task);

        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                "id", task.getId().toString(),
                "title", task.getTitle(),
                "status", task.getStatus().name(),
                "priority", task.getPriority().name()
        ));
    }

    @Operation(summary = "Mark task as complete")
    @PatchMapping("/{id}/complete")
    @Transactional
    public ResponseEntity<Map<String, Object>> completeTask(@PathVariable UUID id) {
        User user = authenticatedUserService.requireUser();
        Task task = taskRepository.findByIdAndUserId(id, user.getId())
                .orElseThrow(() -> VoiceOsException.notFound("Task", id));

        task.complete();
        taskRepository.save(task);

        return ResponseEntity.ok(Map.of(
                "id", task.getId().toString(),
                "status", "COMPLETED",
                "completedAt", task.getCompletedAt().toString()
        ));
    }
}
