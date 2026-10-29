-- V5: tasks. A task belongs to one project; access is decided by the project's workspace.

CREATE TABLE tasks (
    id          BIGINT        NOT NULL AUTO_INCREMENT,
    project_id  BIGINT        NOT NULL,
    title       VARCHAR(200)  NOT NULL,
    description VARCHAR(5000) NULL,
    status      VARCHAR(20)   NOT NULL,
    priority    VARCHAR(20)   NOT NULL,
    assignee_id BIGINT        NULL,
    reporter_id BIGINT        NOT NULL,
    start_date  DATE          NULL,
    due_date    DATE          NULL,
    sort_order  INT           NOT NULL,          -- position inside the Kanban column (status), 0 = top
    version     BIGINT        NOT NULL DEFAULT 0, -- optimistic locking
    created_at  DATETIME(6)   NOT NULL,
    updated_at  DATETIME(6)   NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_tasks_project FOREIGN KEY (project_id) REFERENCES projects (id) ON DELETE CASCADE,
    CONSTRAINT fk_tasks_assignee FOREIGN KEY (assignee_id) REFERENCES users (id),
    CONSTRAINT fk_tasks_reporter FOREIGN KEY (reporter_id) REFERENCES users (id),
    CONSTRAINT chk_tasks_status CHECK (status IN ('TODO', 'IN_PROGRESS', 'IN_REVIEW', 'DONE')),
    CONSTRAINT chk_tasks_priority CHECK (priority IN ('LOW', 'MEDIUM', 'HIGH', 'URGENT')),
    CONSTRAINT chk_tasks_dates CHECK (start_date IS NULL OR due_date IS NULL OR due_date >= start_date),
    INDEX idx_tasks_project_status_order (project_id, status, sort_order),
    INDEX idx_tasks_assignee_due (assignee_id, due_date),
    INDEX idx_tasks_project_due (project_id, due_date)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;
