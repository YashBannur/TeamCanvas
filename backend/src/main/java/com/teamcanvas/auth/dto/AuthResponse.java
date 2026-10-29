package com.teamcanvas.auth.dto;

import com.teamcanvas.user.dto.UserResponse;

/** Body returned by login and refresh. The refresh token is NOT here: it travels only in an HttpOnly cookie. */
public record AuthResponse(String accessToken, String tokenType, long expiresInSeconds, UserResponse user) {
}
