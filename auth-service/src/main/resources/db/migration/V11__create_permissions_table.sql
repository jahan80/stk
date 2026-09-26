-- ==================================================
-- V11: Permissions table
-- ==================================================
CREATE TABLE auth.permissions (
                                  id          BIGSERIAL    PRIMARY KEY,
                                  code        VARCHAR(100) NOT NULL UNIQUE,
                                  description VARCHAR(255),
                                  created_at  TIMESTAMPTZ  NOT NULL,
                                  updated_at  TIMESTAMPTZ  NOT NULL
);

CREATE INDEX idx_permissions_code ON auth.permissions(code);

COMMENT ON TABLE auth.permissions IS 'Fine-grained permissions (RBAC)';
COMMENT ON COLUMN auth.permissions.code IS 'Format: resource:action (e.g. config:write)';

-- Seed initial permissions
INSERT INTO auth.permissions (code, description, created_at, updated_at) VALUES
                                                                             ('user:read',        'Read user information',           NOW(), NOW()),
                                                                             ('user:write',       'Create or update users',          NOW(), NOW()),
                                                                             ('user:delete',      'Delete users',                    NOW(), NOW()),
                                                                             ('user:assign-role', 'Assign roles to users',           NOW(), NOW()),
                                                                             ('config:read',      'Read configurations',             NOW(), NOW()),
                                                                             ('config:write',     'Create or update configurations', NOW(), NOW()),
                                                                             ('config:delete',    'Delete configurations',           NOW(), NOW()),
                                                                             ('role:read',        'Read roles',                      NOW(), NOW()),
                                                                             ('role:write',       'Create or update roles',          NOW(), NOW()),
                                                                             ('role:delete',      'Delete roles',                    NOW(), NOW()),
                                                                             ('audit:read',       'Read audit logs',                 NOW(), NOW());