package com.voiceos.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.voiceos.domain.entity.Approval;
import com.voiceos.domain.repository.ApprovalRepository;
import com.voiceos.tool.core.ToolRegistry;
import com.voiceos.tool.core.ToolResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;

@Service
public class ApprovalService {

    private static final Logger log = LoggerFactory.getLogger(ApprovalService.class);
    
    private final ApprovalRepository approvalRepository;
    private final ToolRegistry toolRegistry;
    private final ObjectMapper objectMapper;

    public ApprovalService(ApprovalRepository approvalRepository, ToolRegistry toolRegistry, ObjectMapper objectMapper) {
        this.approvalRepository = approvalRepository;
        this.toolRegistry = toolRegistry;
        this.objectMapper = objectMapper;
    }

    public ToolResult processApproval(UUID approvalId, boolean isApproved) {
        Approval approval = getApproval(approvalId);
        
        if (approval.getStatus() != Approval.ApprovalStatus.PENDING) {
            throw new IllegalStateException("Approval is not in PENDING state: " + approval.getStatus());
        }

        if (!isApproved) {
            approval.setStatus(Approval.ApprovalStatus.REJECTED);
            approvalRepository.save(approval);
            log.info("Approval {} was rejected by human.", approvalId);
            return ToolResult.success(Map.of("status", "rejected"), "User rejected the action.");
        }

        approval.setStatus(Approval.ApprovalStatus.APPROVED);
        approvalRepository.save(approval);
        log.info("Approval {} was APPROVED by human. Executing tool: {}", approvalId, approval.getActionType());

        return executeApprovedTool(approval);
    }
    
    public ToolResult processAuthentication(UUID approvalId, String authToken) {
        Approval approval = getApproval(approvalId);
        
        if (approval.getStatus() != Approval.ApprovalStatus.REQUIRES_AUTHENTICATION) {
            throw new IllegalStateException("Approval does not require authentication: " + approval.getStatus());
        }
        
        log.info("Authentication provided for Approval {}. Proceeding to execute.", approvalId);
        approval.setStatus(Approval.ApprovalStatus.APPROVED);
        // Inject auth token into payload for tool usage
        Map<String, Object> payload = getPayloadAsMap(approval);
        payload.put("oauth_token", authToken);
        approval.setPayload(payload);
        approvalRepository.save(approval);
        
        return toolRegistry.executeTool(approval.getActionType(), payload);
    }

    public ToolResult processPayment(UUID approvalId, String transactionId) {
        Approval approval = getApproval(approvalId);
        
        if (approval.getStatus() != Approval.ApprovalStatus.REQUIRES_PAYMENT) {
            throw new IllegalStateException("Approval does not require payment: " + approval.getStatus());
        }
        
        log.info("Payment transaction {} provided for Approval {}. Proceeding to execute.", transactionId, approvalId);
        approval.setStatus(Approval.ApprovalStatus.APPROVED);
        Map<String, Object> payload = getPayloadAsMap(approval);
        payload.put("paymentCompleted", true);
        payload.put("transactionId", transactionId);
        approval.setPayload(payload);
        approvalRepository.save(approval);
        
        return toolRegistry.executeTool(approval.getActionType(), payload);
    }

    private Approval getApproval(UUID approvalId) {
        return approvalRepository.findById(approvalId)
                .orElseThrow(() -> new IllegalArgumentException("Approval not found"));
    }

    private Map<String, Object> getPayloadAsMap(Approval approval) {
        try {
            return objectMapper.convertValue(approval.getPayload(), new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            log.error("Failed to parse approval payload: {}", e.getMessage());
            return Map.of();
        }
    }

    private ToolResult executeApprovedTool(Approval approval) {
        Map<String, Object> params = getPayloadAsMap(approval);
        if (params.isEmpty()) {
            return ToolResult.failure("Failed to parse parameters", 0);
        }
        return toolRegistry.executeTool(approval.getActionType(), params);
    }
}
