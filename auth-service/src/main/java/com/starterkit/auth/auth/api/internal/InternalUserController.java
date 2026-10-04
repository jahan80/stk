package com.starterkit.auth.auth.api.internal;

import com.starterkit.auth.auth.api.dto.UserContactResponse;
import com.starterkit.auth.auth.application.service.UserManagementService;
import com.starterkit.auth.shared.api.response.ApiCode;
import com.starterkit.auth.shared.api.response.ApiResponse;
import com.starterkit.auth.shared.api.response.ApiResponseFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Service-to-service internal endpoints.
 *
 * Security:
 *   - These endpoints are NOT exposed via api-gateway (no route).
 *   - They are only reachable from inside the docker network.
 *   - Callers must be other StarterKit services.
 *
 * NOTE: Currently no auth is enforced because all callers are
 * internal services. If the network becomes untrusted, add an
 * internal-token check (X-Internal-Token header).
 */
@Slf4j
@RestController
@RequestMapping("/internal/users")
@RequiredArgsConstructor
public class InternalUserController {

    private final UserManagementService userManagementService;
    private final ApiResponseFactory responseFactory;

    @GetMapping("/{id}/contact")
    public ApiResponse<UserContactResponse> getContact(@PathVariable Long id) {
        log.debug("Internal contact lookup for userId={}", id);
        return responseFactory.success(
                ApiCode.SUCCESS,
                userManagementService.getContact(id)
        );
    }
}
