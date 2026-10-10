package com.starterkit.ticket.ticket.application.service.ticket;

import com.starterkit.ticket.shared.infrastructure.jwt.UserPrincipal;
import com.starterkit.ticket.ticket.api.dto.*;
import com.starterkit.ticket.ticket.application.event.*;
import com.starterkit.ticket.ticket.application.exception.*;
import com.starterkit.ticket.ticket.application.mapper.TicketMapper;
import com.starterkit.ticket.ticket.application.service.TicketAccessService;
import com.starterkit.ticket.ticket.application.service.TicketNotificationFacade;
import com.starterkit.ticket.ticket.domain.entity.*;
import com.starterkit.ticket.ticket.domain.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TicketAssignmentService {

    private final TicketRepository ticketRepo;
    private final TicketCategoryRepository categoryRepo;
    private final TicketGroupRepository groupRepo;
    private final TicketGroupMemberRepository groupMemberRepo;
    private final TicketAccessService access;
    private final TicketMapper mapper;
    private final TicketEventPublisher eventPublisher;
    private final TicketNotificationFacade notificationFacade;

    @Transactional
    public TicketResponse assign(UserPrincipal user, Long id, Long assigneeId) {
        Ticket t = ticketRepo.findById(id)
                .orElseThrow(() -> new TicketNotFoundException(id));

        if (!access.canAct(user, t)) throw new TicketAccessDeniedException();

        if (!user.isAdmin()) {
            boolean targetIsGroupMember = t.getGroupId() != null
                    && access.isMemberOf(t.getGroupId(), assigneeId);
            if (!assigneeId.equals(user.getId()) && !targetIsGroupMember) {
                throw new TicketAccessDeniedException();
            }
        }

        t.setAssignedTo(assigneeId);

        var memberships = groupMemberRepo.findByUserId(assigneeId);
        if (!memberships.isEmpty()) {
            t.setGroupId(memberships.get(0).getGroupId());
            log.info("Ticket {} auto-assigned to group {} from assignee {}",
                    t.getTicketNumber(), memberships.get(0).getGroupId(), assigneeId);
        }

        Ticket saved = ticketRepo.save(t);
        log.info("Ticket {} assigned to user {}", saved.getTicketNumber(), assigneeId);

        eventPublisher.publish(new TicketAssignedEvent(
                saved.getId(), saved.getTicketNumber(), assigneeId, user.getId()));

        notificationFacade.notifyUsers(
                List.of(assigneeId),
                user.getId(),
                NotificationType.TICKET_ASSIGNED,
                String.format("Assigned: %s", saved.getTicketNumber()),
                saved.getTitle(),
                saved.getId(),
                "/tickets/" + saved.getId()
        );

        return toResponse(saved, user);
    }

    @Transactional
    public TicketResponse assignToGroup(UserPrincipal user, Long id, Long groupId) {
        if (!user.isAdmin()) throw new TicketAccessDeniedException();

        Ticket t = ticketRepo.findById(id)
                .orElseThrow(() -> new TicketNotFoundException(id));

        if (groupId != null && !groupRepo.existsById(groupId)) {
            throw new GroupNotFoundException(groupId);
        }

        t.setGroupId(groupId);
        Ticket saved = ticketRepo.save(t);
        log.info("Ticket {} assigned to group {}", saved.getTicketNumber(), groupId);
        return toResponse(saved, user);
    }

    private TicketResponse toResponse(Ticket t, UserPrincipal viewer) {
        TicketCategory cat = categoryRepo.findById(t.getCategoryId()).orElse(null);
        TicketGroup grp = t.getGroupId() != null
                ? groupRepo.findById(t.getGroupId()).orElse(null) : null;

        String viewerRole = "USER";
        if (viewer != null) {
            if (viewer.isAdmin()) viewerRole = "ADMIN";
            else if (access.canAct(viewer, t)) viewerRole = "AGENT";
        }

        return mapper.toResponse(t, cat, grp, List.of(), viewerRole);
    }
}
