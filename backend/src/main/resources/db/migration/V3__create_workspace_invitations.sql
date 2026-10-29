-- V3: workspace invitations.

CREATE TABLE workspace_invitations (
    id           BIGINT      NOT NULL AUTO_INCREMENT,
    workspace_id BIGINT      NOT NULL,
    invitee_id   BIGINT      NOT NULL,
    invited_by   BIGINT      NOT NULL,
    role         VARCHAR(20) NOT NULL,
    status       VARCHAR(20) NOT NULL,
    expires_at   DATETIME(6) NOT NULL,
    created_at   DATETIME(6) NOT NULL,
    responded_at DATETIME(6) NULL,

    -- MySQL has no partial unique indexes. This generated column equals invitee_id only while the invitation is
    -- PENDING (otherwise NULL, and NULLs never collide), so the unique index below allows at most ONE pending
    -- invitation per (workspace, user). Hibernate does not map this column; MySQL maintains it.
    pending_invitee_id BIGINT GENERATED ALWAYS AS (CASE WHEN status = 'PENDING' THEN invitee_id ELSE NULL END) STORED,

    PRIMARY KEY (id),
    CONSTRAINT uk_invitation_one_pending UNIQUE (workspace_id, pending_invitee_id),
    CONSTRAINT fk_invitations_workspace FOREIGN KEY (workspace_id) REFERENCES workspaces (id) ON DELETE CASCADE,
    -- No ON DELETE CASCADE here: MySQL forbids it on the base column of a stored generated column.
    CONSTRAINT fk_invitations_invitee FOREIGN KEY (invitee_id) REFERENCES users (id),
    CONSTRAINT fk_invitations_invited_by FOREIGN KEY (invited_by) REFERENCES users (id),
    CONSTRAINT chk_invitations_role CHECK (role IN ('ADMIN', 'MEMBER')),
    CONSTRAINT chk_invitations_status CHECK (status IN ('PENDING', 'ACCEPTED', 'REJECTED', 'CANCELLED', 'EXPIRED')),
    INDEX idx_invitations_invitee_status (invitee_id, status),
    INDEX idx_invitations_workspace_status (workspace_id, status)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;
