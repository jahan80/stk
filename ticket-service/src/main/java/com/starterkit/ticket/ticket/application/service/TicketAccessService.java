package com.starterkit.ticket.ticket.application.service;

import com.starterkit.ticket.ticket.domain.entity.Ticket;
import com.starterkit.ticket.ticket.domain.repository.TicketGroupMemberRepository;
import com.starterkit.ticket.shared.infrastructure.jwt.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Central access-control logic for tickets.
 *
 * Rules:
 *  - Admin: sees everything
 *  - Owner (created_by): sees own ticket
 *  - Assignee: sees assigned ticket
 *  - Group member: sees tickets of their groups
 *  - Otherwise: denied
 */
@Service
@RequiredArgsConstructor
public class TicketAccessService {

    private final TicketGroupMemberRepository groupMemberRepo;

    /** Can this user see the ticket? */
    public boolean canView(UserPrincipal user, Ticket ticket) {
        if (user.isAdmin()) return true;
        if (ticket.isOwnedBy(user.getId())) return true;
        if (ticket.isAssignedTo(user.getId())) return true;
        if (ticket.getGroupId() != null &&
                groupMemberRepo.existsByGroupIdAndUserId(ticket.getGroupId(), user.getId())) {
            return true;
        }
        return false;
    }

    /** Can this user perform agent actions (assign, status, etc.)? */
    public boolean canAct(UserPrincipal user, Ticket ticket) {
        if (user.isAdmin()) return true;
        if (ticket.getGroupId() != null &&
                groupMemberRepo.existsByGroupIdAndUserId(ticket.getGroupId(), user.getId())) {
            return true;
        }
        return false;
    }

    /** Is this user a member of the given group? */
    public boolean isMemberOf(Long groupId, Long userId) {
        return groupMemberRepo.existsByGroupIdAndUserId(groupId, userId);
    }

    /** Get all group ids this user belongs to */
    public List<Long> getUserGroupIds(Long userId) {
        return groupMemberRepo.findByUserId(userId).stream()
                .map(m -> m.getGroupId()).toList();
    }
}
