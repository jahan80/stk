-- ==================================================
-- V1: Notifications table
-- ==================================================
CREATE TABLE notif.notifications (
    id                   BIGSERIAL    PRIMARY KEY,
    notification_id      UUID         NOT NULL UNIQUE,
    channel              VARCHAR(20)  NOT NULL,
    recipient            VARCHAR(255) NOT NULL,
    subject              VARCHAR(500),
    body                 TEXT         NOT NULL,
    status               VARCHAR(20)  NOT NULL,
    provider             VARCHAR(50)  NOT NULL,
    provider_message_id  VARCHAR(255),
    error_message        TEXT,
    metadata             JSONB,
    created_at           TIMESTAMPTZ  NOT NULL,
    sent_at              TIMESTAMPTZ
);

CREATE INDEX idx_notifications_channel ON notif.notifications(channel);
CREATE INDEX idx_notifications_status ON notif.notifications(status);
CREATE INDEX idx_notifications_recipient ON notif.notifications(recipient);
CREATE INDEX idx_notifications_created_at ON notif.notifications(created_at DESC);
CREATE INDEX idx_notifications_provider ON notif.notifications(provider);

COMMENT ON TABLE notif.notifications IS 'Stores all notifications sent through the system';
COMMENT ON COLUMN notif.notifications.channel IS 'SMS, EMAIL, PUSH';
COMMENT ON COLUMN notif.notifications.status IS 'PENDING, SENT, FAILED';
COMMENT ON COLUMN notif.notifications.provider IS 'MOCK, SMTP, KAVENEGAR, FCM';
