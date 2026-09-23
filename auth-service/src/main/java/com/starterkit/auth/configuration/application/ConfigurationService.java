package com.starterkit.auth.configuration.application;

import com.starterkit.auth.configuration.api.dto.ConfigurationRequest;
import com.starterkit.auth.configuration.domain.entity.Configuration;
import com.starterkit.auth.configuration.domain.repository.ConfigurationRepository;
import com.starterkit.auth.configuration.exception.ConfigurationAlreadyExistsException;
import com.starterkit.auth.configuration.exception.ConfigurationNotFoundException;
import com.starterkit.auth.configuration.exception.ConfigurationValidationException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ConfigurationService {

    private final ConfigurationRepository configurationRepository;

    public String getValue(String configKey) {
        return getRequiredConfiguration(configKey).getConfigValue();
    }


    public boolean getBoolean(String configKey) {
        return Boolean.parseBoolean(
                getRequiredConfiguration(configKey).getConfigValue()
        );
    }

    public int getInteger(String configKey) {
        return Integer.parseInt(
                getRequiredConfiguration(configKey).getConfigValue()
        );
    }

    public long getLong(String configKey) {
        return Long.parseLong(
                getRequiredConfiguration(configKey).getConfigValue()
        );
    }

    public double getDouble(String configKey) {
        return Double.parseDouble(
                getRequiredConfiguration(configKey).getConfigValue()
        );
    }

    public Configuration getByKey(String configKey) {
        return configurationRepository
                .findByConfigKey(configKey)
                .orElseThrow(() ->
                        new ConfigurationNotFoundException(configKey)
                );
    }

    public List<Configuration> getAllEnabled() {
        return configurationRepository
                .findAllByEnabledTrueOrderByConfigKeyAsc();
    }

    @Transactional
    public Configuration create(ConfigurationRequest request) {

        validateConfigKey(request.getConfigKey());
        validateConfigurationValues(request);

        if (configurationRepository
                .findByConfigKey(request.getConfigKey())
                .isPresent()) {

            throw new ConfigurationAlreadyExistsException(
                    request.getConfigKey()
            );
        }

        Configuration configuration = new Configuration();

        configuration.setConfigKey(request.getConfigKey());
        configuration.setConfigValue(request.getConfigValue());
        configuration.setDefaultValue(request.getDefaultValue());
        configuration.setValueType(request.getValueType().toUpperCase());
        configuration.setDescription(request.getDescription());
        configuration.setEnabled(request.isEnabled());

        return configurationRepository.save(configuration);
    }

    @Transactional
    public Configuration update(
            String configKey,
            ConfigurationRequest request
    ) {

        validateConfigurationValues(request);

        Configuration configuration = configurationRepository
                .findByConfigKey(configKey)
                .orElseThrow(() ->
                        new ConfigurationNotFoundException(configKey)
                );

        configuration.setConfigValue(request.getConfigValue());
        configuration.setDefaultValue(request.getDefaultValue());
        configuration.setValueType(request.getValueType().toUpperCase());
        configuration.setDescription(request.getDescription());
        configuration.setEnabled(request.isEnabled());

        return configurationRepository.save(configuration);
    }

    @Transactional
    public void delete(String configKey) {

        Configuration configuration = configurationRepository
                .findByConfigKey(configKey)
                .orElseThrow(() ->
                        new ConfigurationNotFoundException(configKey)
                );

        configurationRepository.delete(configuration);
    }

    @Transactional
    public Configuration resetToDefault(String configKey) {

        Configuration configuration = configurationRepository
                .findByConfigKey(configKey)
                .orElseThrow(() ->
                        new ConfigurationNotFoundException(configKey)
                );

        configuration.setConfigValue(
                configuration.getDefaultValue()
        );

        return configurationRepository.save(configuration);
    }

    @Transactional
    public List<Configuration> resetAllToDefault() {

        List<Configuration> configurations =
                configurationRepository.findAll();

        configurations.forEach(configuration ->
                configuration.setConfigValue(
                        configuration.getDefaultValue()
                )
        );

        return configurationRepository.saveAll(configurations);
    }

    @Transactional
    public Configuration setCurrentValueAsDefault(String configKey) {

        Configuration configuration = configurationRepository
                .findByConfigKey(configKey)
                .orElseThrow(() ->
                        new ConfigurationNotFoundException(configKey)
                );

        configuration.setDefaultValue(
                configuration.getConfigValue()
        );

        return configurationRepository.save(configuration);
    }

    private Configuration getRequiredConfiguration(String configKey) {

        return configurationRepository
                .findByConfigKeyAndEnabledTrue(configKey)
                .orElseThrow(() ->
                        new ConfigurationNotFoundException(configKey)
                );
    }

    private void validateConfigKey(String configKey) {

        if (!StringUtils.hasText(configKey)) {
            throw new IllegalArgumentException(
                    "Configuration key must not be empty"
            );
        }
    }

    private void validateConfigurationValues(
            ConfigurationRequest request
    ) {

        String valueType = request.getValueType();

        if (!StringUtils.hasText(valueType)) {
            throw new ConfigurationValidationException(
                    "valueType",
                    ConfigurationValidationException.Reason.INVALID_VALUE
            );
        }

        valueType = valueType.trim().toUpperCase();

        validateValue(
                "configValue",
                request.getConfigValue(),
                valueType
        );

        validateValue(
                "defaultValue",
                request.getDefaultValue(),
                valueType
        );
    }

    private void validateValue(
            String field,
            String value,
            String valueType
    ) {

        if (!StringUtils.hasText(value)) {
            throw new ConfigurationValidationException(
                    field,
                    ConfigurationValidationException.Reason.REQUIRED
            );
        }

        String normalizedValue = value.trim();

        switch (valueType) {

            case "BOOLEAN" -> {
                if (!"true".equalsIgnoreCase(normalizedValue)
                        && !"false".equalsIgnoreCase(normalizedValue)) {

                    throw new ConfigurationValidationException(
                            field,
                            ConfigurationValidationException.Reason.INVALID_VALUE
                    );
                }
            }

            case "INTEGER" -> {
                try {
                    Integer.parseInt(normalizedValue);
                } catch (NumberFormatException exception) {
                    throw new ConfigurationValidationException(
                            field,
                            ConfigurationValidationException.Reason.INVALID_VALUE
                    );
                }
            }

            case "LONG" -> {
                try {
                    Long.parseLong(normalizedValue);
                } catch (NumberFormatException exception) {
                    throw new ConfigurationValidationException(
                            field,
                            ConfigurationValidationException.Reason.INVALID_VALUE
                    );
                }
            }

            case "DOUBLE" -> {
                try {
                    Double.parseDouble(normalizedValue);
                } catch (NumberFormatException exception) {
                    throw new ConfigurationValidationException(
                            field,
                            ConfigurationValidationException.Reason.INVALID_VALUE
                    );
                }
            }

            case "STRING", "TEXT" -> {
                // Any non-empty string is valid.
            }

            default -> throw new ConfigurationValidationException(
                    "valueType",
                    ConfigurationValidationException.Reason.INVALID_VALUE
            );
        }
    }
}