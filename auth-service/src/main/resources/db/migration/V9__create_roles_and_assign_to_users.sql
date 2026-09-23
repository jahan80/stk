CREATE TABLE auth.roles (
    id          BIGSERIAL    PRIMARY KEY,
    name        VARCHAR(50)  NOT NULL UNIQUE,
    description VARCHAR(255),
    system_role BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at  TIMESTAMPTZ  NOT NULL,
    updated_at  TIMESTAMPTZ  NOT NULL
);

CREATE INDEX idx_roles_name ON auth.roles(name);

COMMENT ON TABLE auth.roles IS 'User roles (RBAC)';
COMMENT ON COLUMN auth.roles.system_role IS 'System roles cannot be deleted';

INSERT INTO auth.roles (name, description, system_role, created_at, updated_at)
VALUES
    ('ADMIN', 'System administrator with full access', TRUE, NOW(), NOW()),
    ('USER',  'Standard user',                         TRUE, NOW(), NOW());

ALTER TABLE auth.users
    ADD COLUMN role_id BIGINT;

UPDATE auth.users
SET role_id = (SELECT id FROM auth.roles WHERE name = 'USER')
WHERE role_id IS NULL;

ALTER TABLE auth.users
    ALTER COLUMN role_id SET NOT NULL;

ALTER TABLE auth.users
    ADD CONSTRAINT fk_users_role
        FOREIGN KEY (role_id) REFERENCES auth.roles(id)
        ON DELETE RESTRICT;

CREATE INDEX idx_users_role_id ON auth.users(role_id);

COMMENT ON COLUMN auth.users.role_id IS 'User role (single-role model)';
