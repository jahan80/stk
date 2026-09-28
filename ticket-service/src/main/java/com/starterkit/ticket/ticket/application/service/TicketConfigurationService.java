package com.starterkit.ticket.ticket.application.service;

import com.starterkit.ticket.ticket.api.dto.ConfigurationResponse;
import com.starterkit.ticket.ticket.application.exception.ConfigurationNotFoundException;
import com.starterkit.ticket.ticket.domain.entity.TicketConfiguration;
import com.starterkit.ticket.ticket.domain.repository.TicketConfigurationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TicketConfigurationService {

    private final TicketConfigurationRepository repo;

    // ===== TYPED GETTERS =====

    public boolean getBoolean(String key, boolean fallback) {
        try {
            return Boolean.parseBoolean(getValue(key));
        } catch (Exception e) {
            return fallback;
        }
    }

    public int getInt(String key, int fallback) {
        try {
            return Integer.parseInt(getValue(key));
        } catch (Exception e) {
            return fallback;
        }
    }

    public String getString(String key, String fallback) {
        try {
            return getValue(key);
        } catch (Exception e) {
            return fallback;
        }
    }

    private String getValue(String key) {
        return repo.findByConfigKeyAndEnabledTrue(key)
                .map(TicketConfiguration::getConfigValue)
                .orElseThrow(() -> new ConfigurationNotFoundException(key));
    }

    // ===== CRUD =====

    public List<ConfigurationResponse> listAll() {
        return repo.findAllByOrderByConfigKeyAsc().stream()
                .map(this::toResponse).toList();
    }

    @Transactional
    public ConfigurationResponse update(String key, String value) {
        TicketConfiguration c = repo.findByConfigKey(key)
                .orElseThrow(() -> new ConfigurationNotFoundException(key));
        c.setConfigValue(value);
        TicketConfiguration saved = repo.save(c);
        log.info("Config updated: {} = {}", key, value);
        return toResponse(saved);
    }

    @Transactional
    public ConfigurationResponse resetToDefault(String key) {
        TicketConfiguration c = repo.findByConfigKey(key)
                .orElseThrow(() -> new ConfigurationNotFoundException(key));
        c.setConfigValue(c.getDefaultValue());
        return toResponse(repo.save(c));
    }

    private ConfigurationResponse toResponse(TicketConfiguration c) {
        return ConfigurationResponse.builder()
                .id(c.getId())
                .configKey(c.getConfigKey())
                .configValue(c.getConfigValue())
                .defaultValue(c.getDefaultValue())
                .valueType(c.getValueType())
                .description(c.getDescription())
                .enabled(c.isEnabled())
                .createdAt(c.getCreatedAt())
                .updatedAt(c.getUpdatedAt())
                .build();
    }
}
