package com.starterkit.auth.auth.api.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Set;

/**
 * Batch lookup of users by username.
 *
 * Request body for POST /internal/users/by-usernames.
 */
@Getter
@Setter
@NoArgsConstructor
public class UsernamesRequest {

    @NotEmpty
    @Size(max = 100)
    private Set<String> usernames;
}
