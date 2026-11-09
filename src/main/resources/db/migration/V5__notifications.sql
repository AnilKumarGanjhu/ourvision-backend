CREATE TABLE IF NOT EXISTS notifications (
    id          BIGSERIAL PRIMARY KEY,
    user_id     BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    title       VARCHAR(200) NOT NULL,
    message     TEXT,
    category    VARCHAR(30) NOT NULL DEFAULT 'GENERAL',
    program_id  BIGINT REFERENCES programs(id) ON DELETE SET NULL,
    is_read     BOOLEAN NOT NULL DEFAULT FALSE,
    created_at  TIMESTAMP NOT NULL DEFAULT NOW()
);
CREATE INDEX IF NOT EXISTS idx_notifications_user_read
ON notifications (user_id, read_status);
