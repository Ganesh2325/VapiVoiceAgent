package com.voiceos.api.controller;

import com.voiceos.provider.ProviderCapability;
import com.voiceos.provider.ProviderRegistry;
import com.voiceos.provider.ServiceProvider;
import com.voiceos.security.AuthenticatedUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Read-only provider catalog. There is no execute endpoint — clients cannot
 * select or invoke an internal provider implementation.
 */
@RestController
@RequestMapping("/api/v1/providers")
@Tag(name = "Providers", description = "Honest provider catalog")
@SecurityRequirement(name = "bearerAuth")
public class ProviderController {

    private final ProviderRegistry providerRegistry;
    private final AuthenticatedUserService authenticatedUserService;

    public ProviderController(ProviderRegistry providerRegistry, AuthenticatedUserService authenticatedUserService) {
        this.providerRegistry = providerRegistry;
        this.authenticatedUserService = authenticatedUserService;
    }

    @GetMapping
    @Operation(summary = "List registered providers (mode and availability, no secrets)")
    public ResponseEntity<List<Map<String, Object>>> list() {
        authenticatedUserService.requireUser();
        List<Map<String, Object>> body = providerRegistry.all().stream()
                .map(ProviderController::toView)
                .toList();
        return ResponseEntity.ok(body);
    }

    private static Map<String, Object> toView(ServiceProvider provider) {
        Map<String, Object> view = new LinkedHashMap<>();
        view.put("name", provider.getName());
        view.put("description", provider.getDescription());
        view.put("mode", provider.getMode() != null ? provider.getMode().name() : null);
        view.put("availability", provider.availability() != null ? provider.availability().name() : null);
        view.put("capabilities", provider.capabilities() == null
                ? List.of()
                : provider.capabilities().stream().map(ProviderCapability::name).collect(Collectors.toList()));
        return view;
    }
}
