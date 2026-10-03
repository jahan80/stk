-- ==================================================
-- V14: Re-sync ticket_number_seq from actual ticket_number values
-- ==================================================
-- V11 synced the sequence from MAX(id), which is WRONG:
--   id can diverge from the numeric part of ticket_number
--   (e.g. after deletes, imports, or manual inserts).
--
-- Example of the bug:
--   id=10 → STK-0100
--   id=11 → STK-0101
--   id=20 → STK-0150    (MAX(id)=20, but max ticket number=150)
--   → V11 setval to 21, next ticket becomes STK-0021 (collision!)
--
-- This migration extracts the numeric suffix from ticket_number
-- and sets the sequence accordingly.

DO $$
DECLARE
    max_num BIGINT;
BEGIN
    -- Extract numeric suffix from ticket_number (e.g. 'STK-0100' → 100)
    SELECT COALESCE(MAX(
        CAST(
            SUBSTRING(ticket_number FROM '(\d+)$') AS BIGINT
        )
    ), 0)
    INTO max_num
    FROM ticket.tickets
    WHERE ticket_number ~ '\d+$';

    -- setval with is_called=false means NEXT nextval() returns max_num+1
    IF max_num > 0 THEN
        PERFORM setval('ticket.ticket_number_seq', max_num, true);
    ELSE
        PERFORM setval('ticket.ticket_number_seq', 1, false);
    END IF;

    RAISE NOTICE 'ticket_number_seq resynced: max_num=%', max_num;
END $$;

-- Guard: verify no duplicate ticket_number suffixes exist
-- (this would indicate a previous data problem)
DO $$
DECLARE
    dup_count INT;
BEGIN
    SELECT COUNT(*) INTO dup_count
    FROM (
        SELECT SUBSTRING(ticket_number FROM '(\d+)$') AS num
        FROM ticket.tickets
        WHERE ticket_number ~ '\d+$'
        GROUP BY 1
        HAVING COUNT(*) > 1
    ) dups;

    IF dup_count > 0 THEN
        RAISE WARNING 'Found % duplicate ticket number suffix(es) — investigate!', dup_count;
    END IF;
END $$;
