package com.voiceos.security;

import com.voiceos.config.JwtProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;

/**
 * JWT token provider.
 * Handles token generation, validation, and claim extraction.
 * Uses HMAC-SHA256 (HS256) signing.
 *
 * <p>The signing secret is required at startup. VoiceOS never generates a
 * production secret and never accepts known-insecure fallbacks.
 */
@Component
public class JwtTokenProvider {

    private static final Logger log = LoggerFactory.getLogger(JwtTokenProvider.class);
    private static final int MIN_SECRET_LENGTH = 32;
    private static final Set<String> REJECTED_SECRETS = Set.of(
            "secret",
            "changeme",
            "password",
            "dev-secret",
            "default_dev_secret_replace_in_production_must_be_very_long",
            "change_this_to_a_very_long_random_secret_at_least_64_characters_please"
    );

    private final JwtProperties jwtProperties;
    private final SecretKey signingKey;

    public JwtTokenProvider(JwtProperties jwtProperties) {
        this.jwtProperties = jwtProperties;
        String secret = jwtProperties.secret();
        validateSecret(secret);
        byte[] keyBytes = Decoders.BASE64.decode(
                java.util.Base64.getEncoder().encodeToString(secret.getBytes(StandardCharsets.UTF_8))
        );
        this.signingKey = Keys.hmacShaKeyFor(keyBytes);
        log.info("JWT HS256 signing key initialized (secret length={}, expirationMs={})",
                secret.length(), jwtProperties.expirationMs());
    }

    static void validateSecret(String secret) {
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException(
                    "JWT_SECRET is required. Set the JWT_SECRET environment variable. "
                            + "VoiceOS will not start with an empty or generated secret.");
        }
        if (secret.length() < MIN_SECRET_LENGTH) {
            throw new IllegalStateException(
                    "JWT_SECRET must be at least " + MIN_SECRET_LENGTH + " characters.");
        }
        if (REJECTED_SECRETS.contains(secret.toLowerCase(Locale.ROOT).trim())) {
            throw new IllegalStateException(
                    "JWT_SECRET is a known-insecure placeholder. Generate a unique secret "
                            + "(for example: openssl rand -base64 64).");
        }
    }

    /**
     * Generates an access token for the given user details.
     *
     * @param userDetails the authenticated user
     * @param userId the user's UUID
     * @return signed JWT token string
     */
    public String generateAccessToken(UserDetails userDetails, UUID userId) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId.toString());
        claims.put("type", "access");
        return buildToken(claims, userDetails.getUsername(), jwtProperties.expirationMs());
    }

    /**
     * Generates a refresh token for the given user details.
     *
     * @param userDetails the authenticated user
     * @return signed JWT refresh token string
     */
    public String generateRefreshToken(UserDetails userDetails) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("type", "refresh");
        return buildToken(claims, userDetails.getUsername(), jwtProperties.refreshExpirationMs());
    }

    /**
     * Extracts the username (email) from a JWT token.
     */
    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    /**
     * Extracts the user ID from a JWT token.
     */
    public UUID extractUserId(String token) {
        String userIdStr = extractClaim(token, claims -> claims.get("userId", String.class));
        return userIdStr != null ? UUID.fromString(userIdStr) : null;
    }

    /**
     * Checks whether a token is valid for the given user details.
     */
    public boolean isTokenValid(String token, UserDetails userDetails) {
        try {
            final String username = extractUsername(token);
            return username.equals(userDetails.getUsername()) && !isTokenExpired(token);
        } catch (ExpiredJwtException e) {
            log.debug("JWT rejected: expired");
            return false;
        } catch (MalformedJwtException | SignatureException | IllegalArgumentException e) {
            log.debug("JWT rejected: malformed or invalid signature");
            return false;
        } catch (JwtException e) {
            log.debug("JWT validation failed: {}", e.getClass().getSimpleName());
            return false;
        }
    }

    /**
     * Extracts a specific claim from the token using the provided resolver function.
     */
    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    private String buildToken(Map<String, Object> extraClaims, String subject, long expiration) {
        long nowMs = System.currentTimeMillis();
        return Jwts.builder()
                .claims(extraClaims)
                .subject(subject)
                .issuedAt(new Date(nowMs))
                .expiration(new Date(nowMs + expiration))
                .signWith(signingKey)
                .compact();
    }

    private boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    private Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
