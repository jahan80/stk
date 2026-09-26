package com.starterkit.notif.notif.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.util.Map;

@Getter
@Setter
public class PushRequest {

    @NotBlank
    @Size(max = 500)
    private String deviceToken;

    @Size(max = 200)
    private String title;

    @NotBlank
    @Size(max = 2000)
    private String body;

    private Map<String, Object> metadata;
}
