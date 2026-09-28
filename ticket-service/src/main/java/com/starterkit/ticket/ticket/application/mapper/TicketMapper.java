package com.starterkit.ticket.ticket.application.mapper;

import com.starterkit.ticket.ticket.api.dto.CommentResponse;
import com.starterkit.ticket.ticket.api.dto.TicketResponse;
import com.starterkit.ticket.ticket.api.dto.TicketSummaryResponse;
import com.starterkit.ticket.ticket.domain.entity.Ticket;
import com.starterkit.ticket.ticket.domain.entity.TicketCategory;
import com.starterkit.ticket.ticket.domain.entity.TicketComment;
import com.starterkit.ticket.ticket.domain.entity.TicketGroup;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class TicketMapper {

    public TicketSummaryResponse toSummary(Ticket t, TicketCategory cat, TicketGroup grp) {
        return TicketSummaryResponse.builder()
                .id(t.getId())
                .ticketNumber(t.getTicketNumber())
                .title(t.getTitle())
                .status(t.getStatus().name())
                .priority(t.getPriority().name())
                .categoryId(t.getCategoryId())
                .categoryName(cat != null ? cat.getName() : null)
                .categoryCode(cat != null ? cat.getCode() : null)
                .groupId(t.getGroupId())
                .groupName(grp != null ? grp.getName() : null)
                .createdBy(t.getCreatedBy())
                .assignedTo(t.getAssignedTo())
                .createdAt(t.getCreatedAt())
                .updatedAt(t.getUpdatedAt())
                .build();
    }

    public TicketResponse toResponse(Ticket t, TicketCategory cat, TicketGroup grp,
                                      List<TicketComment> comments, String viewerRole) {
        List<CommentResponse> commentResponses = comments == null ? List.of()
                : comments.stream().map(this::toComment).toList();

        return TicketResponse.builder()
                .id(t.getId())
                .ticketNumber(t.getTicketNumber())
                .title(t.getTitle())
                .description(t.getDescription())
                .status(t.getStatus().name())
                .priority(t.getPriority().name())
                .categoryId(t.getCategoryId())
                .categoryName(cat != null ? cat.getName() : null)
                .categoryCode(cat != null ? cat.getCode() : null)
                .groupId(t.getGroupId())
                .groupName(grp != null ? grp.getName() : null)
                .createdBy(t.getCreatedBy())
                .assignedTo(t.getAssignedTo())
                .createdAt(t.getCreatedAt())
                .updatedAt(t.getUpdatedAt())
                .resolvedAt(t.getResolvedAt())
                .closedAt(t.getClosedAt())
                .slaDeadline(t.getSlaDeadline())
                .viewerRole(viewerRole)
                .commentCount(commentResponses.size())
                .comments(commentResponses)
                .build();
    }

    public CommentResponse toComment(TicketComment c) {
        return CommentResponse.builder()
                .id(c.getId())
                .ticketId(c.getTicketId())
                .authorId(c.getAuthorId())
                .authorRole(c.getAuthorRole().name())
                .body(c.getBody())
                .createdAt(c.getCreatedAt())
                .build();
    }
}
