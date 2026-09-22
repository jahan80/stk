ALTER TABLE auth.configurations
    ADD COLUMN default_value TEXT;

UPDATE auth.configurations
SET default_value = config_value
WHERE default_value IS NULL;

ALTER TABLE auth.configurations
    ALTER COLUMN default_value SET NOT NULL;