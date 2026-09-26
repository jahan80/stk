package com.starterkit.notif.notif.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.util.Map;

@Getter
@Setter
public class SmsRequest {

    @NotBlank
    @Pattern(regexp = "^\\+?[0-9]{8,20}$", message = "Invalid phone number")
    private String to;

    @NotBlank
    @Size(max = 2000)
    private String message;

    private Map<String, Object> metadata;
}
