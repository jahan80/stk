package com.starterkit.ticket.ticket.domain.repository;

import com.starterkit.ticket.ticket.domain.entity.Ticket;
import com.starterkit.ticket.ticket.domain.entity.TicketPriority;
import com.starterkit.ticket.ticket.domain.entity.TicketStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface TicketRepository extends JpaRepository<Ticket, Long> {

    Optional<Ticket> findById(Long id);

    // ====== Access-control queries ======

    /** All tickets visible to a user (own + group memberships) */
    @Query("""
        SELECT DISTINCT t FROM Ticket t
        WHERE (t.createdBy = :userId
               OR t.assignedTo = :userId
               OR t.groupId IN :groupIds)
          AND (:status IS NULL OR t.status = :status)
          AND (:priority IS NULL OR t.priority = :priority)
        ORDER BY t.createdAt DESC
    """)
    Page<Ticket> findVisibleToUser(
            @Param("userId") Long userId,
            @Param("groupIds") List<Long> groupIds,
            @Param("status") TicketStatus status,
            @Param("priority") TicketPriority priority,
            Pageable pageable);

    /**
     * Admin combined-filter search.
     * All filters are optional and AND-combined.
     */
    @Query("""
        SELECT t FROM Ticket t
        WHERE (:status IS NULL OR t.status = :status)
          AND (:priority IS NULL OR t.priority = :priority)
          AND (:groupId IS NULL OR t.groupId = :groupId)
          AND (:unassignedOnly = false OR t.assignedTo IS NULL)
          AND (:assignedOnly = false OR t.assignedTo IS NOT NULL)
        ORDER BY t.createdAt DESC
    """)
    Page<Ticket> findWithFilters(
            @Param("status") TicketStatus status,
            @Param("priority") TicketPriority priority,
            @Param("groupId") Long groupId,
            @Param("unassignedOnly") boolean unassignedOnly,
            @Param("assignedOnly") boolean assignedOnly,
            Pageable pageable);

    /** Only own tickets (created by user) */
    Page<Ticket> findByCreatedByOrderByCreatedAtDesc(Long createdBy, Pageable pageable);

    /** Assigned to user */
    Page<Ticket> findByAssignedToOrderByCreatedAtDesc(Long assignedTo, Pageable pageable);

    // ====== Counters ======

    long countByGroupId(Long groupId);
}
