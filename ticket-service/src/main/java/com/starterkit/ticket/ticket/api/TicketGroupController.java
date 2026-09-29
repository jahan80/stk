package com.starterkit.ticket.ticket.api;

import com.starterkit.ticket.shared.api.response.ApiCode;
import com.starterkit.ticket.shared.api.response.ApiResponse;
import com.starterkit.ticket.shared.api.response.ApiResponseFactory;
import com.starterkit.ticket.shared.infrastructure.jwt.UserPrincipal;
import com.starterkit.ticket.ticket.api.dto.*;
import com.starterkit.ticket.ticket.application.service.TicketGroupService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/tickets/groups")
@RequiredArgsConstructor
public class TicketGroupController {

    private final TicketGroupService service;
    private final ApiResponseFactory responseFactory;

    /** Admin: list all groups */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<List<GroupResponse>> listAll() {
        return responseFactory.success(ApiCode.SUCCESS, service.listAll(true));
    }

    /** User: list groups I'm a member of */
    @GetMapping("/my")
    public ApiResponse<List<GroupResponse>> listMyGroups(
            @AuthenticationPrincipal UserPrincipal user
    ) {
        return responseFactory.success(ApiCode.SUCCESS, service.listMyGroups(user.getId()));
    }

    @GetMapping("/{id}")
    public ApiResponse<GroupResponse> get(@PathVariable Long id) {
        return responseFactory.success(ApiCode.SUCCESS, service.get(id, true));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<GroupResponse> create(@Valid @RequestBody GroupRequest req) {
        return responseFactory.success(ApiCode.SUCCESS, service.create(req));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<GroupResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody GroupRequest req
    ) {
        return responseFactory.success(ApiCode.SUCCESS, service.update(id, req));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return responseFactory.success(ApiCode.SUCCESS, null);
    }

    @GetMapping("/{id}/members")
    public ApiResponse<List<GroupMemberResponse>> listMembers(@PathVariable Long id) {
        return responseFactory.success(ApiCode.SUCCESS, service.listMembers(id));
    }

    @PostMapping("/{id}/members")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<GroupMemberResponse> addMember(
            @PathVariable Long id,
            @Valid @RequestBody GroupMemberRequest req
    ) {
        return responseFactory.success(ApiCode.SUCCESS, service.addMember(id, req));
    }

    @DeleteMapping("/{id}/members/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Void> removeMember(
            @PathVariable Long id,
            @PathVariable Long userId
    ) {
        service.removeMember(id, userId);
        return responseFactory.success(ApiCode.SUCCESS, null);
    }
}
