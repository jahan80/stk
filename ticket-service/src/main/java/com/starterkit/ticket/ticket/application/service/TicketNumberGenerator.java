package com.starterkit.ticket.ticket.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

/**
 * Generates unique, monotonically-increasing ticket numbers.
 *
 * Uses a PostgreSQL SEQUENCE (nextval) which is atomic across all
 * instances of ticket-service. This replaces the old COUNT()+1
 * approach which was racy under concurrent ticket creation.
 *
 * Format: {PREFIX}-{zero-padded sequence}
 * Example: STK-0001, STK-0002, ...
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TicketNumberGenerator {

    private static final String SEQUENCE_NAME = "ticket.ticket_number_seq";

    private final JdbcTemplate jdbcTemplate;
    private final TicketConfigurationService config;

    public String generate() {
        String prefix = config.getString("TICKET.NUMBER.PREFIX", "STK");
        int padding = config.getInt("TICKET.NUMBER.PADDING", 4);

        Long nextVal = jdbcTemplate.queryForObject(
                "SELECT nextval('" + SEQUENCE_NAME + "')",
                Long.class
        );

        if (nextVal == null) {
            throw new IllegalStateException("Failed to obtain next ticket number");
        }

        String formatted = String.format("%s-%0" + padding + "d", prefix, nextVal);
        log.debug("Generated ticket number: {}", formatted);
        return formatted;
    }
}
