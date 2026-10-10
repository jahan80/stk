package com.starterkit.ticket.ticket.application.service.ticket;

import com.starterkit.ticket.shared.infrastructure.jwt.UserPrincipal;
import com.starterkit.ticket.ticket.api.dto.*;
import com.starterkit.ticket.ticket.application.event.*;
import com.starterkit.ticket.ticket.application.exception.*;
import com.starterkit.ticket.ticket.application.mapper.TicketMapper;
import com.starterkit.ticket.ticket.application.service.TicketAccessService;
import com.starterkit.ticket.ticket.application.service.TicketConfigurationService;
import com.starterkit.ticket.ticket.application.service.TicketNotificationFacade;
import com.starterkit.ticket.ticket.application.service.TicketNumberGenerator;
import com.starterkit.ticket.ticket.domain.entity.*;
import com.starterkit.ticket.ticket.domain.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Command side of the ticket domain: create, close, change status.
 *
 * Notifications are emitted as NOTIFICATION_REQUESTED events via
 * TicketNotificationFacade (which goes through the transactional
 * outbox). notif-service decides how to deliver them.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TicketCommandService {

    private final TicketRepository ticketRepo;
    private final TicketCategoryRepository categoryRepo;
    private final TicketNumberGenerator numberGen;
    private final TicketAccessService access;
    private final TicketMapper mapper;
    private final TicketConfigurationService config;
    private final TicketEventPublisher eventPublisher;
    private final TicketNotificationFacade notificationFacade;

    @Transactional
    public TicketResponse create(UserPrincipal user, CreateTicketRequest req) {
        if (!config.getBoolean("TICKET.CREATE.ENABLED", true)) {
            throw new IllegalStateException("Ticket creation is disabled");
        }

        TicketCategory category = categoryRepo.findById(req.getCategoryId())
                .orElseThrow(() -> new CategoryNotFoundException(req.getCategoryId()));

        Ticket t = new Ticket();
        t.setTicketNumber(numberGen.generate());
        t.setTitle(req.getTitle());
        t.setDescription(req.getDescription());
        t.setCategoryId(category.getId());
        t.setPriority(req.getPriority() != null ? req.getPriority() : TicketPriority.MEDIUM);
        t.setStatus(TicketStatus.OPEN);
        t.setCreatedBy(user.getId());
        t.setSlaDeadline(computeSlaDeadline(t.getPriority()));

        Ticket saved = ticketRepo.save(t);
        log.info("Ticket created: {} by user={}", saved.getTicketNumber(), user.getId());

        eventPublisher.publish(new TicketCreatedEvent(
                saved.getId(),
                saved.getTicketNumber(),
                saved.getTitle(),
                user.getId(),
                user.getEmail(),
                saved.getPriority().name(),
                category.getName()
        ));

        boolean notifyAdmins = config.getBoolean("TICKET.NOTIFY.ADMIN.ON_CREATE", true);
        if (notifyAdmins) {
            notificationFacade.notifyAdmins(
                    user.getId(),
                    NotificationType.TICKET_CREATED,
                    String.format("New ticket: %s", saved.getTicketNumber()),
                    saved.getTitle(),
                    saved.getId(),
                    "/tickets/" + saved.getId()
            );
        }

        return toResponse(saved, true, user);
    }

    @Transactional
    public TicketResponse close(UserPrincipal user, Long id) {
        Ticket t = ticketRepo.findById(id)
                .orElseThrow(() -> new TicketNotFoundException(id));

        if (!t.isOwnedBy(user.getId()) && !user.isAdmin()) {
            throw new TicketAccessDeniedException();
        }

        if (t.getStatus() == TicketStatus.CLOSED) {
            throw new IllegalStateException("Ticket already closed");
        }

        TicketStatus old = t.getStatus();
        t.setStatus(TicketStatus.CLOSED);
        t.setClosedAt(Instant.now());
        Ticket saved = ticketRepo.save(t);

        eventPublisher.publish(new TicketStatusChangedEvent(
                saved.getId(), saved.getTicketNumber(),
                old.name(), "CLOSED", user.getId(), saved.getCreatedBy()));

        notifyStatusChange(saved, old, TicketStatus.CLOSED, user.getId());

        return toResponse(saved, true, user);
    }

    @Transactional
    public TicketResponse changeStatus(UserPrincipal user, Long id, TicketStatus newStatus) {
        Ticket t = ticketRepo.findById(id)
                .orElseThrow(() -> new TicketNotFoundException(id));

        if (!access.canAct(user, t)) throw new TicketAccessDeniedException();

        TicketStatus old = t.getStatus();
        if (old == newStatus) return toResponse(t, true, user);

        if (!isValidTransition(old, newStatus)) {
            throw new InvalidStatusTransitionException(old.name(), newStatus.name());
        }

        t.setStatus(newStatus);
        if (newStatus == TicketStatus.RESOLVED) t.setResolvedAt(Instant.now());
        if (newStatus == TicketStatus.CLOSED) t.setClosedAt(Instant.now());

        Ticket saved = ticketRepo.save(t);
        log.info("Ticket {} status: {} -> {}", saved.getTicketNumber(), old, newStatus);

        eventPublisher.publish(new TicketStatusChangedEvent(
                saved.getId(), saved.getTicketNumber(),
                old.name(), newStatus.name(), user.getId(), saved.getCreatedBy()));

        notifyStatusChange(saved, old, newStatus, user.getId());

        return toResponse(saved, true, user);
    }

    private void notifyStatusChange(Ticket t, TicketStatus oldS, TicketStatus newS, Long actorId) {
        List<Long> recipients = new ArrayList<>();
        recipients.add(t.getCreatedBy());
        if (t.getAssignedTo() != null) recipients.add(t.getAssignedTo());

        notificationFacade.notifyUsers(
                recipients,
                actorId,
                NotificationType.TICKET_STATUS_CHANGED,
                String.format("%s status changed", t.getTicketNumber()),
                oldS + " → " + newS,
                t.getId(),
                "/tickets/" + t.getId()
        );
    }

    private Instant computeSlaDeadline(TicketPriority priority) {
        int hours = switch (priority) {
            case URGENT -> config.getInt("TICKET.SLA.URGENT.HOURS", 2);
            case HIGH -> config.getInt("TICKET.SLA.HIGH.HOURS", 8);
            case MEDIUM -> config.getInt("TICKET.SLA.MEDIUM.HOURS", 24);
            case LOW -> config.getInt("TICKET.SLA.LOW.HOURS", 72);
        };
        return Instant.now().plus(hours, java.time.temporal.ChronoUnit.HOURS);
    }

    private boolean isValidTransition(TicketStatus from, TicketStatus to) {
        return switch (from) {
            case OPEN -> to == TicketStatus.IN_PROGRESS || to == TicketStatus.CLOSED;
            case IN_PROGRESS -> to == TicketStatus.WAITING || to == TicketStatus.RESOLVED || to == TicketStatus.CLOSED;
            case WAITING -> to == TicketStatus.IN_PROGRESS || to == TicketStatus.RESOLVED || to == TicketStatus.CLOSED;
            case RESOLVED -> to == TicketStatus.CLOSED || to == TicketStatus.OPEN;
            case CLOSED -> false;
        };
    }

    private TicketResponse toResponse(Ticket t, boolean withComments, UserPrincipal viewer) {
        TicketCategory cat = categoryRepo.findById(t.getCategoryId()).orElse(null);
        TicketGroup grp = null;

        String viewerRole = "USER";
        if (viewer != null) {
            if (viewer.isAdmin()) viewerRole = "ADMIN";
            else if (access.canAct(viewer, t)) viewerRole = "AGENT";
        }

        return mapper.toResponse(t, cat, grp, java.util.List.of(), viewerRole);
    }
}
