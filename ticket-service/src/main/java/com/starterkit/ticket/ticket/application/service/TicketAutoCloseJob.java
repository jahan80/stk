package com.starterkit.ticket.ticket.application.service;

import com.starterkit.ticket.ticket.domain.entity.Ticket;
import com.starterkit.ticket.ticket.domain.entity.TicketStatus;
import com.starterkit.ticket.ticket.domain.repository.TicketRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * P2-11: Auto-close RESOLVED tickets after a configurable number of days.
 *
 * Config keys (ticket.configurations):
 *   TICKET.AUTO_CLOSE.ENABLED        = true | false  (default: false)
 *   TICKET.AUTO_CLOSE.RESOLVED.DAYS  = integer       (default: 7)
 *
 * Safety:
 *   - Disabled by default. Must be explicitly enabled in config.
 *   - Only touches tickets that are still RESOLVED at close time
 *     (a ticket resolved and re-opened in between will not be closed).
 *   - Runs daily at 3 AM UTC to avoid competing with user traffic.
 *   - Bounded batch (500 per run) to avoid a giant single transaction.
 *
 * Note: this is intentionally conservative. It does NOT send external
 * notifications — closing an old ticket is a cleanup operation, not a
 * user-facing event that deserves an email.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TicketAutoCloseJob {

    private static final int MAX_BATCH = 500;

    private final TicketRepository ticketRepo;
    private final TicketConfigurationService config;

    @Scheduled(cron = "0 0 3 * * *", zone = "UTC")
    @Transactional
    public void autoCloseResolvedTickets() {

        boolean enabled = config.getBoolean("TICKET.AUTO_CLOSE.ENABLED", false);
        if (!enabled) {
            log.debug("Auto-close disabled (TICKET.AUTO_CLOSE.ENABLED=false)");
            return;
        }

        int days = config.getInt("TICKET.AUTO_CLOSE.RESOLVED.DAYS", 7);
        if (days < 1) {
            log.warn("Auto-close: RESOLVED.DAYS={} invalid, skipping", days);
            return;
        }

        Instant cutoff = Instant.now().minus(days, ChronoUnit.DAYS);

        List<Ticket> candidates = ticketRepo.findAutoCloseCandidates(
                TicketStatus.RESOLVED, cutoff, MAX_BATCH);

        if (candidates.isEmpty()) {
            log.debug("Auto-close: no candidates (cutoff={})", cutoff);
            return;
        }

        Instant now = Instant.now();
        int closed = 0;

        for (Ticket t : candidates) {
            // Re-check status in-memory (safe: entity was just loaded)
            if (t.getStatus() != TicketStatus.RESOLVED) {
                continue;
            }
            // Only close if resolvedAt is before cutoff (defense against
            // a concurrent reopen that also changed resolvedAt).
            if (t.getResolvedAt() == null || t.getResolvedAt().isAfter(cutoff)) {
                continue;
            }

            t.setStatus(TicketStatus.CLOSED);
            t.setClosedAt(now);
            closed++;
        }

        // Dirty checking flushes changes at commit.
        log.info("Auto-close: closed {} RESOLVED ticket(s) older than {} days",
                closed, days);
    }
}
