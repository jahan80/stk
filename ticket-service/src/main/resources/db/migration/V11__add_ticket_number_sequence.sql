-- ==================================================
-- V11: Ticket number sequence (race-safe)
-- ==================================================
-- Original: TicketNumberGenerator used COUNT()+1 which is racy
-- across multiple instances. Now we use a PostgreSQL SEQUENCE
-- via nextval() — atomic and cluster-safe.

CREATE SEQUENCE IF NOT EXISTS ticket.ticket_number_seq
    START WITH 1
    INCREMENT BY 1
    NO MAXVALUE
    CACHE 1;

-- Sync sequence to current max ticket id (if any rows exist)
SELECT setval(
    'ticket.ticket_number_seq',
    COALESCE((SELECT MAX(id) FROM ticket.tickets), 0) + 1,
    false
);
