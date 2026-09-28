package com.starterkit.ticket.ticket.api.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateConfigurationRequest {

    @NotBlank(message = "Value is required")
    private String configValue;
}
