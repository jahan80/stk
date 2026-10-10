package com.starterkit.ticket.ticket.application.service.ticket;

import com.starterkit.ticket.shared.infrastructure.jwt.UserPrincipal;
import com.starterkit.ticket.ticket.api.dto.*;
import com.starterkit.ticket.ticket.application.exception.*;
import com.starterkit.ticket.ticket.application.mapper.TicketMapper;
import com.starterkit.ticket.ticket.application.service.TicketAccessService;
import com.starterkit.ticket.ticket.domain.entity.*;
import com.starterkit.ticket.ticket.domain.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Query side of the ticket domain: list, get.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TicketQueryService {

    private final TicketRepository ticketRepo;
    private final TicketCommentRepository commentRepo;
    private final TicketCategoryRepository categoryRepo;
    private final TicketGroupRepository groupRepo;
    private final TicketAccessService access;
    private final TicketMapper mapper;

    public Page<TicketSummaryResponse> list(UserPrincipal user,
                                             TicketStatus status,
                                             TicketPriority priority,
                                             Long groupId,
                                             boolean unassignedOnly,
                                             boolean assignedOnly,
                                             Pageable pageable) {

        Page<Ticket> page;

        if (user.isAdmin()) {
            page = ticketRepo.findWithFilters(
                    status, priority, groupId,
                    unassignedOnly, assignedOnly,
                    pageable);
        } else {
            List<Long> groupIds = access.getUserGroupIds(user.getId());
            page = ticketRepo.findVisibleToUser(user.getId(), groupIds, status, priority, pageable);
        }

        List<Long> catIds = page.getContent().stream().map(Ticket::getCategoryId).distinct().toList();
        List<Long> grpIds = page.getContent().stream()
                .filter(t -> t.getGroupId() != null).map(Ticket::getGroupId).distinct().toList();

        var cats = categoryRepo.findAllById(catIds).stream()
                .collect(java.util.stream.Collectors.toMap(TicketCategory::getId, c -> c));
        var grps = groupRepo.findAllById(grpIds).stream()
                .collect(java.util.stream.Collectors.toMap(TicketGroup::getId, g -> g));

        return page.map(t -> {
            String role = "USER";
            if (user.isAdmin()) role = "ADMIN";
            else if (access.canAct(user, t)) role = "AGENT";
            return mapper.toSummary(
                    t,
                    cats.get(t.getCategoryId()),
                    t.getGroupId() != null ? grps.get(t.getGroupId()) : null,
                    role
            );
        });
    }

    public TicketResponse get(UserPrincipal user, Long id) {
        Ticket t = ticketRepo.findById(id)
                .orElseThrow(() -> new TicketNotFoundException(id));
        if (!access.canView(user, t)) throw new TicketAccessDeniedException();
        return toResponse(t, true, user);
    }

    public List<CommentResponse> listComments(UserPrincipal user, Long ticketId) {
        Ticket t = ticketRepo.findById(ticketId)
                .orElseThrow(() -> new TicketNotFoundException(ticketId));
        if (!access.canView(user, t)) throw new TicketAccessDeniedException();
        return commentRepo.findAllByTicketIdOrderByCreatedAtAsc(ticketId)
                .stream().map(mapper::toComment).toList();
    }

    private TicketResponse toResponse(Ticket t, boolean withComments, UserPrincipal viewer) {
        TicketCategory cat = categoryRepo.findById(t.getCategoryId()).orElse(null);
        TicketGroup grp = t.getGroupId() != null
                ? groupRepo.findById(t.getGroupId()).orElse(null) : null;
        List<TicketComment> comments = withComments
                ? commentRepo.findAllByTicketIdOrderByCreatedAtAsc(t.getId())
                : List.of();

        String viewerRole = "USER";
        if (viewer != null) {
            if (viewer.isAdmin()) viewerRole = "ADMIN";
            else if (access.canAct(viewer, t)) viewerRole = "AGENT";
        }

        return mapper.toResponse(t, cat, grp, comments, viewerRole);
    }
}
