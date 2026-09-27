INSERT INTO gateway.rate_limits 
    (path_pattern, method, key_type, requests_per_window, window_seconds,
     default_requests_per_window, default_window_seconds,
     default_enabled, enabled, priority, description, created_at, updated_at)
VALUES
    ('/auth/login', 'POST', 'IP_PATH', 5, 60, 5, 60, TRUE, TRUE, 100,
     'Login (brute-force protection)', NOW(), NOW()),
    
    ('/auth/register', 'POST', 'IP_PATH', 3, 3600, 3, 3600, TRUE, TRUE, 100,
     'User registration', NOW(), NOW()),
    
    ('/auth/password/forgot', 'POST', 'IP_PATH', 3, 3600, 3, 3600, TRUE, TRUE, 100,
     'Password reset request', NOW(), NOW()),
    
    ('/auth/password/reset', 'POST', 'IP_PATH', 5, 3600, 5, 3600, TRUE, TRUE, 100,
     'Password reset attempts', NOW(), NOW()),
    
    ('/auth/email/verify', 'POST', 'IP_PATH', 10, 60, 10, 60, TRUE, TRUE, 100,
     'Email verification', NOW(), NOW()),
    
    ('/auth/email/resend-verification', 'POST', 'IP_PATH', 3, 3600, 3, 3600, TRUE, TRUE, 100,
     'Email resend', NOW(), NOW()),
    
    ('/auth/mobile/verify', 'POST', 'IP_PATH', 10, 60, 10, 60, TRUE, TRUE, 100,
     'Mobile verification', NOW(), NOW()),
    
    ('/auth/mobile/resend-verification', 'POST', 'IP_PATH', 3, 3600, 3, 3600, TRUE, TRUE, 100,
     'Mobile resend', NOW(), NOW()),
    
    ('/auth/refresh', 'POST', 'IP_PATH', 20, 60, 20, 60, TRUE, TRUE, 100,
     'Token refresh', NOW(), NOW()),
    
    ('/auth/me', 'GET', 'IP_PATH', 60, 60, 60, 60, TRUE, TRUE, 50,
     'Current user info', NOW(), NOW()),
    
    ('/auth/**', NULL, 'IP_PATH', 100, 60, 100, 60, TRUE, TRUE, 0,
     'Default auth', NOW(), NOW()),
    
    ('/audit/**', NULL, 'IP_PATH', 60, 60, 60, 60, TRUE, TRUE, 0,
     'Default audit', NOW(), NOW()),
    
    ('/notify/**', NULL, 'IP_PATH', 30, 60, 30, 60, TRUE, TRUE, 0,
     'Default notify', NOW(), NOW());
