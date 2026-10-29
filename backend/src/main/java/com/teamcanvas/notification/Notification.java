package com.teamcanvas.notification;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "notifications", uniqueConstraints = {
        @UniqueConstraint(name = "uq_notifications_dedupe_key", columnNames = "dedupe_key")
})
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "recipient_id", nullable = false, updatable = false)
    private Long recipientId;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 40, updatable = false)
    private NotificationType type;

    @Column(nullable = false, length = 500, updatable = false)
    private String message;

    @Column(name = "entity_type", nullable = false, length = 30, updatable = false)
    private String entityType;

    @Column(name = "entity_id", nullable = false, updatable = false)
    private Long entityId;

    @Column(name = "is_read", nullable = false)
    private boolean isRead;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "dedupe_key", nullable = false, length = 255, updatable = false)
    private String dedupeKey;

    protected Notification() {
    }

    public Notification(Long recipientId, NotificationType type, String message,
                        String entityType, Long entityId, String dedupeKey) {
        this.recipientId = recipientId;
        this.type = type;
        this.message = message;
        this.entityType = entityType;
        this.entityId = entityId;
        this.dedupeKey = dedupeKey;
        this.isRead = false;
    }

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }

    public void markRead() {
        isRead = true;
    }

    public Long getId() {
        return id;
    }

    public Long getRecipientId() {
        return recipientId;
    }

    public NotificationType getType() {
        return type;
    }

    public String getMessage() {
        return message;
    }

    public String getEntityType() {
        return entityType;
    }

    public Long getEntityId() {
        return entityId;
    }

    public boolean isRead() {
        return isRead;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public String getDedupeKey() {
        return dedupeKey;
    }
}