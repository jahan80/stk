package com.starterkit.ticket.ticket.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class GroupRequest {
    @NotBlank @Size(max = 100)
    private String name;

    @Size(max = 500)
    private String description;

    private Boolean enabled;
}
