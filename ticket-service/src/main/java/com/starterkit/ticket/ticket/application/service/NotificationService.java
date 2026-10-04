package com.starterkit.ticket.ticket.application.service;

import com.starterkit.ticket.ticket.api.dto.NotificationResponse;
import com.starterkit.ticket.ticket.domain.entity.NotificationType;
import com.starterkit.ticket.ticket.domain.entity.TicketNotification;
import com.starterkit.ticket.ticket.domain.repository.TicketNotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NotificationService {

    private final TicketNotificationRepository repo;
    private final ExternalNotificationDispatcher externalDispatcher;

    // ========== CREATE ==========

    /**
     * Create in-app notification + dispatch external channels.
     *
     * If called standalone (not from createForMany), external dispatch
     * happens once for this user.
     */
    @Transactional
    public void create(Long userId, NotificationType type, String title, String message,
                       Long ticketId, Long actorId, String link) {
        // Don't notify the actor about their own action
        if (actorId != null && actorId.equals(userId)) return;

        // 1) In-app
        saveInApp(userId, type, title, message, ticketId, actorId, link);

        // 2) External (single user)
        // Failing to enqueue the outbox row must roll back the business
        // transaction — otherwise we'd have a ticket with no event.
        externalDispatcher.dispatch(
                java.util.List.of(userId), type, title, message, ticketId, actorId);
    }

    /**
     * Internal helper: save in-app row only (no external dispatch).
     */
    private void saveInApp(Long userId, NotificationType type, String title,
                            String message, Long ticketId, Long actorId, String link) {
        TicketNotification n = new TicketNotification();
        n.setUserId(userId);
        n.setType(type);
        n.setTitle(title);
        n.setMessage(message);
        n.setTicketId(ticketId);
        n.setActorId(actorId);
        n.setLink(link);
        n.setRead(false);

        repo.save(n);
        log.debug("In-app notification created: userId={}, type={}, ticketId={}",
                userId, type, ticketId);
    }

    @Transactional
    public void createForMany(Collection<Long> userIds, NotificationType type,
                               String title, String message,
                               Long ticketId, Long actorId, String link) {
        if (userIds == null || userIds.isEmpty()) return;

        // dedup + remove actor
        Set<Long> unique = new LinkedHashSet<>(userIds);
        unique.remove(actorId);
        if (unique.isEmpty()) return;

        // 1) In-app for each user — no external dispatch here.
        for (Long userId : unique) {
            saveInApp(userId, type, title, message, ticketId, actorId, link);
        }

        // 2) External channels (email/sms) — dispatched ONCE for all users.
        //    Failing to enqueue the outbox row must roll back the business TX.
        externalDispatcher.dispatch(unique, type, title, message, ticketId, actorId);
    }

    // ========== READ ==========

    public Page<NotificationResponse> list(Long userId, Boolean unreadOnly, Pageable pageable) {
        Page<TicketNotification> page;
        if (Boolean.TRUE.equals(unreadOnly)) {
            page = repo.findByUserIdAndReadOrderByCreatedAtDesc(userId, false, pageable);
        } else {
            page = repo.findByUserIdOrderByCreatedAtDesc(userId, pageable);
        }
        return page.map(this::toResponse);
    }

    public long getUnreadCount(Long userId) {
        return repo.countByUserIdAndReadFalse(userId);
    }

    // ========== MARK ==========

    @Transactional
    public void markAsRead(Long userId, Long notifId) {
        repo.findById(notifId).ifPresent(n -> {
            if (n.getUserId().equals(userId) && !n.isRead()) {
                n.setRead(true);
                n.setReadAt(Instant.now());
                repo.save(n);
            }
        });
    }

    @Transactional
    public int markAllAsRead(Long userId) {
        return repo.markAllAsRead(userId, Instant.now());
    }

    private NotificationResponse toResponse(TicketNotification n) {
        return NotificationResponse.builder()
                .id(n.getId())
                .type(n.getType().name())
                .title(n.getTitle())
                .message(n.getMessage())
                .ticketId(n.getTicketId())
                .actorId(n.getActorId())
                .read(n.isRead())
                .readAt(n.getReadAt())
                .link(n.getLink())
                .createdAt(n.getCreatedAt())
                .build();
    }
}
