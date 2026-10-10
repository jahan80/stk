package com.starterkit.notif.notif.api;

import com.starterkit.commons.web.ApiResponse;
import com.starterkit.commons.web.ApiResponseFactory;
import com.starterkit.notif.notif.api.dto.NotificationResponse;
import com.starterkit.notif.notif.application.InAppNotificationService;
import com.starterkit.notif.shared.api.response.ApiCode;
import com.starterkit.notif.shared.infrastructure.jwt.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/notify/me")
@RequiredArgsConstructor
public class NotifyMeController {

    private final InAppNotificationService inAppService;
    private final ApiResponseFactory responseFactory;

    @GetMapping
    public ApiResponse<Page<NotificationResponse>> list(
            @AuthenticationPrincipal UserPrincipal user,
            @RequestParam(required = false) Boolean unreadOnly,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        boolean unread = Boolean.TRUE.equals(unreadOnly);
        return responseFactory.success(ApiCode.SUCCESS,
                inAppService.listForUser(
                        user.getId(),
                        unread,
                        PageRequest.of(page, Math.min(size, 100))));
    }

    @GetMapping("/unread-count")
    public ApiResponse<Map<String, Long>> unreadCount(
            @AuthenticationPrincipal UserPrincipal user
    ) {
        long count = inAppService.countUnread(user.getId());
        return responseFactory.success(ApiCode.SUCCESS, Map.of("count", count));
    }

    @PostMapping("/{id}/read")
    public ApiResponse<Void> markRead(
            @AuthenticationPrincipal UserPrincipal user,
            @PathVariable UUID id
    ) {
        inAppService.markReadByNotificationId(user.getId(), id);
        return responseFactory.success(ApiCode.SUCCESS, null);
    }

    @PostMapping("/read-all")
    public ApiResponse<Map<String, Integer>> markAllRead(
            @AuthenticationPrincipal UserPrincipal user
    ) {
        int count = inAppService.markAllRead(user.getId());
        return responseFactory.success(ApiCode.SUCCESS, Map.of("marked", count));
    }
}
