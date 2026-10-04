-- ==================================================
-- V15: Notification channel configs (email/sms)
-- ==================================================
-- In-app notifications are always created (cheap, UI-only).
-- External channels (email, sms) are opt-in via config.
--
-- TICKET.NOTIF.IN_APP.ENABLED : always-on (reserved, currently ignored)
-- TICKET.NOTIF.EMAIL.ENABLED  : send email via notif-service
-- TICKET.NOTIF.SMS.ENABLED    : send sms   via notif-service

INSERT INTO ticket.configurations
    (config_key, config_value, default_value, value_type, description, enabled)
VALUES
    ('TICKET.NOTIF.IN_APP.ENABLED',
     'true', 'true', 'BOOLEAN',
     'Create in-app notifications (always on by design)', TRUE),

    ('TICKET.NOTIF.EMAIL.ENABLED',
     'false', 'false', 'BOOLEAN',
     'Send external email notifications via notif-service', TRUE),

    ('TICKET.NOTIF.SMS.ENABLED',
     'false', 'false', 'BOOLEAN',
     'Send external SMS notifications via notif-service', TRUE);
