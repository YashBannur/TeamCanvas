ALTER TABLE tasks
    ADD COLUMN completed_at DATETIME(6) NULL;

UPDATE tasks
SET completed_at = updated_at
WHERE status = 'DONE';