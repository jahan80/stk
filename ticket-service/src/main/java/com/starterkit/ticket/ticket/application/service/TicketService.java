package com.starterkit.ticket.ticket.application.service;

import com.starterkit.ticket.shared.infrastructure.jwt.UserPrincipal;
import com.starterkit.ticket.ticket.api.dto.*;
import com.starterkit.ticket.ticket.application.exception.*;
import com.starterkit.ticket.ticket.application.mapper.TicketMapper;
import com.starterkit.ticket.ticket.domain.entity.*;
import com.starterkit.ticket.ticket.domain.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TicketService {

    private final TicketRepository ticketRepo;
    private final TicketCommentRepository commentRepo;
    private final TicketCategoryRepository categoryRepo;
    private final TicketGroupRepository groupRepo;
    private final TicketNumberGenerator numberGen;
    private final TicketAccessService access;
    private final TicketMapper mapper;

    // =====================================================
    // CREATE
    // =====================================================

    @Transactional
    public TicketResponse create(UserPrincipal user, CreateTicketRequest req) {
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

        Ticket saved = ticketRepo.save(t);
        log.info("Ticket created: {} by user={}", saved.getTicketNumber(), user.getId());

        return toResponse(saved, true);
    }

    // =====================================================
    // LIST (access-aware)
    // =====================================================

    public Page<TicketSummaryResponse> list(UserPrincipal user,
                                             TicketStatus status,
                                             TicketPriority priority,
                                             Long groupId,
                                             boolean unassignedOnly,
                                             Pageable pageable) {
        Page<Ticket> page;

        if (user.isAdmin()) {
            // Admin: all tickets with optional filters
            if (unassignedOnly) {
                page = ticketRepo.findByAssignedToIsNullOrderByCreatedAtDesc(pageable);
            } else if (groupId != null) {
                page = ticketRepo.findByGroupIdOrderByCreatedAtDesc(groupId, pageable);
            } else if (status != null) {
                page = ticketRepo.findByStatusOrderByCreatedAtDesc(status, pageable);
            } else if (priority != null) {
                page = ticketRepo.findByPriorityOrderByCreatedAtDesc(priority, pageable);
            } else {
                page = ticketRepo.findAllByOrderByCreatedAtDesc(pageable);
            }
        } else {
            // Non-admin: own + assigned + group tickets
            List<Long> groupIds = access.getUserGroupIds(user.getId());
            page = ticketRepo.findVisibleToUser(user.getId(), groupIds, pageable);

            // Apply client-side filters (simpler than many queries)
            // Note: for scale, we'd push these into the query
        }

        // Fetch related entities for mapping
        List<Long> catIds = page.getContent().stream().map(Ticket::getCategoryId).distinct().toList();
        List<Long> grpIds = page.getContent().stream()
                .filter(t -> t.getGroupId() != null).map(Ticket::getGroupId).distinct().toList();

        var cats = categoryRepo.findAllById(catIds).stream()
                .collect(java.util.stream.Collectors.toMap(TicketCategory::getId, c -> c));
        var grps = groupRepo.findAllById(grpIds).stream()
                .collect(java.util.stream.Collectors.toMap(TicketGroup::getId, g -> g));

        return page.map(t -> mapper.toSummary(
                t,
                cats.get(t.getCategoryId()),
                t.getGroupId() != null ? grps.get(t.getGroupId()) : null
        ));
    }

    // =====================================================
    // DETAIL
    // =====================================================

    public TicketResponse get(UserPrincipal user, Long id) {
        Ticket t = ticketRepo.findById(id)
                .orElseThrow(() -> new TicketNotFoundException(id));

        if (!access.canView(user, t)) {
            throw new TicketAccessDeniedException();
        }

        return toResponse(t, true);
    }

    // =====================================================
    // CLOSE (owner)
    // =====================================================

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

        t.setStatus(TicketStatus.CLOSED);
        t.setClosedAt(Instant.now());
        Ticket saved = ticketRepo.save(t);
        return toResponse(saved, true);
    }

    // =====================================================
    // CHANGE STATUS (agent/admin)
    // =====================================================

    @Transactional
    public TicketResponse changeStatus(UserPrincipal user, Long id, TicketStatus newStatus) {
        Ticket t = ticketRepo.findById(id)
                .orElseThrow(() -> new TicketNotFoundException(id));

        if (!access.canAct(user, t)) {
            throw new TicketAccessDeniedException();
        }

        TicketStatus old = t.getStatus();
        if (old == newStatus) return toResponse(t, true);

        if (!isValidTransition(old, newStatus)) {
            throw new InvalidStatusTransitionException(old.name(), newStatus.name());
        }

        t.setStatus(newStatus);
        if (newStatus == TicketStatus.RESOLVED) t.setResolvedAt(Instant.now());
        if (newStatus == TicketStatus.CLOSED) t.setClosedAt(Instant.now());

        Ticket saved = ticketRepo.save(t);
        log.info("Ticket {} status: {} -> {}", saved.getTicketNumber(), old, newStatus);
        return toResponse(saved, true);
    }

    // =====================================================
    // ASSIGN (agent/admin → to self or group member)
    // =====================================================

    @Transactional
    public TicketResponse assign(UserPrincipal user, Long id, Long assigneeId) {
        Ticket t = ticketRepo.findById(id)
                .orElseThrow(() -> new TicketNotFoundException(id));

        if (!access.canAct(user, t)) {
            throw new TicketAccessDeniedException();
        }

        // Non-admin can only assign to self or group members
        if (!user.isAdmin()) {
            boolean targetIsGroupMember = t.getGroupId() != null
                    && access.isMemberOf(t.getGroupId(), assigneeId);
            if (!assigneeId.equals(user.getId()) && !targetIsGroupMember) {
                throw new TicketAccessDeniedException();
            }
        }

        t.setAssignedTo(assigneeId);
        Ticket saved = ticketRepo.save(t);
        log.info("Ticket {} assigned to user {}", saved.getTicketNumber(), assigneeId);
        return toResponse(saved, true);
    }

    // =====================================================
    // ASSIGN TO GROUP (admin only)
    // =====================================================

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
        return toResponse(saved, true);
    }

    // =====================================================
    // COMMENTS
    // =====================================================

    @Transactional
    public CommentResponse addComment(UserPrincipal user, Long ticketId, String body) {
        Ticket t = ticketRepo.findById(ticketId)
                .orElseThrow(() -> new TicketNotFoundException(ticketId));

        if (!access.canView(user, t)) {
            throw new TicketAccessDeniedException();
        }

        AuthorRole role;
        if (user.isAdmin()) role = AuthorRole.ADMIN;
        else if (access.canAct(user, t)) role = AuthorRole.AGENT;
        else role = AuthorRole.USER;

        TicketComment c = new TicketComment();
        c.setTicketId(ticketId);
        c.setAuthorId(user.getId());
        c.setAuthorRole(role);
        c.setBody(body);

        TicketComment saved = commentRepo.save(c);
        return mapper.toComment(saved);
    }

    public List<CommentResponse> listComments(UserPrincipal user, Long ticketId) {
        Ticket t = ticketRepo.findById(ticketId)
                .orElseThrow(() -> new TicketNotFoundException(ticketId));
        if (!access.canView(user, t)) throw new TicketAccessDeniedException();
        return commentRepo.findAllByTicketIdOrderByCreatedAtAsc(ticketId)
                .stream().map(mapper::toComment).toList();
    }

    // =====================================================
    // HELPERS
    // =====================================================

    private boolean isValidTransition(TicketStatus from, TicketStatus to) {
        return switch (from) {
            case OPEN -> to == TicketStatus.IN_PROGRESS || to == TicketStatus.CLOSED;
            case IN_PROGRESS -> to == TicketStatus.WAITING
                    || to == TicketStatus.RESOLVED
                    || to == TicketStatus.CLOSED;
            case WAITING -> to == TicketStatus.IN_PROGRESS
                    || to == TicketStatus.RESOLVED
                    || to == TicketStatus.CLOSED;
            case RESOLVED -> to == TicketStatus.CLOSED || to == TicketStatus.OPEN;
            case CLOSED -> false;
        };
    }

    private TicketResponse toResponse(Ticket t, boolean withComments) {
        TicketCategory cat = categoryRepo.findById(t.getCategoryId()).orElse(null);
        TicketGroup grp = t.getGroupId() != null
                ? groupRepo.findById(t.getGroupId()).orElse(null) : null;
        List<TicketComment> comments = withComments
                ? commentRepo.findAllByTicketIdOrderByCreatedAtAsc(t.getId())
                : List.of();
        return mapper.toResponse(t, cat, grp, comments);
    }
}
