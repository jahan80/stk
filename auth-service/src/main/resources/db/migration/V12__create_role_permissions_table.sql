-- ==================================================
-- V12: Role-Permissions mapping + soft delete for roles
-- ==================================================

-- ۱. Soft delete روی roles
ALTER TABLE auth.roles
    ADD COLUMN deleted_at TIMESTAMPTZ;

CREATE INDEX idx_roles_deleted_at ON auth.roles(deleted_at);

-- ۲. جدول اتصال
CREATE TABLE auth.role_permissions (
                                       role_id       BIGINT NOT NULL,
                                       permission_id BIGINT NOT NULL,
                                       PRIMARY KEY (role_id, permission_id),
                                       CONSTRAINT fk_role_permissions_role
                                           FOREIGN KEY (role_id) REFERENCES auth.roles(id) ON DELETE CASCADE,
                                       CONSTRAINT fk_role_permissions_permission
                                           FOREIGN KEY (permission_id) REFERENCES auth.permissions(id) ON DELETE CASCADE
);

CREATE INDEX idx_role_permissions_role_id ON auth.role_permissions(role_id);
CREATE INDEX idx_role_permissions_permission_id ON auth.role_permissions(permission_id);

COMMENT ON TABLE auth.role_permissions IS 'M:N mapping between roles and permissions';

-- ۳. به ADMIN همه permissionها را بده
INSERT INTO auth.role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM auth.roles r
         CROSS JOIN auth.permissions p
WHERE r.name = 'ADMIN';

-- ۴. به USER فقط user:read بده
INSERT INTO auth.role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM auth.roles r, auth.permissions p
WHERE r.name = 'USER' AND p.code = 'user:read';