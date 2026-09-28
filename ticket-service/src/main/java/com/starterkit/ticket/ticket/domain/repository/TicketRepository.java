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
        WHERE t.createdBy = :userId
           OR t.assignedTo = :userId
           OR t.groupId IN :groupIds
        ORDER BY t.createdAt DESC
    """)
    Page<Ticket> findVisibleToUser(
            @Param("userId") Long userId,
            @Param("groupIds") List<Long> groupIds,
            Pageable pageable);

    /** Only own tickets (created by user) */
    Page<Ticket> findByCreatedByOrderByCreatedAtDesc(Long createdBy, Pageable pageable);

    /** Assigned to user */
    Page<Ticket> findByAssignedToOrderByCreatedAtDesc(Long assignedTo, Pageable pageable);

    /** Filtered queries (admin) */
    Page<Ticket> findAllByOrderByCreatedAtDesc(Pageable pageable);
    Page<Ticket> findByStatusOrderByCreatedAtDesc(TicketStatus status, Pageable pageable);
    Page<Ticket> findByPriorityOrderByCreatedAtDesc(TicketPriority priority, Pageable pageable);
    Page<Ticket> findByGroupIdOrderByCreatedAtDesc(Long groupId, Pageable pageable);
    Page<Ticket> findByAssignedToIsNullOrderByCreatedAtDesc(Pageable pageable);

    // ====== Counters ======

    long countByGroupId(Long groupId);
}
