CREATE TABLE notifications (
    id BIGINT NOT NULL AUTO_INCREMENT,
    recipient_id BIGINT NOT NULL,
    type VARCHAR(40) NOT NULL,
    message VARCHAR(500) NOT NULL,
    entity_type VARCHAR(30) NOT NULL,
    entity_id BIGINT NOT NULL,
    is_read BOOLEAN NOT NULL DEFAULT FALSE,
    created_at DATETIME(6) NOT NULL,
    dedupe_key VARCHAR(255) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_notifications_recipient
        FOREIGN KEY (recipient_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT uq_notifications_dedupe_key UNIQUE (dedupe_key),
    INDEX idx_notifications_recipient_created (recipient_id, created_at, id),
    INDEX idx_notifications_recipient_unread (recipient_id, is_read)
);
