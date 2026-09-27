INSERT INTO auth.configurations 
    (config_key, config_value, default_value, value_type, description, enabled, created_at, updated_at)
VALUES
    ('AUTH.REGISTER.MOBILE.VERIFICATION.REQUIRED', 'false', 'false', 'BOOLEAN',
     'Require mobile verification after registration', true, NOW(), NOW()),
    ('AUTH.LOGIN.MOBILE.VERIFIED.REQUIRED', 'false', 'false', 'BOOLEAN',
     'Require verified mobile for login', true, NOW(), NOW()),
    ('AUTH.MOBILE.VERIFICATION.CODE.LENGTH', '6', '6', 'INTEGER',
     'Length of mobile verification code', true, NOW(), NOW()),
    ('AUTH.MOBILE.VERIFICATION.TOKEN.TTL.SECONDS', '300', '300', 'LONG',
     'Mobile verification code TTL in seconds', true, NOW(), NOW()),
    ('AUTH.MOBILE.VERIFICATION.MAX.ATTEMPTS', '5', '5', 'INTEGER',
     'Max verification attempts before token invalidation', true, NOW(), NOW());
