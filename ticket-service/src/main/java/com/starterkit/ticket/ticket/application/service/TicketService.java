package com.starterkit.ticket.ticket.application.service;

import com.starterkit.ticket.shared.infrastructure.client.UserClient;
import com.starterkit.ticket.shared.infrastructure.jwt.UserPrincipal;
import com.starterkit.ticket.ticket.api.dto.*;
import com.starterkit.ticket.ticket.application.event.*;
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
import java.util.ArrayList;
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
    private final TicketConfigurationService config;
    private final TicketEventPublisher eventPublisher;
    private final NotificationService notificationService;
    private final MentionService mentionService;
    private final UserClient userClient;
    private final TicketGroupMemberRepository groupMemberRepo;

    // =====================================================
    // CREATE
    // =====================================================

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

        // Publish event
        eventPublisher.publish(new TicketCreatedEvent(
                saved.getId(),
                saved.getTicketNumber(),
                saved.getTitle(),
                user.getId(),
                user.getEmail(),
                saved.getPriority().name(),
                category.getName()
        ));

        // Notify admins if config enabled.
        // Admin user IDs are resolved from auth-service (best-effort:
        // empty list on failure — the ticket still gets created).
        boolean notifyAdmins = config.getBoolean("TICKET.NOTIFY.ADMIN.ON_CREATE", true);
        if (notifyAdmins) {
            java.util.List<Long> adminIds = userClient.getAdminIds();
            if (!adminIds.isEmpty()) {
                notificationService.createForMany(
                        adminIds,
                        NotificationType.TICKET_CREATED,
                        String.format("New ticket: %s", saved.getTicketNumber()),
                        saved.getTitle(),
                        saved.getId(),
                        user.getId(),
                        "/tickets/" + saved.getId()
                );
                log.info("Admin notifications sent for ticket {} to {} admin(s)",
                        saved.getTicketNumber(), adminIds.size());
            } else {
                log.debug("No admins to notify for ticket {}", saved.getTicketNumber());
            }
        }

        return toResponse(saved, true);
    }

    // =====================================================
    // LIST
    // =====================================================

    public Page<TicketSummaryResponse> list(UserPrincipal user,
                                             TicketStatus status,
                                             TicketPriority priority,
                                             Long groupId,
                                             boolean unassignedOnly,
                                             boolean assignedOnly,
                                             Pageable pageable) {
        Page<Ticket> page;

        if (user.isAdmin()) {
            // Combined filters: all applied with AND semantics.
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

        // Determine viewer role for each ticket
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

    // =====================================================
    // DETAIL
    // =====================================================

    public TicketResponse get(UserPrincipal user, Long id) {
        Ticket t = ticketRepo.findById(id)
                .orElseThrow(() -> new TicketNotFoundException(id));
        if (!access.canView(user, t)) throw new TicketAccessDeniedException();
        return toResponse(t, true, user);
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

        TicketStatus old = t.getStatus();
        t.setStatus(TicketStatus.CLOSED);
        t.setClosedAt(Instant.now());
        Ticket saved = ticketRepo.save(t);

        eventPublisher.publish(new TicketStatusChangedEvent(
                saved.getId(), saved.getTicketNumber(),
                old.name(), "CLOSED", user.getId(), saved.getCreatedBy()));

        notifyStatusChange(saved, old, TicketStatus.CLOSED, user.getId());

        return toResponse(saved, true);
    }

    // =====================================================
    // CHANGE STATUS (agent/admin)
    // =====================================================

    @Transactional
    public TicketResponse changeStatus(UserPrincipal user, Long id, TicketStatus newStatus) {
        Ticket t = ticketRepo.findById(id)
                .orElseThrow(() -> new TicketNotFoundException(id));

        if (!access.canAct(user, t)) throw new TicketAccessDeniedException();

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

        eventPublisher.publish(new TicketStatusChangedEvent(
                saved.getId(), saved.getTicketNumber(),
                old.name(), newStatus.name(), user.getId(), saved.getCreatedBy()));

        notifyStatusChange(saved, old, newStatus, user.getId());

        return toResponse(saved, true);
    }

    private void notifyStatusChange(Ticket t, TicketStatus oldS, TicketStatus newS, Long actorId) {
        List<Long> recipients = new ArrayList<>();
        recipients.add(t.getCreatedBy());
        if (t.getAssignedTo() != null) recipients.add(t.getAssignedTo());

        notificationService.createForMany(
                recipients,
                NotificationType.TICKET_STATUS_CHANGED,
                String.format("%s status changed", t.getTicketNumber()),
                oldS + " → " + newS,
                t.getId(),
                actorId,
                "/tickets/" + t.getId()
        );
    }

    // =====================================================
    // ASSIGN (agent/admin)
    // =====================================================

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

        // Auto-set group from assignee's first group (if any)
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

        // Notify assignee (in-app)
        notificationService.create(
                assigneeId,
                NotificationType.TICKET_ASSIGNED,
                String.format("Assigned: %s", saved.getTicketNumber()),
                saved.getTitle(),
                saved.getId(),
                user.getId(),
                "/tickets/" + saved.getId()
        );

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
        log.info("Ticket {} assigned to group {}", saved.getTicketNumber(), groupId);
        return toResponse(saved, true);
    }

    // =====================================================
    // COMMENTS
    // =====================================================

    @Transactional
    public CommentResponse addComment(UserPrincipal user, Long ticketId, String body) {
        Ticket t = ticketRepo.findById(ticketId)
                .orElseThrow(() -> new TicketNotFoundException(ticketId));

        if (!access.canView(user, t)) throw new TicketAccessDeniedException();

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

        eventPublisher.publish(new TicketCommentedEvent(
                t.getId(), t.getTicketNumber(), saved.getId(),
                user.getId(), role.name(), t.getCreatedBy(), t.getAssignedTo()));

        // Notify owner + assignee
        List<Long> recipients = new ArrayList<>();
        recipients.add(t.getCreatedBy());
        if (t.getAssignedTo() != null) recipients.add(t.getAssignedTo());

        notificationService.createForMany(
                recipients,
                NotificationType.TICKET_COMMENTED,
                String.format("New comment on %s", t.getTicketNumber()),
                body.length() > 100 ? body.substring(0, 100) + "..." : body,
                t.getId(),
                user.getId(),
                "/tickets/" + t.getId()
        );

        // Mentions — resolve usernames to user IDs and notify.
        java.util.Set<String> mentioned = mentionService.extractUsernames(body);
        if (!mentioned.isEmpty()) {
            java.util.Map<String, Long> userIds = userClient.getUsersByUsernames(mentioned);
            if (!userIds.isEmpty()) {
                java.util.List<Long> recipientIds = new java.util.ArrayList<>(userIds.values());

                notificationService.createForMany(
                        recipientIds,
                        NotificationType.TICKET_MENTIONED,
                        String.format("You were mentioned on %s", t.getTicketNumber()),
                        body.length() > 200 ? body.substring(0, 200) + "..." : body,
                        t.getId(),
                        user.getId(),
                        "/tickets/" + t.getId()
                );
                log.info("Mention notifications sent for ticket {} to {} user(s)",
                        t.getTicketNumber(), recipientIds.size());
            } else {
                log.debug("Mentions found but none resolved: {}", mentioned);
            }
        }

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
    // NOTIFICATION HELPER (best-effort email via notif-service)
    // =====================================================


    // =====================================================
    // HELPERS
    // =====================================================

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

    private TicketResponse toResponse(Ticket t, boolean withComments) {
        return toResponse(t, withComments, null);
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
