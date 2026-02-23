-- V3: Refresh tokens for JWT rotation and server-side revocation
CREATE TABLE IF NOT EXISTS refresh_tokens (
    id         UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    token      TEXT        NOT NULL,
    user_id    UUID        NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    expires_at TIMESTAMPTZ NOT NULL,
    revoked    BOOLEAN     NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT uq_refresh_tokens_token UNIQUE (token)
);

CREATE INDEX idx_refresh_tokens_token   ON refresh_tokens (token);
CREATE INDEX idx_refresh_tokens_user_id ON refresh_tokens (user_id);
CREATE INDEX idx_refresh_tokens_expiry  ON refresh_tokens (expires_at) WHERE revoked = FALSE;

COMMENT ON TABLE  refresh_tokens         IS 'Persisted JWT refresh tokens enabling server-side revocation';
COMMENT ON COLUMN refresh_tokens.revoked IS 'TRUE when explicitly logged out or invalidated by password change';
