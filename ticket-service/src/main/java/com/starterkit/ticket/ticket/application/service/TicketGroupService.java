package com.starterkit.ticket.ticket.application.service;

import com.starterkit.ticket.ticket.api.dto.*;
import com.starterkit.ticket.ticket.application.exception.*;
import com.starterkit.ticket.ticket.domain.entity.TicketGroup;
import com.starterkit.ticket.ticket.domain.entity.TicketGroupMember;
import com.starterkit.ticket.ticket.domain.repository.TicketGroupMemberRepository;
import com.starterkit.ticket.ticket.domain.repository.TicketGroupRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TicketGroupService {

    private final TicketGroupRepository groupRepo;
    private final TicketGroupMemberRepository memberRepo;

    public List<GroupResponse> listAll(boolean withMembers) {
        return groupRepo.findAllByOrderByNameAsc().stream()
                .map(g -> toResponse(g, withMembers)).toList();
    }

    public GroupResponse get(Long id, boolean withMembers) {
        TicketGroup g = groupRepo.findById(id)
                .orElseThrow(() -> new GroupNotFoundException(id));
        return toResponse(g, withMembers);
    }

    /** List groups the user is a member of */
    public List<GroupResponse> listMyGroups(Long userId) {
        var ids = memberRepo.findByUserId(userId).stream()
                .map(TicketGroupMember::getGroupId).toList();
        return groupRepo.findAllById(ids).stream()
                .map(g -> toResponse(g, false)).toList();
    }

    @Transactional
    public GroupResponse create(GroupRequest req) {
        if (groupRepo.existsByName(req.getName())) {
            throw new GroupAlreadyExistsException(req.getName());
        }
        TicketGroup g = new TicketGroup();
        g.setName(req.getName());
        g.setDescription(req.getDescription());
        g.setEnabled(req.getEnabled() == null || req.getEnabled());
        return toResponse(groupRepo.save(g), false);
    }

    @Transactional
    public GroupResponse update(Long id, GroupRequest req) {
        TicketGroup g = groupRepo.findById(id)
                .orElseThrow(() -> new GroupNotFoundException(id));

        if (req.getName() != null && !req.getName().equals(g.getName())
                && groupRepo.existsByName(req.getName())) {
            throw new GroupAlreadyExistsException(req.getName());
        }

        if (req.getName() != null) g.setName(req.getName());
        if (req.getDescription() != null) g.setDescription(req.getDescription());
        if (req.getEnabled() != null) g.setEnabled(req.getEnabled());
        return toResponse(groupRepo.save(g), true);
    }

    @Transactional
    public void delete(Long id) {
        TicketGroup g = groupRepo.findById(id)
                .orElseThrow(() -> new GroupNotFoundException(id));
        g.setEnabled(false);
        groupRepo.save(g);
    }

    // ============ MEMBERS ============

    public List<GroupMemberResponse> listMembers(Long groupId) {
        return memberRepo.findByGroupIdOrderByIdAsc(groupId).stream()
                .map(this::toMember).toList();
    }

    @Transactional
    public GroupMemberResponse addMember(Long groupId, GroupMemberRequest req) {
        if (!groupRepo.existsById(groupId)) throw new GroupNotFoundException(groupId);

        if (memberRepo.existsByGroupIdAndUserId(groupId, req.getUserId())) {
            throw new IllegalStateException("User is already a member");
        }

        TicketGroupMember m = new TicketGroupMember();
        m.setGroupId(groupId);
        m.setUserId(req.getUserId());
        try {
            m.setRole(com.starterkit.ticket.ticket.domain.entity.GroupMemberRole.valueOf(
                    req.getRole() != null ? req.getRole().toUpperCase() : "AGENT"));
        } catch (Exception e) {
            m.setRole(com.starterkit.ticket.ticket.domain.entity.GroupMemberRole.AGENT);
        }
        return toMember(memberRepo.save(m));
    }

    @Transactional
    public void removeMember(Long groupId, Long userId) {
        memberRepo.deleteByGroupIdAndUserId(groupId, userId);
    }

    // ============ MAPPERS ============

    private GroupResponse toResponse(TicketGroup g, boolean withMembers) {
        long count = memberRepo.countByGroupId(g.getId());
        List<GroupMemberResponse> members = withMembers
                ? listMembers(g.getId()) : null;
        return GroupResponse.builder()
                .id(g.getId())
                .name(g.getName())
                .description(g.getDescription())
                .enabled(g.isEnabled())
                .memberCount((int) count)
                .members(members)
                .createdAt(g.getCreatedAt())
                .updatedAt(g.getUpdatedAt())
                .build();
    }

    private GroupMemberResponse toMember(TicketGroupMember m) {
        return GroupMemberResponse.builder()
                .id(m.getId())
                .userId(m.getUserId())
                .role(m.getRole().name())
                .createdAt(m.getCreatedAt())
                .build();
    }
}
