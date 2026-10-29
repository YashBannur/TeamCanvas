package com.teamcanvas.security;

import com.teamcanvas.config.AppProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;
import javax.crypto.SecretKey;
import org.springframework.stereotype.Service;

/** Creates and verifies short-lived access tokens (HS256). */
@Service
public class JwtService {

    private static final String EMAIL_CLAIM = "email";

    private final SecretKey key;
    private final Duration accessTokenExpiry;

    public JwtService(AppProperties properties) {
        // Throws if the secret is shorter than 256 bits, so a weak key can never be used silently.
        this.key = Keys.hmacShaKeyFor(properties.jwt().secret().getBytes(StandardCharsets.UTF_8));
        this.accessTokenExpiry = properties.jwt().accessTokenExpiry();
    }

    public String generateAccessToken(UserPrincipal principal) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(String.valueOf(principal.getId()))
                .claim(EMAIL_CLAIM, principal.getEmail())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(accessTokenExpiry)))
                .signWith(key)
                .compact();
    }

    public long getAccessTokenExpirySeconds() {
        return accessTokenExpiry.toSeconds();
    }

    /**
     * Verifies signature and expiry. Returns empty for anything invalid (expired, tampered, malformed,
     * signed with another key) so callers never have to deal with JWT library exceptions.
     */
    public Optional<UserPrincipal> parse(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            Long userId = Long.valueOf(claims.getSubject());
            String email = claims.get(EMAIL_CLAIM, String.class);
            return Optional.of(UserPrincipal.fromClaims(userId, email));
        } catch (JwtException | IllegalArgumentException ex) {
            return Optional.empty();
        }
    }
}
