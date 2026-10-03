-- ==================================================
-- V3: Fix unique constraint to handle NULL method
-- ==================================================
-- Original constraint: UNIQUE (path_pattern, method, key_type)
-- Problem: PostgreSQL treats NULL as distinct, so multiple rows with
--          method=NULL and same (path_pattern, key_type) could be inserted.
-- Fix: Use a partial unique index with COALESCE.

-- Drop the old constraint
ALTER TABLE gateway.rate_limits
    DROP CONSTRAINT IF EXISTS uk_rate_limits_path_method_key;

-- Create a unique index that treats NULL method as empty string
CREATE UNIQUE INDEX uk_rate_limits_path_method_key
    ON gateway.rate_limits (path_pattern, COALESCE(method, ''), key_type);

COMMENT ON INDEX gateway.uk_rate_limits_path_method_key IS
    'Unique per (path, method, key). COALESCE handles NULL method.';
