package com.starterkit.ticket.ticket.api.dto;

import lombok.Builder;
import lombok.Getter;
import java.time.Instant;

@Getter
@Builder
public class CommentResponse {
    private final Long id;
    private final Long ticketId;
    private final Long authorId;
    private final String authorRole;
    private final String body;
    private final Instant createdAt;
}
