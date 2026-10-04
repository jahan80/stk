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

    @Transactional
    public void create(Long userId, NotificationType type, String title, String message,
                       Long ticketId, Long actorId, String link) {
        // Don't notify the actor about their own action
        if (actorId != null && actorId.equals(userId)) return;

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
        log.debug("Notification created: userId={}, type={}, ticketId={}", userId, type, ticketId);

        // External channels (configurable) - email/sms via outbox.
        // Best-effort: failures do NOT roll back the business TX.
        try {
            externalDispatcher.dispatch(
                    java.util.List.of(userId), type, title, message, ticketId, actorId);
        } catch (Exception ex) {
            log.error("External dispatch failed for userId={}, type={}", userId, type, ex);
        }
    }

    @Transactional
    public void createForMany(Collection<Long> userIds, NotificationType type,
                               String title, String message,
                               Long ticketId, Long actorId, String link) {
        if (userIds == null || userIds.isEmpty()) return;

        // dedup + remove actor
        Set<Long> unique = new LinkedHashSet<>(userIds);
        unique.remove(actorId);

        // 1) In-app notifications (always)
        for (Long userId : unique) {
            create(userId, type, title, message, ticketId, actorId, link);
        }

        // 2) External channels (configurable) - email/sms via outbox.
        //    Best-effort: failures here do NOT roll back the business TX.
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
