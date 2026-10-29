CREATE TABLE activity_logs (
    id BIGINT NOT NULL AUTO_INCREMENT,
    workspace_id BIGINT NOT NULL,
    project_id BIGINT NULL,
    actor_id BIGINT NOT NULL,
    action VARCHAR(50) NOT NULL,
    target_type VARCHAR(30) NOT NULL,
    target_id BIGINT NOT NULL,
    details TEXT NULL,
    created_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_activity_logs_workspace
        FOREIGN KEY (workspace_id) REFERENCES workspaces(id) ON DELETE CASCADE,
    CONSTRAINT fk_activity_logs_project
        FOREIGN KEY (project_id) REFERENCES projects(id) ON DELETE SET NULL,
    CONSTRAINT fk_activity_logs_actor
        FOREIGN KEY (actor_id) REFERENCES users(id),
    INDEX idx_activity_logs_workspace_created (workspace_id, created_at, id),
    INDEX idx_activity_logs_project_created (project_id, created_at, id)
);