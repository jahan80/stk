package com.starterkit.ticket.ticket.application.service;

import com.starterkit.ticket.ticket.domain.repository.TicketRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TicketNumberGenerator {

    private final TicketRepository ticketRepository;
    private final TicketConfigurationService config;

    public synchronized String generate() {
        String prefix = config.getString("TICKET.NUMBER.PREFIX", "STK");
        int padding = config.getInt("TICKET.NUMBER.PADDING", 4);
        long count = ticketRepository.count() + 1;
        return String.format("%s-%0" + padding + "d", prefix, count);
    }
}
