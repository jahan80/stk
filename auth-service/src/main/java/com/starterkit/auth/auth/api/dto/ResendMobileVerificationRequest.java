package com.starterkit.auth.auth.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ResendMobileVerificationRequest {

    @NotBlank
    @Pattern(regexp = "^\\+?[0-9]{8,20}$", message = "Invalid mobile number")
    private String mobileNumber;
}
