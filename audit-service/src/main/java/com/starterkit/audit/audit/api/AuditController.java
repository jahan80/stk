package com.starterkit.audit.audit.api;

import com.starterkit.audit.audit.api.dto.AuditEventResponse;
import com.starterkit.audit.audit.application.service.AuditEventService;
import com.starterkit.audit.shared.api.response.ApiCode;
import com.starterkit.commons.web.ApiResponse;
import com.starterkit.commons.web.ApiResponseFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/audit/events")
@RequiredArgsConstructor
public class AuditController {

    private final AuditEventService auditEventService;
    private final ApiResponseFactory responseFactory;

    @GetMapping
    @PreAuthorize("hasAuthority('audit:read')")
    public ApiResponse<Page<AuditEventResponse>> search(
            @RequestParam(required = false) String eventType,
            @RequestParam(required = false) String source,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Pageable pageable = PageRequest.of(page, Math.min(size, 100));

        Page<AuditEventResponse> events = auditEventService.search(
                eventType, source, from, to, pageable
        );

        return responseFactory.success(ApiCode.SUCCESS, events);
    }

    @GetMapping("/trace/{traceId}")
    @PreAuthorize("hasAuthority('audit:read')")
    public ApiResponse<List<AuditEventResponse>> findByTraceId(
            @PathVariable String traceId
    ) {
        return responseFactory.success(
                ApiCode.SUCCESS,
                auditEventService.findByTraceId(traceId)
        );
    }
}
