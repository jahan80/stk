package com.starterkit.auth.auth.api;

import com.starterkit.auth.auth.api.dto.AssignRoleRequest;
import com.starterkit.auth.auth.api.dto.UserDetailResponse;
import com.starterkit.auth.auth.api.dto.UserSummaryResponse;
import com.starterkit.auth.auth.application.service.UserManagementService;
import com.starterkit.auth.shared.api.response.ApiCode;
import com.starterkit.auth.shared.api.response.ApiResponse;
import com.starterkit.auth.shared.api.response.ApiResponseFactory;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/auth/users")
@RequiredArgsConstructor
public class UserController {

    private final UserManagementService userManagementService;
    private final ApiResponseFactory responseFactory;

    @GetMapping
    @PreAuthorize("hasAuthority('user:read')")
    public ApiResponse<List<UserSummaryResponse>> listAll() {
        return responseFactory.success(
                ApiCode.SUCCESS,
                userManagementService.listAll());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('user:read')")
    public ApiResponse<UserDetailResponse> getById(@PathVariable Long id) {
        return responseFactory.success(
                ApiCode.SUCCESS,
                userManagementService.getById(id));
    }

    @PutMapping("/{id}/role")
    @PreAuthorize("hasAuthority('user:assign-role')")
    public ApiResponse<UserDetailResponse> assignRole(
            @PathVariable Long id,
            @Valid @RequestBody AssignRoleRequest request
    ) {
        return responseFactory.success(
                ApiCode.SUCCESS,
                userManagementService.assignRole(id, request));
    }

    @PostMapping("/{id}/enable")
    @PreAuthorize("hasAuthority('user:write')")
    public ApiResponse<UserDetailResponse> enable(@PathVariable Long id) {
        return responseFactory.success(
                ApiCode.SUCCESS,
                userManagementService.setEnabled(id, true));
    }

    @PostMapping("/{id}/disable")
    @PreAuthorize("hasAuthority('user:write')")
    public ApiResponse<UserDetailResponse> disable(@PathVariable Long id) {
        return responseFactory.success(
                ApiCode.SUCCESS,
                userManagementService.setEnabled(id, false));
    }
}
