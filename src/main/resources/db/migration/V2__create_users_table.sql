-- V2: Create users table with audit columns

CREATE TABLE IF NOT EXISTS users (
    id                      UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    email                   VARCHAR(100) NOT NULL UNIQUE,
    username                VARCHAR(50)  NOT NULL UNIQUE,
    password_hash           TEXT         NOT NULL,
    first_name              VARCHAR(100),
    last_name               VARCHAR(100),
    is_enabled              BOOLEAN      NOT NULL DEFAULT TRUE,
    is_account_non_locked   BOOLEAN      NOT NULL DEFAULT TRUE,
    is_email_verified       BOOLEAN      NOT NULL DEFAULT FALSE,
    failed_login_attempts   INT          NOT NULL DEFAULT 0,
    last_login_at           TIMESTAMPTZ,
    created_at              TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at              TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    created_by              VARCHAR(100),
    updated_by              VARCHAR(100)
);

CREATE INDEX IF NOT EXISTS idx_users_email    ON users (email);
CREATE INDEX IF NOT EXISTS idx_users_username ON users (username);
CREATE INDEX IF NOT EXISTS idx_users_enabled  ON users (is_enabled) WHERE is_enabled = TRUE;
