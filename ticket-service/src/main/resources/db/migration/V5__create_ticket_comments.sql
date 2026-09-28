-- ==================================================
-- V5: Ticket comments
-- ==================================================

CREATE TABLE ticket.ticket_comments (
    id              BIGSERIAL    PRIMARY KEY,
    ticket_id       BIGINT       NOT NULL,
    author_id       BIGINT       NOT NULL,
    author_role     VARCHAR(20)  NOT NULL,
    body            TEXT         NOT NULL,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_comments_ticket
        FOREIGN KEY (ticket_id) REFERENCES ticket.tickets(id) ON DELETE CASCADE
);

CREATE INDEX idx_comments_ticket_id  ON ticket.ticket_comments(ticket_id);
CREATE INDEX idx_comments_created_at ON ticket.ticket_comments(created_at);

COMMENT ON TABLE ticket.ticket_comments IS 'Comments on tickets';
COMMENT ON COLUMN ticket.ticket_comments.author_role IS 'USER, ADMIN, AGENT, SYSTEM';
