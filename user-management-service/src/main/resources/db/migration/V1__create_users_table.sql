-- ============================================================
-- V1: Create users table
-- ============================================================

CREATE EXTENSION IF NOT EXISTS "pgcrypto";

CREATE TABLE IF NOT EXISTS users (
    id                        UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    username                  VARCHAR(50)  NOT NULL UNIQUE,
    email                     VARCHAR(255) NOT NULL UNIQUE,
    password                  VARCHAR(255) NOT NULL,
    first_name                VARCHAR(100),
    last_name                 VARCHAR(100),
    role                      VARCHAR(20)  NOT NULL DEFAULT 'USER',
    is_enabled                BOOLEAN      NOT NULL DEFAULT TRUE,
    is_account_non_expired    BOOLEAN      NOT NULL DEFAULT TRUE,
    is_account_non_locked     BOOLEAN      NOT NULL DEFAULT TRUE,
    is_credentials_non_expired BOOLEAN     NOT NULL DEFAULT TRUE,
    created_at                TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at                TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Indexes for common lookup patterns
CREATE INDEX IF NOT EXISTS idx_users_email    ON users(email);
CREATE INDEX IF NOT EXISTS idx_users_username ON users(username);

-- Auto-update updated_at on every row modification
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_users_updated_at
    BEFORE UPDATE ON users
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();
