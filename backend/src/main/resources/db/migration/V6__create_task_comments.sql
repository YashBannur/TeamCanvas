CREATE TABLE task_comments (
    id         BIGINT        NOT NULL AUTO_INCREMENT,
    task_id    BIGINT        NOT NULL,
    author_id  BIGINT        NOT NULL,
    content    VARCHAR(2000) NOT NULL,
    created_at DATETIME(6)   NOT NULL,
    updated_at DATETIME(6)   NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_task_comments_task
        FOREIGN KEY (task_id) REFERENCES tasks (id) ON DELETE CASCADE,
    CONSTRAINT fk_task_comments_author
        FOREIGN KEY (author_id) REFERENCES users (id),
    CONSTRAINT chk_task_comments_content
        CHECK (CHAR_LENGTH(content) BETWEEN 1 AND 2000),
    INDEX idx_task_comments_task_created (task_id, created_at, id),
    INDEX idx_task_comments_author (author_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;