-- ==================================================
-- V2: Add event_id to notifications
-- ==================================================
-- eventId is the source event's UUID (from the producer service).
-- It is used for idempotency: if the same RabbitMQ message is
-- delivered more than once, we must NOT send the notification again.
--
-- For existing rows (created before this migration), we backfill
-- event_id with notification_id (each row is its own pseudo-event).

ALTER TABLE notif.notifications
    ADD COLUMN event_id UUID;

-- Backfill: existing rows get a synthetic event_id
UPDATE notif.notifications
SET event_id = notification_id
WHERE event_id IS NULL;

-- Enforce non-null + uniqueness
ALTER TABLE notif.notifications
    ALTER COLUMN event_id SET NOT NULL;

ALTER TABLE notif.notifications
    ADD CONSTRAINT uk_notifications_event_id UNIQUE (event_id);

CREATE INDEX idx_notifications_event_id ON notif.notifications(event_id);

COMMENT ON COLUMN notif.notifications.event_id
    IS 'Source event UUID (for idempotency); unique per notification';
