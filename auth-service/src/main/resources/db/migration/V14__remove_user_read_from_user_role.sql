-- ==================================================
-- V14: Remove user:read from USER role
-- Reason: USER should not list all users
-- ==================================================

DELETE FROM auth.role_permissions
WHERE role_id = (SELECT id FROM auth.roles WHERE name = 'USER')
  AND permission_id = (SELECT id FROM auth.permissions WHERE code = 'user:read');
