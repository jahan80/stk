package com.starterkit.auth.auth.api.internal;

import com.starterkit.auth.auth.api.dto.UserContactResponse;
import com.starterkit.auth.auth.application.service.UserManagementService;
import com.starterkit.auth.shared.api.response.ApiCode;
import com.starterkit.commons.web.ApiResponse;
import com.starterkit.commons.web.ApiResponseFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.starterkit.auth.auth.api.dto.UsernamesRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import java.util.List;
import java.util.Map;

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

    /**
     * Batch lookup of usernames → userId.
     *
     * Used by ticket-service to resolve @mentions in comments.
     * Unknown usernames are silently omitted from the response map.
     */
    @PostMapping("/by-usernames")
    public ApiResponse<Map<String, Long>> getByUsernames(
            @Valid @RequestBody UsernamesRequest request
    ) {
        log.debug("Internal batch username lookup: {} usernames",
                request.getUsernames().size());
        return responseFactory.success(
                ApiCode.SUCCESS,
                userManagementService.getUserIdsByUsernames(request.getUsernames())
        );
    }

    /**
     * All ADMIN user IDs.
     *
     * Used by ticket-service to notify admins on ticket creation.
     */
    @GetMapping("/admins")
    public ApiResponse<List<Long>> getAdminIds() {
        log.debug("Internal admin user IDs lookup");
        return responseFactory.success(
                ApiCode.SUCCESS,
                userManagementService.getAdminUserIds()
        );
    }
}
