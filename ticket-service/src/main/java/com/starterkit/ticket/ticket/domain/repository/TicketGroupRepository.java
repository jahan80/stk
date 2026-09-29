package com.starterkit.ticket.ticket.domain.repository;

import com.starterkit.ticket.ticket.domain.entity.TicketGroup;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TicketGroupRepository extends JpaRepository<TicketGroup, Long> {
    List<TicketGroup> findAllByOrderByNameAsc();
    List<TicketGroup> findByEnabledTrueOrderByNameAsc();
    Optional<TicketGroup> findByName(String name);
    boolean existsByName(String name);
}
