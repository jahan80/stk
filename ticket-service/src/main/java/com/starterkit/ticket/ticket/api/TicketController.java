package com.starterkit.ticket.ticket.api;

import com.starterkit.ticket.shared.api.response.ApiCode;
import com.starterkit.commons.web.ApiResponse;
import com.starterkit.commons.web.ApiResponseFactory;
import com.starterkit.ticket.shared.infrastructure.jwt.UserPrincipal;
import com.starterkit.ticket.ticket.api.dto.*;
import com.starterkit.ticket.ticket.application.service.ticket.TicketAssignmentService;
import com.starterkit.ticket.ticket.application.service.ticket.TicketCommandService;
import com.starterkit.ticket.ticket.application.service.ticket.TicketCommentService;
import com.starterkit.ticket.ticket.application.service.ticket.TicketQueryService;
import com.starterkit.ticket.ticket.domain.entity.TicketPriority;
import com.starterkit.ticket.ticket.domain.entity.TicketStatus;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Ticket REST API.
 *
 * Delegates to four specialized services:
 *   - TicketCommandService     (create, close, changeStatus)
 *   - TicketQueryService       (list, get, listComments)
 *   - TicketAssignmentService  (assign, assignToGroup)
 *   - TicketCommentService     (addComment)
 */
@RestController
@RequestMapping("/tickets")
@RequiredArgsConstructor
public class TicketController {

    private final TicketCommandService commandService;
    private final TicketQueryService queryService;
    private final TicketAssignmentService assignmentService;
    private final TicketCommentService commentService;
    private final ApiResponseFactory responseFactory;

    // =====================================================
    // LIST
    // =====================================================

    @GetMapping
    public ApiResponse<Page<TicketSummaryResponse>> list(
            @AuthenticationPrincipal UserPrincipal user,
            @RequestParam(required = false) TicketStatus status,
            @RequestParam(required = false) TicketPriority priority,
            @RequestParam(required = false) Long groupId,
            @RequestParam(required = false, defaultValue = "false") boolean unassignedOnly,
            @RequestParam(required = false, defaultValue = "false") boolean assignedOnly,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return responseFactory.success(ApiCode.SUCCESS,
                queryService.list(user, status, priority, groupId, unassignedOnly, assignedOnly,
                        PageRequest.of(page, Math.min(size, 100))));
    }

    // =====================================================
    // CREATE
    // =====================================================

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('ticket:write') or hasRole('ADMIN')")
    public ApiResponse<TicketResponse> create(
            @AuthenticationPrincipal UserPrincipal user,
            @Valid @RequestBody CreateTicketRequest req
    ) {
        return responseFactory.success(ApiCode.TICKET_CREATED,
                commandService.create(user, req));
    }

    // =====================================================
    // DETAIL
    // =====================================================

    @GetMapping("/{id}")
    public ApiResponse<TicketResponse> get(
            @AuthenticationPrincipal UserPrincipal user,
            @PathVariable Long id
    ) {
        return responseFactory.success(ApiCode.SUCCESS, queryService.get(user, id));
    }

    // =====================================================
    // CLOSE
    // =====================================================

    @PostMapping("/{id}/close")
    public ApiResponse<TicketResponse> close(
            @AuthenticationPrincipal UserPrincipal user,
            @PathVariable Long id
    ) {
        return responseFactory.success(ApiCode.SUCCESS, commandService.close(user, id));
    }

    // =====================================================
    // STATUS
    // =====================================================

    @PostMapping("/{id}/status")
    public ApiResponse<TicketResponse> changeStatus(
            @AuthenticationPrincipal UserPrincipal user,
            @PathVariable Long id,
            @Valid @RequestBody ChangeStatusRequest req
    ) {
        return responseFactory.success(ApiCode.SUCCESS,
                commandService.changeStatus(user, id, req.getStatus()));
    }

    // =====================================================
    // ASSIGN
    // =====================================================

    @PostMapping("/{id}/assign")
    public ApiResponse<TicketResponse> assign(
            @AuthenticationPrincipal UserPrincipal user,
            @PathVariable Long id,
            @Valid @RequestBody AssignTicketRequest req
    ) {
        return responseFactory.success(ApiCode.SUCCESS,
                assignmentService.assign(user, id, req.getAssigneeId()));
    }

    @PostMapping("/{id}/assign-group")
    public ApiResponse<TicketResponse> assignToGroup(
            @AuthenticationPrincipal UserPrincipal user,
            @PathVariable Long id,
            @RequestParam(required = false) Long groupId
    ) {
        return responseFactory.success(ApiCode.SUCCESS,
                assignmentService.assignToGroup(user, id, groupId));
    }

    // =====================================================
    // COMMENTS
    // =====================================================

    @GetMapping("/{id}/comments")
    public ApiResponse<List<CommentResponse>> listComments(
            @AuthenticationPrincipal UserPrincipal user,
            @PathVariable Long id
    ) {
        return responseFactory.success(ApiCode.SUCCESS,
                queryService.listComments(user, id));
    }

    @PostMapping("/{id}/comments")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('ticket:write') or hasRole('ADMIN')")
    public ApiResponse<CommentResponse> addComment(
            @AuthenticationPrincipal UserPrincipal user,
            @PathVariable Long id,
            @Valid @RequestBody AddCommentRequest req
    ) {
        return responseFactory.success(ApiCode.COMMENT_ADDED,
                commentService.addComment(user, id, req.getBody()));
    }
}
