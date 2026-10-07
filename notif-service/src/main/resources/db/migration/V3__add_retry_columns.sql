-- ==================================================
-- V3: Retry / attempts tracking for notifications
-- ==================================================
-- Rationale:
--   The old idempotency guard skipped ANY event whose event_id
--   already existed, which silently killed provider-level retries.
--   Now we distinguish:
--     - event received  (row exists)
--     - notification delivered (status=SENT)
--   A duplicate delivery is only skipped when status=SENT.
--   FAILED / PENDING rows are reused and retried on the same row.
--
-- New columns:
--   attempts         : number of delivery attempts so far
--   last_error       : most recent failure reason (already have error_message,
--                      but we keep both: error_message = user-facing summary,
--                      last_error = raw provider/technical error)
--   next_attempt_at  : when this row becomes eligible for retry
--   locked_until     : lease for multi-instance retry job
--   claimed_by       : which instance is currently retrying this row

ALTER TABLE notif.notifications
    ADD COLUMN IF NOT EXISTS attempts        INT          NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS last_error      TEXT,
    ADD COLUMN IF NOT EXISTS next_attempt_at TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    ADD COLUMN IF NOT EXISTS locked_until    TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS claimed_by      VARCHAR(100);

-- Retry job query index: (status, next_attempt_at)
CREATE INDEX IF NOT EXISTS idx_notifications_retry
    ON notif.notifications (status, next_attempt_at)
    WHERE status IN ('PENDING', 'FAILED');

-- Backfill: existing PENDING/FAILED rows become immediately eligible
UPDATE notif.notifications
SET next_attempt_at = NOW()
WHERE next_attempt_at IS NULL;

COMMENT ON COLUMN notif.notifications.attempts
    IS 'Number of delivery attempts (incremented per try)';
COMMENT ON COLUMN notif.notifications.next_attempt_at
    IS 'Earliest time this row may be retried by the retry job';
COMMENT ON COLUMN notif.notifications.locked_until
    IS 'Lease for multi-instance retry job (reclaim after expiry)';
COMMENT ON COLUMN notif.notifications.claimed_by
    IS 'Instance ID currently retrying this row';
