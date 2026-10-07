package com.starterkit.ticket.ticket.api;

import com.starterkit.ticket.shared.api.response.ApiCode;
import com.starterkit.commons.web.ApiResponse;
import com.starterkit.commons.web.ApiResponseFactory;
import com.starterkit.ticket.shared.infrastructure.jwt.UserPrincipal;
import com.starterkit.ticket.ticket.api.dto.NotificationResponse;
import com.starterkit.ticket.ticket.application.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/tickets/notifications")
@RequiredArgsConstructor
public class TicketNotificationController {

    private final NotificationService service;
    private final ApiResponseFactory factory;

    @GetMapping
    public ApiResponse<Page<NotificationResponse>> list(
            @AuthenticationPrincipal UserPrincipal user,
            @RequestParam(required = false) Boolean unreadOnly,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return factory.success(ApiCode.SUCCESS,
                service.list(user.getId(), unreadOnly, PageRequest.of(page, size)));
    }

    @GetMapping("/unread-count")
    public ApiResponse<Map<String, Long>> unreadCount(@AuthenticationPrincipal UserPrincipal user) {
        long count = service.getUnreadCount(user.getId());
        return factory.success(ApiCode.SUCCESS, Map.of("count", count));
    }

    @PostMapping("/{id}/read")
    public ApiResponse<Void> markAsRead(
            @AuthenticationPrincipal UserPrincipal user,
            @PathVariable Long id
    ) {
        service.markAsRead(user.getId(), id);
        return factory.success(ApiCode.SUCCESS, null);
    }

    @PostMapping("/read-all")
    public ApiResponse<Map<String, Integer>> markAll(
            @AuthenticationPrincipal UserPrincipal user
    ) {
        int count = service.markAllAsRead(user.getId());
        return factory.success(ApiCode.SUCCESS, Map.of("marked", count));
    }
}
