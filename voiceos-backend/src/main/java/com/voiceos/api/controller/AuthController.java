package com.voiceos.api.controller;

import com.voiceos.api.dto.AuthDtos;
import com.voiceos.service.auth.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Authentication controller.
 * Public endpoints — no JWT required.
 */
@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Authentication", description = "User registration, login, and token refresh")
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @Operation(
        summary = "Register a new user",
        description = "Creates a new user account and returns JWT tokens."
    )
    @PostMapping("/register")
    public ResponseEntity<AuthDtos.AuthResponse> register(
            @Valid @RequestBody AuthDtos.RegisterRequest request) {
        log.info("Registration request for email: {}", request.email());
        AuthDtos.AuthResponse response = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(
        summary = "Login",
        description = "Authenticates user credentials and returns JWT tokens."
    )
    @PostMapping("/login")
    public ResponseEntity<AuthDtos.AuthResponse> login(
            @Valid @RequestBody AuthDtos.LoginRequest request) {
        AuthDtos.AuthResponse response = authService.login(request);
        return ResponseEntity.ok(response);
    }

    @Operation(
        summary = "Refresh access token",
        description = "Exchanges a valid refresh token for new access and refresh tokens."
    )
    @PostMapping("/refresh")
    public ResponseEntity<AuthDtos.AuthResponse> refresh(
            @Valid @RequestBody AuthDtos.RefreshRequest request) {
        AuthDtos.AuthResponse response = authService.refresh(request);
        return ResponseEntity.ok(response);
    }
}
