-- V1: Create core users table
-- Uses UUID primary key for horizontal scalability and privacy (no sequential ID enumeration)

CREATE TABLE IF NOT EXISTS users (
    id                      UUID            NOT NULL DEFAULT gen_random_uuid(),
    username                VARCHAR(50)     NOT NULL,
    email                   VARCHAR(255)    NOT NULL,
    password                VARCHAR(255)    NOT NULL,
    first_name              VARCHAR(100),
    last_name               VARCHAR(100),
    enabled                 BOOLEAN         NOT NULL DEFAULT TRUE,
    account_non_locked      BOOLEAN         NOT NULL DEFAULT TRUE,
    credentials_non_expired BOOLEAN         NOT NULL DEFAULT TRUE,
    created_at              TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at              TIMESTAMPTZ,
    created_by              VARCHAR(100),
    updated_by              VARCHAR(100),

    CONSTRAINT pk_users PRIMARY KEY (id),
    CONSTRAINT uq_users_username UNIQUE (username),
    CONSTRAINT uq_users_email    UNIQUE (email)
);

-- Indexes for common lookup patterns
CREATE INDEX IF NOT EXISTS idx_users_email    ON users (email);
CREATE INDEX IF NOT EXISTS idx_users_username ON users (username);
CREATE INDEX IF NOT EXISTS idx_users_enabled  ON users (enabled);

COMMENT ON TABLE  users IS 'User accounts with authentication credentials and profile data';
COMMENT ON COLUMN users.id                      IS 'UUID primary key';
COMMENT ON COLUMN users.password                IS 'BCrypt-hashed password';
COMMENT ON COLUMN users.enabled                 IS 'False = account deactivated';
COMMENT ON COLUMN users.account_non_locked      IS 'False = account locked (e.g. after failed attempts)';
COMMENT ON COLUMN users.credentials_non_expired IS 'False = password must be reset';
