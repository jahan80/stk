CREATE TABLE auth.configurations (
                                     id BIGSERIAL PRIMARY KEY,

                                     config_key VARCHAR(200) NOT NULL UNIQUE,

                                     config_value TEXT NOT NULL,

                                     value_type VARCHAR(20) NOT NULL,

                                     description VARCHAR(500),

                                     enabled BOOLEAN NOT NULL DEFAULT TRUE,

                                     created_at TIMESTAMPTZ NOT NULL,

                                     updated_at TIMESTAMPTZ NOT NULL
);

INSERT INTO auth.configurations
(config_key, config_value, value_type, description, enabled, created_at, updated_at)
VALUES
    ('AUTH.REGISTER.USERNAME.ENABLED', 'true', 'BOOLEAN',
     'Enable username field in registration', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

    ('AUTH.REGISTER.USERNAME.REQUIRED', 'true', 'BOOLEAN',
     'Username is required during registration', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

    ('AUTH.REGISTER.EMAIL.ENABLED', 'true', 'BOOLEAN',
     'Enable email field in registration', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

    ('AUTH.REGISTER.EMAIL.REQUIRED', 'true', 'BOOLEAN',
     'Email is required during registration', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

    ('AUTH.REGISTER.MOBILE_NUMBER.ENABLED', 'true', 'BOOLEAN',
     'Enable mobile number field in registration', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

    ('AUTH.REGISTER.MOBILE_NUMBER.REQUIRED', 'false', 'BOOLEAN',
     'Mobile number is required during registration', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

    ('AUTH.REGISTER.PASSWORD.ENABLED', 'true', 'BOOLEAN',
     'Enable password field in registration', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

    ('AUTH.REGISTER.PASSWORD.REQUIRED', 'true', 'BOOLEAN',
     'Password is required during registration', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

    ('AUTH.REGISTER.FIRST_NAME.ENABLED', 'true', 'BOOLEAN',
     'Enable first name field in registration', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

    ('AUTH.REGISTER.FIRST_NAME.REQUIRED', 'false', 'BOOLEAN',
     'First name is required during registration', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

    ('AUTH.REGISTER.LAST_NAME.ENABLED', 'true', 'BOOLEAN',
     'Enable last name field in registration', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

    ('AUTH.REGISTER.LAST_NAME.REQUIRED', 'false', 'BOOLEAN',
     'Last name is required during registration', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);