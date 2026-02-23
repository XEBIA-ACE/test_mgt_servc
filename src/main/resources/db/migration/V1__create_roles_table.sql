-- V1: Create roles table and seed default roles

CREATE TABLE IF NOT EXISTS roles (
    id          BIGSERIAL PRIMARY KEY,
    name        VARCHAR(50)  NOT NULL UNIQUE,
    description VARCHAR(255),
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

-- Seed the three default roles
INSERT INTO roles (name, description) VALUES
    ('ROLE_USER',      'Standard user with read/write access to own resources'),
    ('ROLE_MODERATOR', 'Can moderate content and manage standard users'),
    ('ROLE_ADMIN',     'Full administrative access to all resources')
ON CONFLICT (name) DO NOTHING;
