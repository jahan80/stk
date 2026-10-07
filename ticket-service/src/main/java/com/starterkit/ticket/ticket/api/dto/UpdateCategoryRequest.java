package com.starterkit.ticket.ticket.api.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/**
 * Update does NOT allow changing `code` — it is immutable
 * (it is referenced in events / external integrations).
 */
@Getter
@Setter
public class UpdateCategoryRequest {

    @NotBlank
    @Size(max = 100)
    private String name;

    @Size(max = 500)
    private String description;

    @Min(0)
    private Integer displayOrder;

    private Boolean enabled;
}
