-- V3: Refresh token storage

CREATE TABLE IF NOT EXISTS refresh_tokens (
    id          BIGSERIAL       NOT NULL,
    token       VARCHAR(36)     NOT NULL,   -- UUID string
    user_id     UUID            NOT NULL,
    expiry_date TIMESTAMPTZ     NOT NULL,
    revoked     BOOLEAN         NOT NULL DEFAULT FALSE,

    CONSTRAINT pk_refresh_tokens       PRIMARY KEY (id),
    CONSTRAINT uq_refresh_tokens_token UNIQUE (token),
    CONSTRAINT fk_refresh_tokens_user  FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_refresh_tokens_token   ON refresh_tokens (token);
CREATE INDEX IF NOT EXISTS idx_refresh_tokens_user_id ON refresh_tokens (user_id);
CREATE INDEX IF NOT EXISTS idx_refresh_tokens_expiry  ON refresh_tokens (expiry_date)
    WHERE revoked = FALSE;   -- Partial index — only index active tokens

COMMENT ON TABLE  refresh_tokens             IS 'Persisted refresh tokens for JWT rotation strategy';
COMMENT ON COLUMN refresh_tokens.token       IS 'Opaque UUID token value returned to the client';
COMMENT ON COLUMN refresh_tokens.expiry_date IS 'Token becomes invalid after this timestamp';
COMMENT ON COLUMN refresh_tokens.revoked     IS 'True = token has been consumed or invalidated';
