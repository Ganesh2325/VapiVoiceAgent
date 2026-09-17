package com.voiceos.security;

import com.voiceos.config.JwtProperties;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.User;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class JwtTokenProviderTest {

    private static final String SECRET =
            "test_secret_key_that_is_at_least_64_characters_long_for_testing_purposes_only";

    @Test
    void signedTokenRoundTrip() {
        JwtTokenProvider provider = provider(86_400_000);
        var user = User.withUsername("alice@example.com").password("x").authorities(List.of()).build();
        UUID userId = UUID.randomUUID();
        String token = provider.generateAccessToken(user, userId);

        assertEquals("alice@example.com", provider.extractUsername(token));
        assertEquals(userId, provider.extractUserId(token));
        assertTrue(provider.isTokenValid(token, user));
    }

    @Test
    void expiredTokenIsRejected() throws InterruptedException {
        JwtTokenProvider provider = provider(1);
        var user = User.withUsername("alice@example.com").password("x").authorities(List.of()).build();
        String token = provider.generateAccessToken(user, UUID.randomUUID());
        Thread.sleep(20);
        assertFalse(provider.isTokenValid(token, user));
        assertThrows(Exception.class, () -> provider.extractUsername(token));
    }

    @Test
    void malformedTokenIsRejected() {
        JwtTokenProvider provider = provider(86_400_000);
        var user = User.withUsername("alice@example.com").password("x").authorities(List.of()).build();
        assertFalse(provider.isTokenValid("not-a-jwt", user));
    }

    @Test
    void blankSecretFailsClosed() {
        assertThrows(IllegalStateException.class, () -> JwtTokenProvider.validateSecret(""));
        assertThrows(IllegalStateException.class, () -> JwtTokenProvider.validateSecret(null));
        assertThrows(IllegalStateException.class, () -> JwtTokenProvider.validateSecret("changeme"));
        assertThrows(IllegalStateException.class, () -> JwtTokenProvider.validateSecret("short"));
    }

    private static JwtTokenProvider provider(long expirationMs) {
        return new JwtTokenProvider(new JwtProperties(SECRET, expirationMs, 86_400_000));
    }
}
