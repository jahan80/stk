package com.starterkit.ticket.ticket.domain.repository;

import com.starterkit.ticket.ticket.domain.entity.TicketCategory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TicketCategoryRepository extends JpaRepository<TicketCategory, Long> {
    List<TicketCategory> findByEnabledTrueOrderByDisplayOrderAsc();
    List<TicketCategory> findAllByOrderByDisplayOrderAsc();
    Optional<TicketCategory> findByCode(String code);
    boolean existsByCode(String code);
}
