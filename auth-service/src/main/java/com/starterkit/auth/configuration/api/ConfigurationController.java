package com.starterkit.auth.configuration.api;

import com.starterkit.auth.shared.api.response.ApiCode;
import com.starterkit.auth.shared.api.response.ApiResponse;
import com.starterkit.auth.shared.api.response.ApiResponseFactory;
import com.starterkit.auth.configuration.api.dto.ConfigurationRequest;
import com.starterkit.auth.configuration.api.dto.ConfigurationResponse;
import com.starterkit.auth.configuration.application.ConfigurationService;
import com.starterkit.auth.configuration.domain.entity.Configuration;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/auth/configurations")
@RequiredArgsConstructor
public class ConfigurationController {

    private final ConfigurationService configurationService;
    private final ApiResponseFactory responseFactory;

    @GetMapping
    public ApiResponse<List<ConfigurationResponse>> getAll() {

        List<ConfigurationResponse> configurations =
                configurationService.getAllEnabled()
                        .stream()
                        .map(this::toResponse)
                        .toList();

        return responseFactory.success(
                ApiCode.SUCCESS,
                configurations
        );
    }


    @GetMapping("/{configKey}")
    public ApiResponse<ConfigurationResponse> getByKey(
            @PathVariable String configKey
    ) {

        Configuration configuration =
                configurationService.getByKey(configKey);

        return responseFactory.success(
                ApiCode.SUCCESS,
                toResponse(configuration)
        );
    }

    @PostMapping
    @PreAuthorize("hasAuthority('config:write')")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<ConfigurationResponse> create(
            @Valid @RequestBody ConfigurationRequest request
    ) {

        Configuration configuration =
                configurationService.create(request);

        return responseFactory.success(
                ApiCode.SUCCESS,
                toResponse(configuration)
        );
    }

    @PutMapping("/{configKey}")
    @PreAuthorize("hasAuthority('config:write')")
    public ApiResponse<ConfigurationResponse> update(
            @PathVariable String configKey,
            @Valid @RequestBody ConfigurationRequest request
    ) {

        Configuration configuration =
                configurationService.update(configKey, request);

        return responseFactory.success(
                ApiCode.SUCCESS,
                toResponse(configuration)
        );
    }

    @DeleteMapping("/{configKey}")
    @PreAuthorize("hasAuthority('config:delete')")
    public ApiResponse<Void> delete(
            @PathVariable String configKey
    ) {

        configurationService.delete(configKey);

        return responseFactory.success(
                ApiCode.SUCCESS,
                null
        );
    }


    @PostMapping("/{configKey}/reset-default")
    @PreAuthorize("hasAuthority('config:write')")
    public ApiResponse<ConfigurationResponse> resetToDefault(
            @PathVariable String configKey
    ) {

        Configuration configuration =
                configurationService.resetToDefault(configKey);

        return responseFactory.success(
                ApiCode.SUCCESS,
                toResponse(configuration)
        );
    }
    @PostMapping("/reset-defaults")
    @PreAuthorize("hasAuthority('config:write')")
    public ApiResponse<List<ConfigurationResponse>> resetAllToDefault() {

        List<ConfigurationResponse> configurations =
                configurationService.resetAllToDefault()
                        .stream()
                        .map(this::toResponse)
                        .toList();

        return responseFactory.success(
                ApiCode.SUCCESS,
                configurations
        );
    }

    @PostMapping("/{configKey}/set-default")
    @PreAuthorize("hasAuthority('config:write')")
    public ApiResponse<ConfigurationResponse> setCurrentValueAsDefault(
            @PathVariable String configKey
    ) {

        Configuration configuration =
                configurationService.setCurrentValueAsDefault(configKey);

        return responseFactory.success(
                ApiCode.SUCCESS,
                toResponse(configuration)
        );
    }
    private ConfigurationResponse toResponse(
            Configuration configuration
    ) {
        return ConfigurationResponse.builder()
                .id(configuration.getId())
                .configKey(configuration.getConfigKey())
                .configValue(configuration.getConfigValue())
                .defaultValue(configuration.getDefaultValue())
                .valueType(configuration.getValueType())
                .description(configuration.getDescription())
                .enabled(configuration.isEnabled())
                .createdAt(configuration.getCreatedAt())
                .updatedAt(configuration.getUpdatedAt())
                .build();
    }
}