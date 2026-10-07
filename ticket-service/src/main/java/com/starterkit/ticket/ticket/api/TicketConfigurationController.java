package com.starterkit.ticket.ticket.api;

import com.starterkit.ticket.shared.api.response.ApiCode;
import com.starterkit.commons.web.ApiResponse;
import com.starterkit.commons.web.ApiResponseFactory;
import com.starterkit.ticket.ticket.api.dto.ConfigurationResponse;
import com.starterkit.ticket.ticket.api.dto.UpdateConfigurationRequest;
import com.starterkit.ticket.ticket.application.service.TicketConfigurationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/tickets/configurations")
@RequiredArgsConstructor
public class TicketConfigurationController {

    private final TicketConfigurationService service;
    private final ApiResponseFactory factory;

    /** Admin only */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<List<ConfigurationResponse>> list() {
        return factory.success(ApiCode.SUCCESS, service.listAll());
    }

    @PutMapping("/{key}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<ConfigurationResponse> update(
            @PathVariable String key,
            @Valid @RequestBody UpdateConfigurationRequest req
    ) {
        return factory.success(ApiCode.SUCCESS, service.update(key, req.getConfigValue()));
    }

    @PostMapping("/{key}/reset-default")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<ConfigurationResponse> reset(@PathVariable String key) {
        return factory.success(ApiCode.SUCCESS, service.resetToDefault(key));
    }
}
