package com.starterkit.ticket.ticket.domain.repository;

import com.starterkit.ticket.ticket.domain.entity.TicketComment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TicketCommentRepository extends JpaRepository<TicketComment, Long> {
    List<TicketComment> findAllByTicketIdOrderByCreatedAtAsc(Long ticketId);
    long countByTicketId(Long ticketId);
}
