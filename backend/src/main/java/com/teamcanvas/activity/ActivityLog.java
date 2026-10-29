package com.teamcanvas.activity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "activity_logs")
public class ActivityLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "workspace_id", nullable = false, updatable = false)
    private Long workspaceId;

    @Column(name = "project_id", updatable = false)
    private Long projectId;

    @Column(name = "actor_id", nullable = false, updatable = false)
    private Long actorId;

    @Column(nullable = false, length = 50, updatable = false)
    private String action;

    @Column(name = "target_type", nullable = false, length = 30, updatable = false)
    private String targetType;

    @Column(name = "target_id", nullable = false, updatable = false)
    private Long targetId;

    @Column(columnDefinition = "TEXT", updatable = false)
    private String details;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected ActivityLog() {
    }

    public ActivityLog(Long workspaceId, Long projectId, Long actorId,
                       String action, String targetType, Long targetId, String details) {
        this.workspaceId = workspaceId;
        this.projectId = projectId;
        this.actorId = actorId;
        this.action = action;
        this.targetType = targetType;
        this.targetId = targetId;
        this.details = details;
    }

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }

    public Long getId() {
        return id;
    }

    public Long getWorkspaceId() {
        return workspaceId;
    }

    public Long getProjectId() {
        return projectId;
    }

    public Long getActorId() {
        return actorId;
    }

    public String getAction() {
        return action;
    }

    public String getTargetType() {
        return targetType;
    }

    public Long getTargetId() {
        return targetId;
    }

    public String getDetails() {
        return details;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}