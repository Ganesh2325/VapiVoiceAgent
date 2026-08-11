package com.voiceos.service.auth;

import com.voiceos.api.dto.AuthDtos;
import com.voiceos.config.VoiceOsProperties;
import com.voiceos.domain.entity.User;
import com.voiceos.domain.repository.UserRepository;
import com.voiceos.exception.VoiceOsException;
import com.voiceos.security.JwtTokenProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Authentication service.
 * Handles user registration, login, and token refresh.
 */
@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final AuthenticationManager authenticationManager;
    private final UserDetailsService userDetailsService;
    private final VoiceOsProperties properties;

    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       JwtTokenProvider jwtTokenProvider,
                       AuthenticationManager authenticationManager,
                       UserDetailsService userDetailsService,
                       VoiceOsProperties properties) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
        this.authenticationManager = authenticationManager;
        this.userDetailsService = userDetailsService;
        this.properties = properties;
    }

    /**
     * Registers a new user account.
     *
     * @param request the registration request
     * @return JWT tokens for the new user
     * @throws VoiceOsException if email is already registered
     */
    @Transactional
    public AuthDtos.AuthResponse register(AuthDtos.RegisterRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw VoiceOsException.conflict(
                    "An account with email '" + request.email() + "' already exists.");
        }

        User user = new User(
                request.email().toLowerCase().trim(),
                passwordEncoder.encode(request.password()),
                request.displayName().trim()
        );
        user = userRepository.save(user);
        log.info("New user registered: {} (id={})", user.getEmail(), user.getId());

        return generateTokenResponse(user);
    }

    /**
     * Authenticates a user and returns JWT tokens.
     *
     * @param request the login request
     * @return JWT tokens
     * @throws org.springframework.security.authentication.BadCredentialsException if credentials invalid
     */
    @Transactional(readOnly = true)
    public AuthDtos.AuthResponse login(AuthDtos.LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.email().toLowerCase().trim(),
                        request.password()
                )
        );

        User user = userRepository.findByEmail(request.email().toLowerCase().trim())
                .orElseThrow(() -> VoiceOsException.notFound("User", request.email()));

        log.info("User logged in: {} (id={})", user.getEmail(), user.getId());
        return generateTokenResponse(user);
    }

    /**
     * Refreshes an access token using a valid refresh token.
     *
     * @param request the refresh request
     * @return new JWT tokens
     */
    @Transactional(readOnly = true)
    public AuthDtos.AuthResponse refresh(AuthDtos.RefreshRequest request) {
        String username;
        try {
            username = jwtTokenProvider.extractUsername(request.refreshToken());
        } catch (Exception e) {
            throw VoiceOsException.badRequest("Invalid or expired refresh token.");
        }

        UserDetails userDetails = userDetailsService.loadUserByUsername(username);
        if (!jwtTokenProvider.isTokenValid(request.refreshToken(), userDetails)) {
            throw VoiceOsException.badRequest("Invalid or expired refresh token.");
        }

        User user = userRepository.findByEmail(username)
                .orElseThrow(() -> VoiceOsException.notFound("User", username));

        log.debug("Token refreshed for user: {}", username);
        return generateTokenResponse(user);
    }

    // ─── Private Helpers ─────────────────────────────────────────────────────

    private AuthDtos.AuthResponse generateTokenResponse(User user) {
        UserDetails userDetails = userDetailsService.loadUserByUsername(user.getEmail());
        String accessToken = jwtTokenProvider.generateAccessToken(userDetails, user.getId());
        String refreshToken = jwtTokenProvider.generateRefreshToken(userDetails);

        return new AuthDtos.AuthResponse(
                accessToken,
                refreshToken,
                properties.jwt().expirationMs(),
                new AuthDtos.UserInfo(
                        user.getId().toString(),
                        user.getEmail(),
                        user.getDisplayName(),
                        user.getRole().name()
                )
        );
    }
}
