CREATE TABLE IF NOT EXISTS program_feedback (
    id          BIGSERIAL PRIMARY KEY,
    program_id  BIGINT NOT NULL REFERENCES programs(id) ON DELETE CASCADE,
    user_id     BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    rating      INTEGER NOT NULL CHECK (rating BETWEEN 1 AND 5),
    comment     TEXT,
    created_at  TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_program_feedback UNIQUE (program_id, user_id)
);
