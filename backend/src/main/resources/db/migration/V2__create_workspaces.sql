-- V2: workspaces and their members. workspace_members is the single source of truth for who may access what.

CREATE TABLE workspaces (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    name        VARCHAR(100) NOT NULL,
    description VARCHAR(500) NULL,
    created_by  BIGINT       NOT NULL,
    created_at  DATETIME(6)  NOT NULL,
    updated_at  DATETIME(6)  NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_workspaces_created_by FOREIGN KEY (created_by) REFERENCES users (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE workspace_members (
    id           BIGINT      NOT NULL AUTO_INCREMENT,
    workspace_id BIGINT      NOT NULL,
    user_id      BIGINT      NOT NULL,
    role         VARCHAR(20) NOT NULL,
    joined_at    DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_workspace_members UNIQUE (workspace_id, user_id),
    CONSTRAINT fk_workspace_members_workspace FOREIGN KEY (workspace_id) REFERENCES workspaces (id) ON DELETE CASCADE,
    CONSTRAINT fk_workspace_members_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT chk_workspace_members_role CHECK (role IN ('OWNER', 'ADMIN', 'MEMBER')),
    INDEX idx_workspace_members_user_id (user_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;
