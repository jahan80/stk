-- ==================================================
-- V4: Add DEFAULT NOW() to notifications.created_at
-- ==================================================
-- Rationale:
--   The entity sets created_at in @PrePersist, but direct SQL inserts
--   (tests, imports, manual backfills) fail with NOT NULL violation.
--   Adding a DB-level default makes the schema self-sufficient.

ALTER TABLE notif.notifications
    ALTER COLUMN created_at SET DEFAULT NOW();

COMMENT ON COLUMN notif.notifications.created_at
    IS 'Row creation time; defaults to NOW() at DB level';
