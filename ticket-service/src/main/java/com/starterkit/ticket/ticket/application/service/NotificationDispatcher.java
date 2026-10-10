package com.starterkit.ticket.ticket.application.service;

import com.starterkit.ticket.ticket.application.event.NotifRequestedEvent;
import com.starterkit.ticket.ticket.application.event.TicketEventPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Publishes NOTIFICATION_REQUESTED events to the transactional outbox.
 *
 * Replaces the old NotificationService (in-app DB writes) and
 * ExternalNotificationDispatcher (per-channel outbox events).
 *
 * Design principle:
 *   The ticket domain decides WHO and WHAT to notify.
 *   The notif-service decides HOW to deliver it (which channels,
 *   which providers).
 *
 * Channel policy for the current ticket domain:
 *   - IN_APP  : always on (cheap, user-facing)
 *   - EMAIL   : controlled by TICKET.NOTIF.EMAIL.ENABLED
 *   - SMS     : controlled by TICKET.NOTIF.SMS.ENABLED
 *
 * Callers pass a channel list; this class simply forwards it. The
 * channel-enable flags are resolved by the caller (see
 * TicketNotificationFacade), so this class stays domain-agnostic.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationDispatcher {

    private final TicketEventPublisher eventPublisher;

    /**
     * Publish a notification command for the given recipients.
     *
     * @param recipientUserIds users to notify (actor is filtered out)
     * @param actorId          user who triggered the event (never notified)
     * @param type             logical type (TICKET_CREATED, etc.)
     * @param title            short title (used as email subject prefix)
     * @param message          body text
     * @param channels         list of channel names (IN_APP, EMAIL, SMS)
     * @param referenceType    e.g. "TICKET"
     * @param referenceId      e.g. ticket id
     * @param link             frontend deep link (e.g. /tickets/42)
     */
    public void dispatch(Collection<Long> recipientUserIds,
                          Long actorId,
                          String type,
                          String title,
                          String message,
                          List<String> channels,
                          String referenceType,
                          Long referenceId,
                          String link) {

        if (recipientUserIds == null || recipientUserIds.isEmpty()) return;
        if (channels == null || channels.isEmpty()) {
            log.debug("NotificationDispatcher: no channels for type={}, skipping", type);
            return;
        }

        // Deduplicate + remove actor
        Set<Long> unique = new LinkedHashSet<>(recipientUserIds);
        unique.remove(actorId);
        if (unique.isEmpty()) return;

        // Only publish if at least one channel is requested
        eventPublisher.publish(new NotifRequestedEvent(
                new ArrayList<>(unique),
                actorId,
                type,
                title,
                message,
                new ArrayList<>(channels),
                referenceType,
                referenceId,
                link
        ));

        log.debug("NOTIFICATION_REQUESTED published: type={}, recipients={}, channels={}",
                type, unique.size(), channels);
    }
}
