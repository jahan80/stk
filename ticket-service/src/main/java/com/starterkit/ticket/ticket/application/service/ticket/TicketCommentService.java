package com.starterkit.ticket.ticket.application.service.ticket;

import com.starterkit.ticket.shared.infrastructure.client.UserClient;
import com.starterkit.ticket.shared.infrastructure.jwt.UserPrincipal;
import com.starterkit.ticket.ticket.api.dto.CommentResponse;
import com.starterkit.ticket.ticket.application.event.TicketCommentedEvent;
import com.starterkit.ticket.ticket.application.event.TicketEventPublisher;
import com.starterkit.ticket.ticket.application.exception.*;
import com.starterkit.ticket.ticket.application.mapper.TicketMapper;
import com.starterkit.ticket.ticket.application.service.MentionService;
import com.starterkit.ticket.ticket.application.service.NotificationService;
import com.starterkit.ticket.ticket.application.service.TicketAccessService;
import com.starterkit.ticket.ticket.domain.entity.*;
import com.starterkit.ticket.ticket.domain.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Comment + mention side of the ticket domain.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TicketCommentService {

    private final TicketRepository ticketRepo;
    private final TicketCommentRepository commentRepo;
    private final TicketAccessService access;
    private final TicketMapper mapper;
    private final TicketEventPublisher eventPublisher;
    private final NotificationService notificationService;
    private final MentionService mentionService;
    private final UserClient userClient;

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

        Set<String> mentioned = mentionService.extractUsernames(body);
        if (!mentioned.isEmpty()) {
            Map<String, Long> userIds = userClient.getUsersByUsernames(mentioned);
            if (!userIds.isEmpty()) {
                List<Long> recipientIds = new ArrayList<>(userIds.values());

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
}
