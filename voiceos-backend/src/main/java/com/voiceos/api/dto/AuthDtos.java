package com.voiceos.api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Authentication DTOs — all records for immutability.
 */
public final class AuthDtos {

    private AuthDtos() {}

    /** Request body for POST /api/v1/auth/register */
    public record RegisterRequest(
            @NotBlank(message = "Display name is required")
            @Size(min = 2, max = 100, message = "Display name must be between 2 and 100 characters")
            String displayName,

            @NotBlank(message = "Email is required")
            @Email(message = "Email must be a valid email address")
            String email,

            @NotBlank(message = "Password is required")
            @Size(min = 8, max = 100, message = "Password must be between 8 and 100 characters")
            String password
    ) {}

    /** Request body for POST /api/v1/auth/login */
    public record LoginRequest(
            @NotBlank(message = "Email is required")
            @Email(message = "Email must be a valid email address")
            String email,

            @NotBlank(message = "Password is required")
            String password
    ) {}

    /** Request body for POST /api/v1/auth/refresh */
    public record RefreshRequest(
            @NotBlank(message = "Refresh token is required")
            String refreshToken
    ) {}

    /** Response body for successful authentication. */
    public record AuthResponse(
            String accessToken,
            String refreshToken,
            String tokenType,
            long expiresIn,
            UserInfo user
    ) {
        public AuthResponse(String accessToken, String refreshToken, long expiresIn, UserInfo user) {
            this(accessToken, refreshToken, "Bearer", expiresIn, user);
        }
    }

    /** Minimal user info included in auth response. */
    public record UserInfo(
            String id,
            String email,
            String displayName,
            String role
    ) {}
}
