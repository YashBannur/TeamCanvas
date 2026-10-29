package com.teamcanvas.invitation;

import com.teamcanvas.user.User;
import com.teamcanvas.workspace.Workspace;
import com.teamcanvas.workspace.WorkspaceRole;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/** An invitation for an existing user to join a workspace with a given role. */
@Entity
@Table(name = "workspace_invitations")
public class WorkspaceInvitation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "workspace_id", nullable = false, updatable = false)
    private Workspace workspace;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "invitee_id", nullable = false, updatable = false)
    private User invitee;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "invited_by", nullable = false, updatable = false)
    private User invitedBy;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 20, updatable = false)
    private WorkspaceRole role;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 20)
    private InvitationStatus status;

    @Column(name = "expires_at", nullable = false, updatable = false)
    private Instant expiresAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "responded_at")
    private Instant respondedAt;

    /** Required by JPA. */
    protected WorkspaceInvitation() {
    }

    public WorkspaceInvitation(Workspace workspace, User invitee, User invitedBy, WorkspaceRole role,
                               Instant expiresAt) {
        this.workspace = workspace;
        this.invitee = invitee;
        this.invitedBy = invitedBy;
        this.role = role;
        this.expiresAt = expiresAt;
        this.status = InvitationStatus.PENDING;
    }

    @PrePersist
    void onCreate() {
        this.createdAt = Instant.now();
    }

    public boolean isPending() {
        return status == InvitationStatus.PENDING;
    }

    public boolean isExpired(Instant now) {
        return !expiresAt.isAfter(now);
    }

    public void accept(Instant now) {
        this.status = InvitationStatus.ACCEPTED;
        this.respondedAt = now;
    }

    public void reject(Instant now) {
        this.status = InvitationStatus.REJECTED;
        this.respondedAt = now;
    }

    public void cancel(Instant now) {
        this.status = InvitationStatus.CANCELLED;
        this.respondedAt = now;
    }

    public void markExpired() {
        this.status = InvitationStatus.EXPIRED;
    }

    public Long getId() {
        return id;
    }

    public Workspace getWorkspace() {
        return workspace;
    }

    public User getInvitee() {
        return invitee;
    }

    public User getInvitedBy() {
        return invitedBy;
    }

    public WorkspaceRole getRole() {
        return role;
    }

    public InvitationStatus getStatus() {
        return status;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getRespondedAt() {
        return respondedAt;
    }
}
