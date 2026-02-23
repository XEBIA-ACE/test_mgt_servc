-- V2: Create roles and user_roles join table

CREATE TABLE IF NOT EXISTS roles (
    id      BIGSERIAL       NOT NULL,
    name    VARCHAR(50)     NOT NULL,

    CONSTRAINT pk_roles      PRIMARY KEY (id),
    CONSTRAINT uq_roles_name UNIQUE (name)
);

COMMENT ON TABLE  roles      IS 'Application roles (authorities)';
COMMENT ON COLUMN roles.name IS 'Role identifier, e.g. ROLE_USER, ROLE_ADMIN';

-- Many-to-many join table between users and roles
CREATE TABLE IF NOT EXISTS user_roles (
    user_id UUID    NOT NULL,
    role_id BIGINT  NOT NULL,

    CONSTRAINT pk_user_roles         PRIMARY KEY (user_id, role_id),
    CONSTRAINT fk_user_roles_user    FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_user_roles_role    FOREIGN KEY (role_id) REFERENCES roles (id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_user_roles_user_id ON user_roles (user_id);
CREATE INDEX IF NOT EXISTS idx_user_roles_role_id ON user_roles (role_id);

-- Seed default roles
INSERT INTO roles (name) VALUES
    ('ROLE_USER'),
    ('ROLE_ADMIN'),
    ('ROLE_MODERATOR')
ON CONFLICT (name) DO NOTHING;
