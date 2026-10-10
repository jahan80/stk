package com.starterkit.ticket.ticket.application.service;

import com.starterkit.ticket.shared.infrastructure.client.UserClient;
import com.starterkit.ticket.ticket.domain.entity.NotificationType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * High-level API for the ticket domain to send notifications.
 *
 * Responsibilities:
 *   - resolve channel list from TICKET.NOTIF.*.* config flags
 *   - fetch admin user IDs when needed
 *   - delegate to NotificationDispatcher (which goes through outbox)
 *
 * The actual delivery (in-app, email, sms) happens in notif-service.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TicketNotificationFacade {

    private final NotificationDispatcher dispatcher;
    private final TicketConfigurationService config;
    private final UserClient userClient;

    /**
     * Notify a fixed list of users.
     */
    public void notifyUsers(Collection<Long> userIds,
                            Long actorId,
                            NotificationType type,
                            String title,
                            String message,
                            Long ticketId,
                            String link) {

        List<String> channels = resolveChannels();
        if (channels.isEmpty()) {
            log.debug("No notification channels enabled for type={}", type);
            return;
        }

        dispatcher.dispatch(
                userIds,
                actorId,
                type.name(),
                title,
                message,
                channels,
                "TICKET",
                ticketId,
                link
        );
    }

    /**
     * Notify all current ADMIN users.
     * Used on ticket create when TICKET.NOTIFY.ADMIN.ON_CREATE=true.
     */
    public void notifyAdmins(Long actorId,
                             NotificationType type,
                             String title,
                             String message,
                             Long ticketId,
                             String link) {

        List<Long> adminIds = userClient.getAdminIds();
        if (adminIds.isEmpty()) {
            log.debug("No admins to notify for type={}", type);
            return;
        }
        notifyUsers(adminIds, actorId, type, title, message, ticketId, link);
    }

    /**
     * Resolve enabled channels from config.
     *   IN_APP : always enabled (TICKET.NOTIF.IN_APP.ENABLED default true)
     *   EMAIL  : TICKET.NOTIF.EMAIL.ENABLED  (default false)
     *   SMS    : TICKET.NOTIF.SMS.ENABLED    (default false)
     */
    private List<String> resolveChannels() {
        List<String> channels = new ArrayList<>();

        if (config.getBoolean("TICKET.NOTIF.IN_APP.ENABLED", true)) {
            channels.add("IN_APP");
        }
        if (config.getBoolean("TICKET.NOTIF.EMAIL.ENABLED", false)) {
            channels.add("EMAIL");
        }
        if (config.getBoolean("TICKET.NOTIF.SMS.ENABLED", false)) {
            channels.add("SMS");
        }

        return channels;
    }
}
