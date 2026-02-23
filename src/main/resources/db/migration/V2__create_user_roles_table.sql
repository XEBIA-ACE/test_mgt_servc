-- V2: User roles (many-to-one mapping stored as element collection)
CREATE TABLE IF NOT EXISTS user_roles (
    user_id UUID        NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    role    VARCHAR(50) NOT NULL,

    CONSTRAINT pk_user_roles PRIMARY KEY (user_id, role)
);

CREATE INDEX idx_user_roles_user_id ON user_roles (user_id);

-- Seed default admin user (password: Admin@12345 — CHANGE BEFORE GOING TO PROD)
INSERT INTO users (id, username, email, password_hash, first_name, is_enabled)
VALUES (
    gen_random_uuid(),
    'admin',
    'admin@example.com',
    '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/LewdBPj4t.qBV/Kiq', -- Admin@12345
    'System',
    TRUE
)
ON CONFLICT (username) DO NOTHING;

INSERT INTO user_roles (user_id, role)
SELECT id, 'ROLE_ADMIN'
FROM   users
WHERE  username = 'admin'
ON CONFLICT DO NOTHING;
