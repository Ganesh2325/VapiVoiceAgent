package com.voiceos.security;

import com.voiceos.domain.entity.User;
import com.voiceos.domain.repository.UserRepository;
import com.voiceos.exception.VoiceOsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Resolves the authenticated VoiceOS user from the Spring Security context.
 *
 * <p>Identity always comes from a validated JWT (or equivalent SecurityContext
 * principal). Request-body {@code userId} / email values are never treated as
 * proof of identity.
 */
@Service
public class AuthenticatedUserService {

    private final UserRepository userRepository;

    public AuthenticatedUserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * @return the persisted user bound to the current security principal
     * @throws VoiceOsException UNAUTHORIZED when no authenticated identity exists
     */
    public User requireUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication.getPrincipal() == null
                || "anonymousUser".equals(authentication.getPrincipal())) {
            throw VoiceOsException.unauthorized("Authentication required");
        }

        String email = authentication.getName();
        if (email == null || email.isBlank()) {
            throw VoiceOsException.unauthorized("Authentication required");
        }

        return userRepository.findByEmail(email)
                .orElseThrow(() -> VoiceOsException.unauthorized("Authenticated user no longer exists"));
    }

    public UUID requireUserId() {
        return requireUser().getId();
    }
}
