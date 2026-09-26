package com.starterkit.auth.auth.api;

import com.starterkit.auth.auth.api.dto.RoleRequest;
import com.starterkit.auth.auth.api.dto.RoleResponse;
import com.starterkit.auth.auth.application.service.RoleService;
import com.starterkit.auth.shared.api.response.ApiCode;
import com.starterkit.auth.shared.api.response.ApiResponse;
import com.starterkit.auth.shared.api.response.ApiResponseFactory;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/auth/roles")
@RequiredArgsConstructor
public class RoleController {

    private final RoleService roleService;
    private final ApiResponseFactory responseFactory;

    @GetMapping
    @PreAuthorize("hasAuthority('role:read')")
    public ApiResponse<List<RoleResponse>> listAll() {
        return responseFactory.success(ApiCode.SUCCESS, roleService.listAll());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('role:read')")
    public ApiResponse<RoleResponse> getById(@PathVariable Long id) {
        return responseFactory.success(ApiCode.SUCCESS, roleService.getById(id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('role:write')")
    public ApiResponse<RoleResponse> create(
            @Valid @RequestBody RoleRequest request
    ) {
        return responseFactory.success(ApiCode.SUCCESS, roleService.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('role:write')")
    public ApiResponse<RoleResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody RoleRequest request
    ) {
        return responseFactory.success(ApiCode.SUCCESS, roleService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('role:delete')")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        roleService.delete(id);
        return responseFactory.success(ApiCode.SUCCESS, null);
    }

    @PutMapping("/{id}/permissions")
    @PreAuthorize("hasAuthority('role:write')")
    public ApiResponse<RoleResponse> assignPermissions(
            @PathVariable Long id,
            @RequestBody Set<Long> permissionIds
    ) {
        return responseFactory.success(
                ApiCode.SUCCESS,
                roleService.assignPermissions(id, permissionIds));
    }
}
