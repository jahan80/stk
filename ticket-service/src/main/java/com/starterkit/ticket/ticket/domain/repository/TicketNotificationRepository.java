package com.starterkit.ticket.ticket.domain.repository;

import com.starterkit.ticket.ticket.domain.entity.TicketNotification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;

public interface TicketNotificationRepository extends JpaRepository<TicketNotification, Long> {

    Page<TicketNotification> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    Page<TicketNotification> findByUserIdAndReadOrderByCreatedAtDesc(Long userId, boolean read, Pageable pageable);

    long countByUserIdAndReadFalse(Long userId);

    @Modifying
    @Query("UPDATE TicketNotification n SET n.read = true, n.readAt = :now " +
           "WHERE n.userId = :userId AND n.read = false")
    int markAllAsRead(@Param("userId") Long userId, @Param("now") Instant now);
}
