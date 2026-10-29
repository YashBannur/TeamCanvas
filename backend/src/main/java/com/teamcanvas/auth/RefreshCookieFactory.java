package com.teamcanvas.auth;

import com.teamcanvas.config.AppProperties;
import java.time.Duration;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

/**
 * Builds the refresh-token cookie:
 *  - HttpOnly: JavaScript cannot read it, so an XSS bug cannot steal it.
 *  - Secure: only sent over https (enabled in production through COOKIE_SECURE).
 *  - SameSite: limits cross-site sending (CSRF defence).
 *  - Path=/api/auth: the browser sends it only to the auth endpoints, never to normal API calls.
 */
@Component
public class RefreshCookieFactory {

    private static final String COOKIE_PATH = "/api/auth";

    private final AppProperties.Refresh settings;

    public RefreshCookieFactory(AppProperties properties) {
        this.settings = properties.refresh();
    }

    public String cookieName() {
        return settings.cookieName();
    }

    public ResponseCookie create(String rawToken) {
        return build(rawToken, settings.expiry());
    }

    /** An expired, empty cookie with the same name/path makes the browser delete the real one. */
    public ResponseCookie clear() {
        return build("", Duration.ZERO);
    }

    private ResponseCookie build(String value, Duration maxAge) {
        return ResponseCookie.from(settings.cookieName(), value)
                .httpOnly(true)
                .secure(settings.cookieSecure())
                .sameSite(settings.cookieSameSite())
                .path(COOKIE_PATH)
                .maxAge(maxAge)
                .build();
    }
}
