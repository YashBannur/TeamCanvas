package com.teamcanvas.user.dto;

import com.teamcanvas.user.User;
import java.time.Instant;

/** What the API exposes about a user. Deliberately has no password or hash field. */
public record UserResponse(Long id, String name, String email, Instant createdAt) {

    public static UserResponse from(User user) {
        return new UserResponse(user.getId(), user.getName(), user.getEmail(), user.getCreatedAt());
    }
}
