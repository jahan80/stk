-- ==================================================
-- V4: Tickets
-- ==================================================

CREATE TABLE ticket.tickets (
    id              BIGSERIAL    PRIMARY KEY,
    ticket_number   VARCHAR(30)  NOT NULL UNIQUE,
    title           VARCHAR(255) NOT NULL,
    description     TEXT         NOT NULL,
    status          VARCHAR(20)  NOT NULL DEFAULT 'OPEN',
    priority        VARCHAR(20)  NOT NULL DEFAULT 'MEDIUM',
    category_id     BIGINT       NOT NULL,
    group_id        BIGINT,
    created_by      BIGINT       NOT NULL,
    assigned_to     BIGINT,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    resolved_at     TIMESTAMPTZ,
    closed_at       TIMESTAMPTZ,

    CONSTRAINT fk_tickets_category
        FOREIGN KEY (category_id) REFERENCES ticket.ticket_categories(id),

    CONSTRAINT fk_tickets_group
        FOREIGN KEY (group_id) REFERENCES ticket.ticket_groups(id) ON DELETE SET NULL
);

CREATE INDEX idx_tickets_number      ON ticket.tickets(ticket_number);
CREATE INDEX idx_tickets_created_by  ON ticket.tickets(created_by);
CREATE INDEX idx_tickets_assigned_to ON ticket.tickets(assigned_to);
CREATE INDEX idx_tickets_group_id    ON ticket.tickets(group_id);
CREATE INDEX idx_tickets_status      ON ticket.tickets(status);
CREATE INDEX idx_tickets_priority    ON ticket.tickets(priority);
CREATE INDEX idx_tickets_category_id ON ticket.tickets(category_id);
CREATE INDEX idx_tickets_created_at  ON ticket.tickets(created_at DESC);

COMMENT ON TABLE ticket.tickets IS 'Support tickets';
COMMENT ON COLUMN ticket.tickets.status IS 'OPEN, IN_PROGRESS, WAITING, RESOLVED, CLOSED';
COMMENT ON COLUMN ticket.tickets.priority IS 'LOW, MEDIUM, HIGH, URGENT';
