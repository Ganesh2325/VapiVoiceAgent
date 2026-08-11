package com.voiceos.tool.impl;

import com.voiceos.domain.entity.Task;
import com.voiceos.domain.entity.User;
import com.voiceos.domain.repository.TaskRepository;
import com.voiceos.domain.repository.UserRepository;
import com.voiceos.tool.core.Tool;
import com.voiceos.tool.core.ToolResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Task Management Tool.
 * Directly interacts with TaskRepository to create, list, and complete user tasks.
 * Risk Level: MEDIUM (creates/modifies user data safely within VoiceOS).
 */
@Component
public class TaskTool implements Tool {

    private static final Logger log = LoggerFactory.getLogger(TaskTool.class);

    private final TaskRepository taskRepository;
    private final UserRepository userRepository;

    public TaskTool(TaskRepository taskRepository, UserRepository userRepository) {
        this.taskRepository = taskRepository;
        this.userRepository = userRepository;
    }

    @Override
    public String getName() {
        return "task_manager";
    }

    @Override
    public String getDescription() {
        return "Creates, lists, or completes tasks for the user. Actions: 'create', 'list', 'complete'.";
    }

    @Override
    public ToolRiskLevel getRiskLevel() {
        return ToolRiskLevel.MEDIUM;
    }

    @Override
    public void validateInput(Map<String, Object> params) {
        Tool.super.validateInput(params);
        if (!params.containsKey("action")) {
            throw new IllegalArgumentException("TaskTool requires 'action' ('create', 'list', 'complete').");
        }
    }

    @Override
    @Transactional
    public ToolResult execute(Map<String, Object> params) {
        long startMs = System.currentTimeMillis();
        String action = params.get("action").toString().toLowerCase();

        UUID userId = null;
        if (params.containsKey("userId") && params.get("userId") != null) {
            userId = UUID.fromString(params.get("userId").toString());
        }

        try {
            return switch (action) {
                case "create" -> handleCreate(params, userId, startMs);
                case "list" -> handleList(userId, startMs);
                case "complete" -> handleComplete(params, userId, startMs);
                default -> ToolResult.failure("Unknown task action: " + action, System.currentTimeMillis() - startMs);
            };
        } catch (Exception e) {
            log.error("TaskTool action '{}' failed: {}", action, e.getMessage());
            return ToolResult.failure("Task operation failed: " + e.getMessage(), System.currentTimeMillis() - startMs);
        }
    }

    private ToolResult handleCreate(Map<String, Object> params, UUID userId, long startMs) {
        String title = (String) params.getOrDefault("title", "New Task");
        String description = (String) params.getOrDefault("description", "");
        String priorityStr = (String) params.getOrDefault("priority", "MEDIUM");

        Task.TaskPriority priority = Task.TaskPriority.MEDIUM;
        try {
            priority = Task.TaskPriority.valueOf(priorityStr.toUpperCase());
        } catch (Exception ignored) {}

        User user = null;
        if (userId != null) {
            user = userRepository.findById(userId).orElse(null);
        }
        if (user == null) {
            user = userRepository.findAll().stream().findFirst().orElse(null);
        }

        if (user == null) {
            return ToolResult.failure("No valid user found to assign task.", System.currentTimeMillis() - startMs);
        }

        Task task = new Task(user, title, description, priority);
        task = taskRepository.save(task);
        log.info("Created task '{}' (id={}) for user {}", title, task.getId(), user.getEmail());

        long latency = System.currentTimeMillis() - startMs;
        return ToolResult.success(
                Map.of("taskId", task.getId().toString(), "title", task.getTitle(), "status", task.getStatus().name()),
                "Task '" + title + "' created successfully (ID: " + task.getId() + ").",
                latency
        );
    }

    private ToolResult handleList(UUID userId, long startMs) {
        List<Task> tasks;
        if (userId != null) {
            tasks = taskRepository.findByUserIdAndStatus(userId, Task.TaskStatus.PENDING);
        } else {
            tasks = taskRepository.findAll().stream()
                    .filter(t -> t.getStatus() == Task.TaskStatus.PENDING)
                    .toList();
        }

        List<Map<String, Object>> taskSummaries = tasks.stream()
                .map(t -> Map.<String, Object>of(
                        "id", t.getId().toString(),
                        "title", t.getTitle(),
                        "priority", t.getPriority().name(),
                        "createdAt", t.getCreatedAt().toString()
                ))
                .toList();

        long latency = System.currentTimeMillis() - startMs;
        return ToolResult.success(
                Map.of("tasks", taskSummaries, "count", tasks.size()),
                "Found " + tasks.size() + " active task(s).",
                latency
        );
    }

    private ToolResult handleComplete(Map<String, Object> params, UUID userId, long startMs) {
        String taskIdStr = (String) params.get("taskId");
        if (taskIdStr == null) {
            return ToolResult.failure("taskId is required to complete a task.", System.currentTimeMillis() - startMs);
        }

        UUID taskId = UUID.fromString(taskIdStr);
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new IllegalArgumentException("Task not found with ID: " + taskId));

        task.complete();
        taskRepository.save(task);
        log.info("Task '{}' marked completed.", task.getTitle());

        long latency = System.currentTimeMillis() - startMs;
        return ToolResult.success(
                Map.of("taskId", task.getId().toString(), "status", "COMPLETED"),
                "Task '" + task.getTitle() + "' marked as completed.",
                latency
        );
    }
}
