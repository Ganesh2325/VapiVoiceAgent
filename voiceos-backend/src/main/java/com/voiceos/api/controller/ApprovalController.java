package com.voiceos.api.controller;

import com.voiceos.domain.entity.Approval;
import com.voiceos.domain.entity.User;
import com.voiceos.domain.repository.ApprovalRepository;
import com.voiceos.domain.repository.UserRepository;
import com.voiceos.exception.VoiceOsException;
import com.voiceos.service.ApprovalService;
import com.voiceos.tool.core.ToolRegistry;
import com.voiceos.tool.core.ToolResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
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
    private final UserRepository userRepository;
    private final ApprovalService approvalService;

    public ApprovalController(ApprovalRepository approvalRepository,
                              UserRepository userRepository,
                              ToolRegistry toolRegistry,
                              ApprovalService approvalService) {
        this.approvalRepository = approvalRepository;
        this.userRepository = userRepository;
        this.toolRegistry = toolRegistry;
        this.approvalService = approvalService;
    }

    @Operation(summary = "List pending approvals for current user")
    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> listPendingApprovals(
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        User user = getUser(userDetails);
        
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
    public ResponseEntity<Map<String, Object>> approve(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        User user = getUser(userDetails);
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
            @RequestBody Map<String, String> body,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        User user = getUser(userDetails);
        Approval approval = approvalRepository.findByIdAndUserId(id, user.getId())
                .orElseThrow(() -> VoiceOsException.notFound("Approval", id));

        String token = body.getOrDefault("token", "dummy-auth-token");
        ToolResult executionResult = approvalService.processAuthentication(id, token);

        return ResponseEntity.ok(Map.of(
                "id", approval.getId().toString(),
                "status", "APPROVED",
                "message", "Authentication accepted. Action dispatched.",
                "executionResult", executionResult != null ? executionResult.rawOutput() : "Executed"
        ));
    }

    @Operation(summary = "Provide payment for a paused action")
    @PostMapping("/{id}/pay")
    @Transactional
    public ResponseEntity<Map<String, Object>> pay(
            @PathVariable UUID id,
            @RequestBody Map<String, String> body,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        User user = getUser(userDetails);
        Approval approval = approvalRepository.findByIdAndUserId(id, user.getId())
                .orElseThrow(() -> VoiceOsException.notFound("Approval", id));

        String transactionId = body.getOrDefault("transactionId", "txn-" + UUID.randomUUID());
        ToolResult executionResult = approvalService.processPayment(id, transactionId);

        return ResponseEntity.ok(Map.of(
                "id", approval.getId().toString(),
                "status", "APPROVED",
                "message", "Payment accepted. Action dispatched.",
                "executionResult", executionResult != null ? executionResult.rawOutput() : "Executed"
        ));
    }

    @Operation(summary = "Reject a pending action")
    @PostMapping("/{id}/reject")
    @Transactional
    public ResponseEntity<Map<String, Object>> reject(
            @PathVariable UUID id,
            @RequestBody(required = false) Map<String, String> body,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        User user = getUser(userDetails);
        Approval approval = approvalRepository.findByIdAndUserId(id, user.getId())
                .orElseThrow(() -> VoiceOsException.notFound("Approval", id));

        String reason = (body != null && body.containsKey("reason")) ? body.get("reason") : "User rejected";
        approval.reject(reason);
        approvalRepository.save(approval);
        log.info("User {} rejected action: {} (reason: {})", user.getEmail(), approval.getActionType(), reason);

        return ResponseEntity.ok(Map.of(
                "id", approval.getId().toString(),
                "status", "REJECTED",
                "reason", reason
        ));
    }

    private User getUser(UserDetails userDetails) {
        if (userDetails == null) {
            return userRepository.findAll().stream().findFirst()
                    .orElseThrow(() -> VoiceOsException.badRequest("User not found"));
        }
        return userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> VoiceOsException.notFound("User", userDetails.getUsername()));
    }
}
