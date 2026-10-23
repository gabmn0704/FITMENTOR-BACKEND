package com.fitmentor.security;

import com.fitmentor.config.FitmentorProperties;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;

/**
 * Issues and verifies HS256 JWTs (jjwt). Tokens last seven days and carry the user id as
 * subject and the guest flag as a claim.
 *
 * <p>The secret comes from {@code JWT_SECRET} (at least 32 characters). In the production
 * profiles ({@code prod} or {@code render}) it is mandatory. In development a random secret
 * is generated and tokens become invalid after a restart; the secret is never logged.
 */
@Service
public class JwtService {

    static final Duration TTL = Duration.ofDays(7);
    static final int MIN_SECRET_LENGTH = 32;
    static final String SESSION_MARKER_CLAIM = "svm";

    private static final Logger logger = LoggerFactory.getLogger(JwtService.class);
    private static final Profiles PRODUCTION = Profiles.of("prod", "render");

    private final SecretKey key;
    private final Clock clock;

    public JwtService(FitmentorProperties properties, Environment environment, Clock clock) {
        this.clock = clock;
        this.key = Keys.hmacShaKeyFor(resolveSecret(properties.jwt().secret(), environment)
            .getBytes(StandardCharsets.UTF_8));
    }

    public String issue(UUID userId, boolean guest) {
        return issue(userId, guest, 0L);
    }

    /** Issues a token bound to the user's current session marker ({@code svm} claim; 0 = never revoked). */
    public String issue(UUID userId, boolean guest, long sessionMarker) {
        Instant now = clock.instant();
        return Jwts.builder()
            .subject(userId.toString())
            .claim("guest", guest)
            .claim(SESSION_MARKER_CLAIM, sessionMarker)
            .issuedAt(Date.from(now))
            .expiration(Date.from(now.plus(TTL)))
            .signWith(key)
            .compact();
    }

    /** Returns the principal when the token is valid and not expired; empty otherwise. */
    public Optional<AuthenticatedUser> verify(String token) {
        if (token == null || token.isBlank()) {
            return Optional.empty();
        }
        try {
            var claims = Jwts.parser()
                .clock(() -> Date.from(clock.instant()))
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
            Object marker = claims.get(SESSION_MARKER_CLAIM);
            return Optional.of(new AuthenticatedUser(
                UUID.fromString(claims.getSubject()),
                Boolean.TRUE.equals(claims.get("guest", Boolean.class)),
                marker instanceof Number number ? number.longValue() : 0L));
        } catch (JwtException | IllegalArgumentException exception) {
            return Optional.empty();
        }
    }

    private static String resolveSecret(String configured, Environment environment) {
        if (configured == null || configured.isBlank()) {
            if (environment.acceptsProfiles(PRODUCTION)) {
                throw new IllegalStateException("JWT_SECRET is required in production (at least 32 characters).");
            }
            logger.warn("JWT_SECRET is not set: using an ephemeral development secret. Tokens expire on restart.");
            byte[] random = new byte[48];
            new SecureRandom().nextBytes(random);
            return Base64.getEncoder().encodeToString(random);
        }
        if (configured.length() < MIN_SECRET_LENGTH) {
            throw new IllegalStateException("JWT_SECRET must be at least 32 characters long.");
        }
        return configured;
    }
}
