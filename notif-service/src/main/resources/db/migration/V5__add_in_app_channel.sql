-- ==================================================
-- V5: Extend notifications for IN_APP channel
-- ==================================================
-- In-app notifications need extra fields that email/SMS do not:
--   read_at, link, ticket_id, actor_id, recipient_user_id
--
-- Order matters: add ALL columns first, THEN create indexes
-- that reference them.

-- Step 1: add columns
ALTER TABLE notif.notifications
    ADD COLUMN IF NOT EXISTS read_at            TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS link               VARCHAR(500),
    ADD COLUMN IF NOT EXISTS ticket_id          BIGINT,
    ADD COLUMN IF NOT EXISTS actor_id           BIGINT,
    ADD COLUMN IF NOT EXISTS recipient_user_id  BIGINT;

-- Step 2: indexes (safe now that columns exist)
CREATE INDEX IF NOT EXISTS idx_notif_in_app_unread
    ON notif.notifications (recipient_user_id, read_at)
    WHERE status = 'SENT' AND read_at IS NULL AND channel = 'IN_APP';

CREATE INDEX IF NOT EXISTS idx_notif_recipient_user
    ON notif.notifications (recipient_user_id, created_at DESC);

-- Step 3: comments
COMMENT ON COLUMN notif.notifications.channel
    IS 'SMS, EMAIL, PUSH, IN_APP';
COMMENT ON COLUMN notif.notifications.recipient_user_id
    IS 'Set for IN_APP channel; NULL for external channels';
COMMENT ON COLUMN notif.notifications.read_at
    IS 'IN_APP only: when the user read the notification';
COMMENT ON COLUMN notif.notifications.link
    IS 'IN_APP only: frontend deep link (e.g. /tickets/42)';
COMMENT ON COLUMN notif.notifications.ticket_id
    IS 'IN_APP only: ticket context (for ticket notifications)';
COMMENT ON COLUMN notif.notifications.actor_id
    IS 'IN_APP only: user who triggered this notification';
