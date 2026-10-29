package com.teamcanvas.activity;

import java.time.Instant;

public record ActivityLogResponse(
        Long id,
        Long workspaceId,
        Long projectId,
        Long actorId,
        String action,
        String targetType,
        Long targetId,
        String details,
        Instant createdAt) {

    public static ActivityLogResponse from(ActivityLog log) {
        return new ActivityLogResponse(
                log.getId(),
                log.getWorkspaceId(),
                log.getProjectId(),
                log.getActorId(),
                log.getAction(),
                log.getTargetType(),
                log.getTargetId(),
                log.getDetails(),
                log.getCreatedAt());
    }
}