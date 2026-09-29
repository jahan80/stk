package com.starterkit.ticket.ticket.domain.repository;

import com.starterkit.ticket.ticket.domain.entity.TicketGroupMember;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TicketGroupMemberRepository extends JpaRepository<TicketGroupMember, Long> {
    List<TicketGroupMember> findByGroupIdOrderByIdAsc(Long groupId);
    List<TicketGroupMember> findByUserId(Long userId);
    Optional<TicketGroupMember> findByGroupIdAndUserId(Long groupId, Long userId);
    boolean existsByGroupIdAndUserId(Long groupId, Long userId);
    long countByGroupId(Long groupId);
    void deleteByGroupIdAndUserId(Long groupId, Long userId);
}
