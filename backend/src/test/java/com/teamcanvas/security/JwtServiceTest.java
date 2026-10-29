package com.teamcanvas.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.teamcanvas.config.AppProperties;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/** Pure unit test: no Spring context, no database. */
class JwtServiceTest {

    private static final String SECRET = "unit-test-secret-that-is-at-least-32-characters-long";
    private static final String OTHER_SECRET = "a-completely-different-secret-with-32-plus-characters";

    private JwtService serviceWith(String secret, Duration accessExpiry) {
        AppProperties properties = new AppProperties(
                new AppProperties.Jwt(secret, accessExpiry),
                new AppProperties.Refresh(Duration.ofHours(24), "tc_refresh", false, "Lax"),
                new AppProperties.Cors(List.of("http://localhost:5173")));
        return new JwtService(properties);
    }

    @Test
    void generatedTokenCanBeParsedBackToTheSameUser() {
        JwtService service = serviceWith(SECRET, Duration.ofMinutes(30));
        String token = service.generateAccessToken(UserPrincipal.fromClaims(42L, "yash@example.com"));

        Optional<UserPrincipal> parsed = service.parse(token);

        assertTrue(parsed.isPresent());
        assertEquals(42L, parsed.get().getId());
        assertEquals("yash@example.com", parsed.get().getEmail());
    }

    @Test
    void tokenSignedWithAnotherSecretIsRejected() {
        String token = serviceWith(OTHER_SECRET, Duration.ofMinutes(30))
                .generateAccessToken(UserPrincipal.fromClaims(1L, "a@example.com"));

        assertTrue(serviceWith(SECRET, Duration.ofMinutes(30)).parse(token).isEmpty());
    }

    @Test
    void expiredTokenIsRejected() {
        JwtService service = serviceWith(SECRET, Duration.ofSeconds(-5));
        String token = service.generateAccessToken(UserPrincipal.fromClaims(1L, "a@example.com"));

        assertTrue(service.parse(token).isEmpty());
    }

    @Test
    void tokenWithTamperedPayloadIsRejected() {
        JwtService service = serviceWith(SECRET, Duration.ofMinutes(30));
        String[] parts = service.generateAccessToken(UserPrincipal.fromClaims(1L, "a@example.com")).split("\\.");
        String forgedPayload = Base64.getUrlEncoder().withoutPadding().encodeToString(
                "{\"sub\":\"999\",\"email\":\"evil@example.com\"}".getBytes(StandardCharsets.UTF_8));

        String forged = parts[0] + "." + forgedPayload + "." + parts[2];

        assertTrue(service.parse(forged).isEmpty());
    }

    @Test
    void garbageIsRejected() {
        JwtService service = serviceWith(SECRET, Duration.ofMinutes(30));

        assertTrue(service.parse("not-a-jwt").isEmpty());
        assertTrue(service.parse("").isEmpty());
    }
}
