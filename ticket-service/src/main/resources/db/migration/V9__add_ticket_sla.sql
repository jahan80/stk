ALTER TABLE ticket.tickets
    ADD COLUMN sla_deadline TIMESTAMPTZ;

CREATE INDEX idx_tickets_sla_deadline ON ticket.tickets(sla_deadline);
