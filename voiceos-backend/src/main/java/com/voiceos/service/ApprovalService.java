package com.voiceos.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.voiceos.config.VoiceOsProperties;
import com.voiceos.domain.entity.Approval;
import com.voiceos.domain.repository.ApprovalRepository;
import com.voiceos.exception.VoiceOsException;
import com.voiceos.tool.core.ToolRegistry;
import com.voiceos.tool.core.ToolResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
public class ApprovalService {

    private static final Logger log = LoggerFactory.getLogger(ApprovalService.class);
    private static final Set<String> MOCK_AUTH_TOKENS = Set.of(
            "dummy-auth-token",
            "mock-auth-token"
    );
    private static final Set<String> MOCK_PAYMENT_TOKENS = Set.of(
            "mock-txn-id",
            "mock-payment-success",
            "dummy-payment-token"
    );

    private final ApprovalRepository approvalRepository;
    private final ToolRegistry toolRegistry;
    private final ObjectMapper objectMapper;
    private final VoiceOsProperties properties;

    public ApprovalService(ApprovalRepository approvalRepository,
                           ToolRegistry toolRegistry,
                           ObjectMapper objectMapper,
                           VoiceOsProperties properties) {
        this.approvalRepository = approvalRepository;
        this.toolRegistry = toolRegistry;
        this.objectMapper = objectMapper;
        this.properties = properties;
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
            return ToolResult.success(Map.of("status", "rejected"), "User rejected the action.", 0);
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

        String mode = classifyAuthToken(authToken);
        Map<String, Object> payload = new HashMap<>(getPayloadAsMap(approval));
        payload.put("authMode", mode);
        payload.put("credentialProvided", true);
        payload.remove("oauth_token");
        approval.setPayload(payload);

        if ("MOCK".equals(mode)) {
            if (!isMockEnabled()) {
                throw VoiceOsException.forbidden(
                        "Mock authentication tokens are not accepted when MOCK_EXTERNAL_APIS is false.");
            }
            log.info("MOCK authentication recorded for approval {} (not a real OAuth grant)", approvalId);
        } else {
            log.info("STUB authentication credential recorded for approval {} (not verified against an identity provider)",
                    approvalId);
        }

        approval.setStatus(Approval.ApprovalStatus.APPROVED);
        approvalRepository.save(approval);

        ToolResult result = toolRegistry.executeTool(approval.getActionType(), payload);
        return withMode(result, "authMode", mode);
    }

    public ToolResult processPayment(UUID approvalId, String transactionId) {
        Approval approval = getApproval(approvalId);

        if (approval.getStatus() != Approval.ApprovalStatus.REQUIRES_PAYMENT) {
            throw new IllegalStateException("Approval does not require payment: " + approval.getStatus());
        }

        String mode = classifyPaymentToken(transactionId);
        Map<String, Object> payload = new HashMap<>(getPayloadAsMap(approval));
        payload.put("paymentMode", mode);
        payload.put("transactionId", transactionId);
        payload.put("paymentCompleted", "MOCK".equals(mode) || "STUB".equals(mode));
        payload.put("realPaymentConfirmed", false);
        approval.setPayload(payload);

        if ("MOCK".equals(mode)) {
            if (!isMockEnabled()) {
                throw VoiceOsException.forbidden(
                        "Mock payment tokens are not accepted when MOCK_EXTERNAL_APIS is false.");
            }
            log.info("MOCK payment recorded for approval {} (not a real provider confirmation)", approvalId);
        } else {
            log.info("STUB payment credential recorded for approval {} (not confirmed by a payment provider)",
                    approvalId);
        }

        approval.setStatus(Approval.ApprovalStatus.APPROVED);
        approvalRepository.save(approval);

        ToolResult result = toolRegistry.executeTool(approval.getActionType(), payload);
        return withMode(result, "paymentMode", mode);
    }

    private boolean isMockEnabled() {
        return properties.ai() != null && properties.ai().mock();
    }

    private String classifyAuthToken(String token) {
        if (token == null) {
            return "STUB";
        }
        if (MOCK_AUTH_TOKENS.contains(token.trim().toLowerCase(Locale.ROOT))) {
            return "MOCK";
        }
        return "STUB";
    }

    private String classifyPaymentToken(String transactionId) {
        if (transactionId == null) {
            return "STUB";
        }
        if (MOCK_PAYMENT_TOKENS.contains(transactionId.trim().toLowerCase(Locale.ROOT))) {
            return "MOCK";
        }
        return "STUB";
    }

    private ToolResult withMode(ToolResult result, String key, String mode) {
        if (result == null) {
            return ToolResult.success(Map.of(key, mode), "Recorded as " + mode, 0);
        }
        Map<String, Object> data = result.data() != null ? new HashMap<>(result.data()) : new HashMap<>();
        data.put(key, mode);
        if (result.success()) {
            return ToolResult.success(data, result.rawOutput(), result.durationMs());
        }
        return result;
    }

    private Approval getApproval(UUID approvalId) {
        return approvalRepository.findById(approvalId)
                .orElseThrow(() -> new IllegalArgumentException("Approval not found"));
    }

    private Map<String, Object> getPayloadAsMap(Approval approval) {
        try {
            Map<String, Object> converted = objectMapper.convertValue(
                    approval.getPayload(), new TypeReference<Map<String, Object>>() {});
            return converted != null ? converted : new HashMap<>();
        } catch (Exception e) {
            log.error("Failed to parse approval payload: {}", e.getClass().getSimpleName());
            return new HashMap<>();
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
