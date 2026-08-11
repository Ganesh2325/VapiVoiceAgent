package com.voiceos.domain.repository;

import com.voiceos.domain.entity.ToolExecution;
import com.voiceos.domain.entity.ToolExecution.ToolExecutionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ToolExecutionRepository extends JpaRepository<ToolExecution, UUID> {

    List<ToolExecution> findByAgentExecutionId(UUID agentExecutionId);

    List<ToolExecution> findByApprovalId(UUID approvalId);

    List<ToolExecution> findByStatus(ToolExecutionStatus status);
}
