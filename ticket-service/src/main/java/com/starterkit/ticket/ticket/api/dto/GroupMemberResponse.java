package com.starterkit.ticket.ticket.api.dto;

import lombok.Builder;
import lombok.Getter;
import java.time.Instant;

@Getter
@Builder
public class GroupMemberResponse {
    private final Long id;
    private final Long userId;
    private final String role;
    private final Instant createdAt;
}
