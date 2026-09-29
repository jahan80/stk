-- ==================================================
-- V21: Ticket service permissions
-- ==================================================

INSERT INTO auth.permissions (code, description, created_at, updated_at) VALUES
    ('ticket:read',    'Read tickets',              NOW(), NOW()),
    ('ticket:write',   'Create tickets and comments', NOW(), NOW());

-- Give to ADMIN
INSERT INTO auth.role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM auth.roles r, auth.permissions p
WHERE r.name = 'ADMIN'
  AND p.code IN ('ticket:read', 'ticket:write');

-- Give to USER
INSERT INTO auth.role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM auth.roles r, auth.permissions p
WHERE r.name = 'USER'
  AND p.code IN ('ticket:read', 'ticket:write');
