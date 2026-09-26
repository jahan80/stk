package com.starterkit.auth.auth.api;

import com.starterkit.auth.auth.api.dto.PermissionResponse;
import com.starterkit.auth.auth.application.service.RoleService;
import com.starterkit.auth.shared.api.response.ApiCode;
import com.starterkit.auth.shared.api.response.ApiResponse;
import com.starterkit.auth.shared.api.response.ApiResponseFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/auth/permissions")
@RequiredArgsConstructor
public class PermissionController {

    private final RoleService roleService;
    private final ApiResponseFactory responseFactory;

    @GetMapping
    @PreAuthorize("hasAuthority('permission:read')")
    public ApiResponse<List<PermissionResponse>> listAll() {
        return responseFactory.success(
                ApiCode.SUCCESS,
                roleService.listAllPermissions());
    }
}
