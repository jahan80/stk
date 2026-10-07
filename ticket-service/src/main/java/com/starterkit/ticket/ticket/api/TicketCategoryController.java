package com.starterkit.ticket.ticket.api;

import com.starterkit.ticket.shared.api.response.ApiCode;
import com.starterkit.commons.web.ApiResponse;
import com.starterkit.commons.web.ApiResponseFactory;
import com.starterkit.ticket.ticket.api.dto.CategoryResponse;
import com.starterkit.ticket.ticket.api.dto.CreateCategoryRequest;
import com.starterkit.ticket.ticket.api.dto.UpdateCategoryRequest;
import com.starterkit.ticket.ticket.application.service.TicketCategoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/tickets/categories")
@RequiredArgsConstructor
public class TicketCategoryController {

    private final TicketCategoryService service;
    private final ApiResponseFactory responseFactory;

    // ===== PUBLIC / AUTHENTICATED =====

    /** Public list of enabled categories (for dropdowns). */
    @GetMapping
    public ApiResponse<List<CategoryResponse>> listEnabled() {
        return responseFactory.success(ApiCode.SUCCESS, service.listEnabled());
    }

    /** Detail of a single category. */
    @GetMapping("/{id}")
    public ApiResponse<CategoryResponse> getById(@PathVariable Long id) {
        return responseFactory.success(ApiCode.SUCCESS, service.getById(id));
    }

    // ===== ADMIN ONLY =====

    /** Admin view: includes disabled categories. */
    @GetMapping("/all")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<List<CategoryResponse>> listAll() {
        return responseFactory.success(ApiCode.SUCCESS, service.listAll());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<CategoryResponse> create(
            @Valid @RequestBody CreateCategoryRequest req
    ) {
        return responseFactory.success(ApiCode.SUCCESS, service.create(req));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<CategoryResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody UpdateCategoryRequest req
    ) {
        return responseFactory.success(ApiCode.SUCCESS, service.update(id, req));
    }

    @PostMapping("/{id}/enable")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<CategoryResponse> enable(@PathVariable Long id) {
        return responseFactory.success(ApiCode.SUCCESS, service.setEnabled(id, true));
    }

    @PostMapping("/{id}/disable")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<CategoryResponse> disable(@PathVariable Long id) {
        return responseFactory.success(ApiCode.SUCCESS, service.setEnabled(id, false));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return responseFactory.success(ApiCode.SUCCESS, null);
    }
}
