INSERT INTO auth.configurations 
    (config_key, config_value, default_value, value_type, description, enabled, created_at, updated_at)
VALUES
    ('AUTH.REGISTER.EMAIL.VERIFICATION.REQUIRED', 'true', 'true', 'BOOLEAN',
     'Require email verification after registration', true, NOW(), NOW()),
    ('AUTH.LOGIN.EMAIL.VERIFIED.REQUIRED', 'true', 'true', 'BOOLEAN',
     'Require verified email for login', true, NOW(), NOW()),
    ('AUTH.EMAIL.VERIFICATION.CODE.LENGTH', '8', '8', 'INTEGER',
     'Length of verification code', true, NOW(), NOW()),
    ('AUTH.EMAIL.VERIFICATION.TOKEN.TTL.SECONDS', '600', '600', 'LONG',
     'Verification code TTL in seconds', true, NOW(), NOW()),
    ('AUTH.EMAIL.VERIFICATION.MAX.ATTEMPTS', '5', '5', 'INTEGER',
     'Max attempts before token invalidation', true, NOW(), NOW()),
    ('AUTH.EMAIL.VERIFICATION.RESEND.COOLDOWN.SECONDS', '60', '60', 'INTEGER',
     'Cooldown between resend requests', true, NOW(), NOW());
