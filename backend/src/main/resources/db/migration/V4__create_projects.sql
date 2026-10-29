-- V4: projects. A project belongs to exactly one workspace; access is decided by workspace membership.

CREATE TABLE projects (
    id           BIGINT        NOT NULL AUTO_INCREMENT,
    workspace_id BIGINT        NOT NULL,
    name         VARCHAR(150)  NOT NULL,
    description  VARCHAR(2000) NULL,
    start_date   DATE          NULL,
    due_date     DATE          NULL,
    status       VARCHAR(20)   NOT NULL,
    created_by   BIGINT        NOT NULL,
    created_at   DATETIME(6)   NOT NULL,
    updated_at   DATETIME(6)   NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_projects_workspace FOREIGN KEY (workspace_id) REFERENCES workspaces (id) ON DELETE CASCADE,
    CONSTRAINT fk_projects_created_by FOREIGN KEY (created_by) REFERENCES users (id),
    CONSTRAINT chk_projects_status CHECK (status IN ('ACTIVE', 'ARCHIVED')),
    CONSTRAINT chk_projects_dates CHECK (start_date IS NULL OR due_date IS NULL OR due_date >= start_date),
    INDEX idx_projects_workspace_status (workspace_id, status)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;
