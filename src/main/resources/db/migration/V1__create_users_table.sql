-- V1: Core users table
CREATE TABLE IF NOT EXISTS users (
    id                    UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    username              VARCHAR(50) NOT NULL,
    email                 VARCHAR(254) NOT NULL,
    password_hash         TEXT        NOT NULL,
    first_name            VARCHAR(100),
    last_name             VARCHAR(100),
    is_enabled            BOOLEAN     NOT NULL DEFAULT TRUE,
    is_account_non_locked BOOLEAN     NOT NULL DEFAULT TRUE,
    created_at            TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at            TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT uq_users_email    UNIQUE (email),
    CONSTRAINT uq_users_username UNIQUE (username)
);

CREATE INDEX idx_users_email    ON users (email);
CREATE INDEX idx_users_username ON users (username);

COMMENT ON TABLE  users                IS 'Application user accounts';
COMMENT ON COLUMN users.password_hash  IS 'BCrypt-hashed password — never stored in plaintext';
COMMENT ON COLUMN users.is_enabled     IS 'FALSE = soft-disabled; user cannot log in';
