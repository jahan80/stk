INSERT INTO auth.configurations 
    (config_key, config_value, default_value, value_type, description, enabled, created_at, updated_at)
VALUES
    ('AUTH.PASSWORD.RESET.ENABLED', 'true', 'true', 'BOOLEAN',
     'Password reset feature enabled', true, NOW(), NOW()),
    ('AUTH.PASSWORD.RESET.CODE.LENGTH', '6', '6', 'INTEGER',
     'Length of password reset code', true, NOW(), NOW()),
    ('AUTH.PASSWORD.RESET.TOKEN.TTL.SECONDS', '900', '900', 'LONG',
     'Password reset code TTL in seconds', true, NOW(), NOW()),
    ('AUTH.PASSWORD.RESET.MAX.ATTEMPTS', '5', '5', 'INTEGER',
     'Max attempts before token invalidation', true, NOW(), NOW()),
    ('AUTH.PASSWORD.RESET.REVOKE.SESSIONS', 'true', 'true', 'BOOLEAN',
     'Revoke all refresh tokens after password reset', true, NOW(), NOW());
