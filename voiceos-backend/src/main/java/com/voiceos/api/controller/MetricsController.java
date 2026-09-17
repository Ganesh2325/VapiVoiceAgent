package com.voiceos.api.controller;

import com.voiceos.domain.entity.User;
import com.voiceos.domain.repository.EvaluationRepository;
import com.voiceos.security.AuthenticatedUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * AI Quality & Evaluation Metrics REST API.
 * Missing measurements are returned as {@code unavailable}, never invented.
 */
@RestController
@RequestMapping("/api/v1/metrics")
@Tag(name = "Metrics & Evaluation", description = "AI quality, tool success rates, and latency metrics")
@SecurityRequirement(name = "bearerAuth")
public class MetricsController {

    private final EvaluationRepository evaluationRepository;
    private final AuthenticatedUserService authenticatedUserService;

    public MetricsController(EvaluationRepository evaluationRepository,
                             AuthenticatedUserService authenticatedUserService) {
        this.evaluationRepository = evaluationRepository;
        this.authenticatedUserService = authenticatedUserService;
    }

    @Operation(summary = "Get platform AI quality scores and metrics")
    @GetMapping
    public ResponseEntity<Map<String, Object>> getMetrics() {
        User user = authenticatedUserService.requireUser();

        Double avgToolSuccess = evaluationRepository.findAverageToolSuccessRateByUserId(user.getId());
        Double avgLatency = evaluationRepository.findAverageLatencyByUserId(user.getId());
        long successfulTasks = evaluationRepository.countSuccessfulTasksByUserId(user.getId());

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("taskSuccessRate", "unavailable");
        body.put("toolSuccessRate", avgToolSuccess != null ? avgToolSuccess : "unavailable");
        body.put("averageLatencyMs", avgLatency != null ? avgLatency : "unavailable");
        body.put("successfulTasksCount", successfulTasks);
        body.put("hallucinationRate", "unavailable");
        body.put("approvalCompliance", "unavailable");
        return ResponseEntity.ok(body);
    }
}
