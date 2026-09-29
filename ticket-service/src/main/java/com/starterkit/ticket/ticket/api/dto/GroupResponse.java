package com.starterkit.ticket.ticket.api.dto;

import lombok.Builder;
import lombok.Getter;
import java.time.Instant;
import java.util.List;

@Getter
@Builder
public class GroupResponse {
    private final Long id;
    private final String name;
    private final String description;
    private final boolean enabled;
    private final int memberCount;
    private final List<GroupMemberResponse> members;
    private final Instant createdAt;
    private final Instant updatedAt;
}
