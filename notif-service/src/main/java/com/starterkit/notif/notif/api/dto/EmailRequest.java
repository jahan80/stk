package com.starterkit.notif.notif.api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.util.Map;

@Getter
@Setter
public class EmailRequest {

    @NotBlank
    @Email
    @Size(max = 255)
    private String to;

    @Size(max = 500)
    private String subject;

    @NotBlank
    private String body;

    private Map<String, Object> metadata;
}
