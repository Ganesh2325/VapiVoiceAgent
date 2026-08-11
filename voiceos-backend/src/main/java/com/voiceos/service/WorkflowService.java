package com.voiceos.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;

@Service
public class WorkflowService {

    private static final Logger log = LoggerFactory.getLogger(WorkflowService.class);

    private final TaskScheduler taskScheduler;
    private final Map<UUID, ScheduledFuture<?>> scheduledTasks = new ConcurrentHashMap<>();

    public WorkflowService(TaskScheduler taskScheduler) {
        this.taskScheduler = taskScheduler;
    }

    public UUID scheduleTask(Runnable task, Instant startTime) {
        UUID taskId = UUID.randomUUID();
        ScheduledFuture<?> future = taskScheduler.schedule(task, startTime);
        if (future != null) {
            scheduledTasks.put(taskId, future);
            log.info("Scheduled workflow task {} for {}", taskId, startTime);
        }
        return taskId;
    }

    public boolean cancelTask(UUID taskId) {
        ScheduledFuture<?> future = scheduledTasks.remove(taskId);
        if (future != null) {
            boolean cancelled = future.cancel(false);
            log.info("Cancelled workflow task {}: {}", taskId, cancelled);
            return cancelled;
        }
        return false;
    }
}
