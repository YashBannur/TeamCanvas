package com.teamcanvas.notification;

import java.time.Instant;

public record NotificationResponse(
        Long id,
        NotificationType type,
        String message,
        String entityType,
        Long entityId,
        boolean isRead,
        Instant createdAt) {

    public static NotificationResponse from(Notification notification) {
        return new NotificationResponse(
                notification.getId(),
                notification.getType(),
                notification.getMessage(),
                notification.getEntityType(),
                notification.getEntityId(),
                notification.isRead(),
                notification.getCreatedAt());
    }
}