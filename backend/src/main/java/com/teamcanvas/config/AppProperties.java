package com.teamcanvas.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Duration;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Type-safe binding of the "app.*" section of application.yml.
 * Because it is @Validated, the application refuses to start if, for example, the JWT secret is missing or too short.
 */
@Validated
@ConfigurationProperties(prefix = "app")
public record AppProperties(
        @Valid @NotNull Jwt jwt,
        @Valid @NotNull Refresh refresh,
        @Valid @NotNull Cors cors) {

    public record Jwt(@NotBlank @Size(min = 32) String secret, @NotNull Duration accessTokenExpiry) {
    }

    public record Refresh(@NotNull Duration expiry,
                          @NotBlank String cookieName,
                          boolean cookieSecure,
                          @NotBlank String cookieSameSite) {
    }

    public record Cors(@NotEmpty List<String> allowedOrigins) {
    }
}
