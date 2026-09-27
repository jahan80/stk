package com.starterkit.auth.auth.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class VerifyMobileRequest {

    @NotBlank
    @Pattern(regexp = "^\\+?[0-9]{8,20}$", message = "Invalid mobile number")
    private String mobileNumber;

    @NotBlank
    @Size(min = 4, max = 10)
    private String code;
}
