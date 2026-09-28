package com.starterkit.ticket.ticket.api.dto;

import com.starterkit.ticket.ticket.domain.entity.TicketPriority;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateTicketRequest {
    @NotBlank @Size(max = 255)
    private String title;

    @NotBlank @Size(max = 10000)
    private String description;

    @NotNull
    private Long categoryId;

    private TicketPriority priority;
}
