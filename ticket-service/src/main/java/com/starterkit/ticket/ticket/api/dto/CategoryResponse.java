package com.starterkit.ticket.ticket.api.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CategoryResponse {
    private final Long id;
    private final String code;
    private final String name;
    private final String description;
    private final boolean enabled;
    private final Integer displayOrder;
}
