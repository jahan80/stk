-- ==================================================
-- V13: Add permission:read permission
-- ==================================================

INSERT INTO auth.permissions (code, description, created_at, updated_at)
VALUES ('permission:read', 'Read permissions', NOW(), NOW());

-- Give to ADMIN
INSERT INTO auth.role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM auth.roles r, auth.permissions p
WHERE r.name = 'ADMIN' AND p.code = 'permission:read';
