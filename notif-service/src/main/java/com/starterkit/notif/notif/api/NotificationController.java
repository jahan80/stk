package com.starterkit.notif.notif.api;

import com.starterkit.notif.notif.api.dto.EmailRequest;
import com.starterkit.notif.notif.api.dto.NotificationResponse;
import com.starterkit.notif.notif.api.dto.PushRequest;
import com.starterkit.notif.notif.api.dto.SmsRequest;
import com.starterkit.notif.notif.application.NotificationService;
import com.starterkit.notif.notif.domain.entity.Notification;
import com.starterkit.notif.shared.api.response.ApiCode;
import com.starterkit.commons.web.ApiResponse;
import com.starterkit.commons.web.ApiResponseFactory;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/notify")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;
    private final ApiResponseFactory responseFactory;

    @PostMapping("/sms")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<NotificationResponse> sendSms(
            @Valid @RequestBody SmsRequest request
    ) {
        return responseFactory.success(
                ApiCode.SUCCESS,
                notificationService.sendSms(request, UUID.randomUUID())
        );
    }

    @PostMapping("/email")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<NotificationResponse> sendEmail(
            @Valid @RequestBody EmailRequest request
    ) {
        return responseFactory.success(
                ApiCode.SUCCESS,
                notificationService.sendEmail(request, UUID.randomUUID())
        );
    }

    @PostMapping("/push")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<NotificationResponse> sendPush(
            @Valid @RequestBody PushRequest request
    ) {
        return responseFactory.success(
                ApiCode.SUCCESS,
                notificationService.sendPush(request, UUID.randomUUID())
        );
    }

    @GetMapping("/{notificationId}")
    public ApiResponse<NotificationResponse> getById(
            @PathVariable UUID notificationId
    ) {
        return responseFactory.success(
                ApiCode.SUCCESS,
                notificationService.getById(notificationId)
        );
    }

    @GetMapping
    public ApiResponse<Page<NotificationResponse>> search(
            @RequestParam(required = false) String channel,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String recipient,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Pageable pageable = PageRequest.of(page, size);

        Notification.Channel channelEnum = null;
        if (channel != null && !channel.isBlank()) {
            channelEnum = Notification.Channel.valueOf(channel.toUpperCase());
        }

        Notification.Status statusEnum = null;
        if (status != null && !status.isBlank()) {
            statusEnum = Notification.Status.valueOf(status.toUpperCase());
        }

        return responseFactory.success(
                ApiCode.SUCCESS,
                notificationService.search(channelEnum, statusEnum, recipient, pageable)
        );
    }
}
