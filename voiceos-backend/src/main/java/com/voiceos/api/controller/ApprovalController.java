package com.voiceos.api.controller;

import com.voiceos.domain.entity.Approval;
import com.voiceos.domain.entity.User;
import com.voiceos.domain.repository.ApprovalRepository;
import com.voiceos.exception.VoiceOsException;
import com.voiceos.security.AuthenticatedUserService;
import com.voiceos.service.ApprovalService;
import com.voiceos.tool.core.ToolResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Human-in-the-Loop Approval Controller.
 * Enables users to review, approve, or reject high-risk agent actions.
 */
@RestController
@RequestMapping("/api/v1/approvals")
@Tag(name = "Approvals", description = "Human-in-the-loop approval management")
@SecurityRequirement(name = "bearerAuth")
public class ApprovalController {

    private static final Logger log = LoggerFactory.getLogger(ApprovalController.class);

    private final ApprovalRepository approvalRepository;
    private final AuthenticatedUserService authenticatedUserService;
    private final ApprovalService approvalService;

    public ApprovalController(ApprovalRepository approvalRepository,
                              AuthenticatedUserService authenticatedUserService,
                              ApprovalService approvalService) {
        this.approvalRepository = approvalRepository;
        this.authenticatedUserService = authenticatedUserService;
        this.approvalService = approvalService;
    }

    @Operation(summary = "List pending approvals for current user")
    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> listPendingApprovals() {
        User user = authenticatedUserService.requireUser();

        List<Approval> approvals = approvalRepository.findAll().stream()
            .filter(a -> a.getUser().getId().equals(user.getId()) &&
                         (a.getStatus() == Approval.ApprovalStatus.PENDING ||
                          a.getStatus() == Approval.ApprovalStatus.REQUIRES_AUTHENTICATION ||
                          a.getStatus() == Approval.ApprovalStatus.REQUIRES_PAYMENT))
            .toList();

        List<Map<String, Object>> response = approvals.stream()
                .map(a -> Map.<String, Object>of(
                        "id", a.getId().toString(),
                        "actionType", a.getActionType(),
                        "actionDescription", a.getActionDescription(),
                        "riskLevel", a.getRiskLevel().name(),
                        "payload", a.getPayload() != null ? a.getPayload() : Map.of(),
                        "status", a.getStatus().name(),
                        "createdAt", a.getCreatedAt().toString()
                ))
                .toList();

        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Approve a pending action")
    @PostMapping("/{id}/approve")
    @Transactional
    public ResponseEntity<Map<String, Object>> approve(@PathVariable UUID id) {
        User user = authenticatedUserService.requireUser();
        Approval approval = approvalRepository.findByIdAndUserId(id, user.getId())
                .orElseThrow(() -> VoiceOsException.notFound("Approval", id));

        ToolResult executionResult = approvalService.processApproval(id, true);

        return ResponseEntity.ok(Map.of(
                "id", approval.getId().toString(),
                "status", "APPROVED",
                "message", "Action approved and dispatched successfully.",
                "executionResult", executionResult != null ? executionResult.rawOutput() : "Executed"
        ));
    }

    @Operation(summary = "Provide authentication for a paused action")
    @PostMapping("/{id}/authenticate")
    @Transactional
    public ResponseEntity<Map<String, Object>> authenticate(
            @PathVariable UUID id,
            @RequestBody Map<String, String> body
    ) {
        User user = authenticatedUserService.requireUser();
        Approval approval = approvalRepository.findByIdAndUserId(id, user.getId())
                .orElseThrow(() -> VoiceOsException.notFound("Approval", id));

        if (body == null || body.get("token") == null || body.get("token").isBlank()) {
            throw VoiceOsException.badRequest("token is required");
        }
        ToolResult executionResult = approvalService.processAuthentication(id, body.get("token"));

        return ResponseEntity.ok(Map.of(
                "id", approval.getId().toString(),
                "status", approval.getStatus().name(),
                "mode", executionResult != null && executionResult.data() != null
                        ? executionResult.data().getOrDefault("authMode", "STUB")
                        : "STUB",
                "message", "Credential recorded. This is not a verified identity-provider grant unless mode is REAL.",
                "executionResult", executionResult != null ? executionResult.rawOutput() : "Executed"
        ));
    }

    @Operation(summary = "Provide payment for a paused action")
    @PostMapping("/{id}/pay")
    @Transactional
    public ResponseEntity<Map<String, Object>> pay(
            @PathVariable UUID id,
            @RequestBody Map<String, String> body
    ) {
        User user = authenticatedUserService.requireUser();
        Approval approval = approvalRepository.findByIdAndUserId(id, user.getId())
                .orElseThrow(() -> VoiceOsException.notFound("Approval", id));

        if (body == null || body.get("transactionId") == null || body.get("transactionId").isBlank()) {
            throw VoiceOsException.badRequest("transactionId is required");
        }
        ToolResult executionResult = approvalService.processPayment(id, body.get("transactionId"));

        return ResponseEntity.ok(Map.of(
                "id", approval.getId().toString(),
                "status", approval.getStatus().name(),
                "mode", executionResult != null && executionResult.data() != null
                        ? executionResult.data().getOrDefault("paymentMode", "STUB")
                        : "STUB",
                "message", "Payment credential recorded. This is not a real provider confirmation unless mode is REAL.",
                "executionResult", executionResult != null ? executionResult.rawOutput() : "Executed"
        ));
    }

    @Operation(summary = "Reject a pending action")
    @PostMapping("/{id}/reject")
    @Transactional
    public ResponseEntity<Map<String, Object>> reject(
            @PathVariable UUID id,
            @RequestBody(required = false) Map<String, String> body
    ) {
        User user = authenticatedUserService.requireUser();
        Approval approval = approvalRepository.findByIdAndUserId(id, user.getId())
                .orElseThrow(() -> VoiceOsException.notFound("Approval", id));

        String reason = (body != null && body.containsKey("reason")) ? body.get("reason") : "User rejected";
        approval.reject(reason);
        approvalRepository.save(approval);
        log.info("User {} rejected action: {}", user.getId(), approval.getActionType());

        return ResponseEntity.ok(Map.of(
                "id", approval.getId().toString(),
                "status", "REJECTED",
                "reason", reason
        ));
    }
}
