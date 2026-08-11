package com.voiceos.domain.repository;

import com.voiceos.domain.entity.Task;
import com.voiceos.domain.entity.Task.TaskStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TaskRepository extends JpaRepository<Task, UUID> {

    Page<Task> findByUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);

    List<Task> findByUserIdAndStatus(UUID userId, TaskStatus status);

    Optional<Task> findByIdAndUserId(UUID id, UUID userId);

    List<Task> findByUserIdAndDueDateBefore(UUID userId, Instant dueDate);

    long countByUserIdAndStatus(UUID userId, TaskStatus status);
}
