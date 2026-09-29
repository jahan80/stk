package com.starterkit.ticket.ticket.api.dto;

import com.starterkit.ticket.ticket.domain.entity.TicketStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ChangeStatusRequest {
    @NotNull
    private TicketStatus status;
}
