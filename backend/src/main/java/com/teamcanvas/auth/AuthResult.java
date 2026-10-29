package com.teamcanvas.auth;

import com.teamcanvas.auth.dto.AuthResponse;

/** Internal result of login/refresh: the JSON body for the client plus the raw refresh token for the cookie. */
public record AuthResult(AuthResponse response, String refreshToken) {
}
