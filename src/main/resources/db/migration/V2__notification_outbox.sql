CREATE TABLE outbox_notifications (
    id VARCHAR(36) PRIMARY KEY,
    recipient VARCHAR(255) NOT NULL,
    status VARCHAR(12) NOT NULL,
    attempts INT NOT NULL DEFAULT 0,
    next_attempt_at TIMESTAMP(6) NOT NULL,
    lease_token VARCHAR(36)
);

CREATE INDEX idx_outbox_notifications_due ON outbox_notifications (status, next_attempt_at, id);
