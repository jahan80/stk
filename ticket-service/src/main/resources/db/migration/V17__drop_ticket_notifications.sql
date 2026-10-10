-- ==================================================
-- V17: Drop ticket.notifications
-- ==================================================
-- After step D2, all notifications (in-app, email, sms) are routed
-- through NOTIFICATION_REQUESTED events and persisted in
-- notif.notifications (channel=IN_APP for in-app).
--
-- Data loss is acceptable:
--   - dev database
--   - old unread/read state has no business value
--   - frontend migrates to /notify/me
--
-- If you are running this in an environment with historical data
-- you care about, back up first and consider migrating rows into
-- notif.notifications.

DROP TABLE IF EXISTS ticket.notifications;
