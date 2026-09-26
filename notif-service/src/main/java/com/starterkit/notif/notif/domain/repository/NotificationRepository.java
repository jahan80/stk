package com.starterkit.notif.notif.domain.repository;

import com.starterkit.notif.notif.domain.entity.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    Optional<Notification> findByNotificationId(UUID notificationId);

    boolean existsByNotificationId(UUID notificationId);

    Page<Notification> findByChannel(Notification.Channel channel, Pageable pageable);

    Page<Notification> findByStatus(Notification.Status status, Pageable pageable);

    Page<Notification> findByRecipient(String recipient, Pageable pageable);
}
