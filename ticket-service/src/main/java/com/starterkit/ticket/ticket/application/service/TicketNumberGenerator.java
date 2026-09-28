package com.starterkit.ticket.ticket.application.service;

import com.starterkit.ticket.ticket.domain.repository.TicketRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TicketNumberGenerator {

    private static final String PREFIX = "STK";
    private static final int PADDING = 4;

    private final TicketRepository ticketRepository;

    public synchronized String generate() {
        long count = ticketRepository.count() + 1;
        return String.format("%s-%0" + PADDING + "d", PREFIX, count);
    }
}
