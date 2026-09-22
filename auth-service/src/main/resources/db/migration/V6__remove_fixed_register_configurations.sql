DELETE FROM auth.configurations
WHERE config_key IN (
                     'AUTH.REGISTER.USERNAME.ENABLED',
                     'AUTH.REGISTER.USERNAME.REQUIRED',
                     'AUTH.REGISTER.PASSWORD.ENABLED',
                     'AUTH.REGISTER.PASSWORD.REQUIRED'
    );