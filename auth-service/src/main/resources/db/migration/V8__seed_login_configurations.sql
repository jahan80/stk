INSERT INTO auth.configurations (
    config_key,
    config_value,
    default_value,
    value_type,
    description,
    enabled,
    created_at,
    updated_at
)
VALUES
    (
        'AUTH.LOGIN.ENABLED',
        'true',
        'true',
        'BOOLEAN',
        'Login feature is enabled',
        true,
        NOW(),
        NOW()
    ),
    (
        'AUTH.LOGIN.USERNAME.ENABLED',
        'true',
        'true',
        'BOOLEAN',
        'Username can be used for login',
        true,
        NOW(),
        NOW()
    ),
    (
        'AUTH.LOGIN.EMAIL.ENABLED',
        'true',
        'true',
        'BOOLEAN',
        'Email can be used for login',
        true,
        NOW(),
        NOW()
    ),
    (
        'AUTH.LOGIN.MOBILE_NUMBER.ENABLED',
        'true',
        'true',
        'BOOLEAN',
        'Mobile number can be used for login',
        true,
        NOW(),
        NOW()
    );