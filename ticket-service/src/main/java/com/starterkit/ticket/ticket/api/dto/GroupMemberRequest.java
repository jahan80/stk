package com.starterkit.ticket.ticket.api.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class GroupMemberRequest {
    @NotNull
    private Long userId;

    private String role = "AGENT";
}
