-- ==================================================
-- V26: Role name UNIQUE only for non-deleted roles
-- ==================================================
-- Original constraint: UNIQUE (name) — blocks creating a role
-- with the same name even after the old one was soft-deleted.
--
-- Fix: partial unique index that ignores soft-deleted rows.

ALTER TABLE auth.roles
    DROP CONSTRAINT IF EXISTS roles_name_key;

-- Also drop any old index with the same name (from earlier migrations)
DROP INDEX IF EXISTS auth.roles_name_key;

CREATE UNIQUE INDEX uk_roles_name_active
    ON auth.roles (name)
    WHERE deleted_at IS NULL;

COMMENT ON INDEX auth.uk_roles_name_active IS
    'Role name is unique among non-deleted roles';
