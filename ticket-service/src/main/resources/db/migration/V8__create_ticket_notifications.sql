-- ==================================================
-- V8: Ticket notifications (in-app)
-- ==================================================

CREATE TABLE ticket.notifications (
    id              BIGSERIAL    PRIMARY KEY,
    user_id         BIGINT       NOT NULL,
    type            VARCHAR(50)  NOT NULL,
    title           VARCHAR(255) NOT NULL,
    message         TEXT,
    ticket_id       BIGINT,
    actor_id        BIGINT,
    read            BOOLEAN      NOT NULL DEFAULT FALSE,
    read_at         TIMESTAMPTZ,
    link            VARCHAR(500),
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_notifications_ticket
        FOREIGN KEY (ticket_id) REFERENCES ticket.tickets(id) ON DELETE CASCADE
);

CREATE INDEX idx_notif_user        ON ticket.notifications(user_id);
CREATE INDEX idx_notif_user_unread ON ticket.notifications(user_id) WHERE read = FALSE;
CREATE INDEX idx_notif_created     ON ticket.notifications(created_at DESC);

COMMENT ON TABLE ticket.notifications IS 'In-app notifications for ticket events';
COMMENT ON COLUMN ticket.notifications.type IS 'TICKET_CREATED, TICKET_ASSIGNED, TICKET_COMMENTED, TICKET_STATUS_CHANGED, TICKET_MENTIONED';
