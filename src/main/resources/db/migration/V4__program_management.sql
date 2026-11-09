CREATE TABLE IF NOT EXISTS programs (
    id              BIGSERIAL PRIMARY KEY,
    title           VARCHAR(200) NOT NULL,
    category        VARCHAR(40)  NOT NULL,
    description     TEXT,
    status          VARCHAR(20)  NOT NULL DEFAULT 'PLANNING',
    start_date      DATE,
    end_date        DATE,
    budget          NUMERIC(14,2) DEFAULT 0,
    progress        INTEGER      NOT NULL DEFAULT 0,
    coordinator_id  BIGINT REFERENCES users(id) ON DELETE SET NULL,
    banner_url      VARCHAR(500),
    created_at      TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS program_participants (
    id          BIGSERIAL PRIMARY KEY,
    program_id  BIGINT NOT NULL REFERENCES programs(id) ON DELETE CASCADE,
    user_id     BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    role        VARCHAR(20) NOT NULL DEFAULT 'VOLUNTEER',
    joined_at   TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_program_participant UNIQUE (program_id, user_id)
);

CREATE TABLE IF NOT EXISTS program_expenses (
    id           BIGSERIAL PRIMARY KEY,
    program_id   BIGINT NOT NULL REFERENCES programs(id) ON DELETE CASCADE,
    title        VARCHAR(200) NOT NULL,
    amount       NUMERIC(14,2) NOT NULL,
    expense_date DATE,
    created_at   TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS program_media (
    id            BIGSERIAL PRIMARY KEY,
    program_id    BIGINT NOT NULL REFERENCES programs(id) ON DELETE CASCADE,
    file_url      VARCHAR(500) NOT NULL,
    original_name VARCHAR(255),
    media_type    VARCHAR(20) NOT NULL DEFAULT 'PHOTO',
    created_at    TIMESTAMP NOT NULL DEFAULT NOW()
);
