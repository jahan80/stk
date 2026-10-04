package com.starterkit.ticket.shared.infrastructure.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Mirror of auth-service UserContactResponse.
 * Only the fields we care about.
 */
@Getter
@Setter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class UserContact {
    private Long id;
    private String email;
    private String mobileNumber;
    private String firstName;
    private String lastName;
}
