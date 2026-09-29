package com.starterkit.ticket.ticket.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AddCommentRequest {
    @NotBlank @Size(max = 10000)
    private String body;
}
