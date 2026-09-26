package com.starterkit.auth.auth.application.validator;

import com.starterkit.auth.auth.api.dto.RegisterRequest;
import com.starterkit.auth.configuration.exception.ConfigurationValidationException;
import com.starterkit.auth.configuration.application.ConfigurationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
@RequiredArgsConstructor
public class RegisterConfigurationValidator {

    private final ConfigurationService configurationService;

    public void validate(RegisterRequest request) {

        // username and password are ALWAYS required - not configurable
        validateRequired("username", request.getUsername());
        validateRequired("password", request.getPassword());

        validateField(
                "email",
                request.getEmail(),
                "AUTH.REGISTER.EMAIL.ENABLED",
                "AUTH.REGISTER.EMAIL.REQUIRED"
        );

        validateField(
                "mobileNumber",
                request.getMobileNumber(),
                "AUTH.REGISTER.MOBILE_NUMBER.ENABLED",
                "AUTH.REGISTER.MOBILE_NUMBER.REQUIRED"
        );

        validateField(
                "firstName",
                request.getFirstName(),
                "AUTH.REGISTER.FIRST_NAME.ENABLED",
                "AUTH.REGISTER.FIRST_NAME.REQUIRED"
        );

        validateField(
                "lastName",
                request.getLastName(),
                "AUTH.REGISTER.LAST_NAME.ENABLED",
                "AUTH.REGISTER.LAST_NAME.REQUIRED"
        );
    }

    private void validateRequired(
            String field,
            String value
    ) {

        if (!StringUtils.hasText(value)) {
            throw new ConfigurationValidationException(
                    field,
                    ConfigurationValidationException.Reason.REQUIRED
            );
        }
    }

    private void validateField(
            String field,
            String value,
            String enabledKey,
            String requiredKey
    ) {

        boolean enabled =
                configurationService.getBoolean(enabledKey);

        boolean required =
                configurationService.getBoolean(requiredKey);

        if (!enabled && StringUtils.hasText(value)) {
            throw new ConfigurationValidationException(
                    field,
                    ConfigurationValidationException.Reason.DISABLED
            );
        }

        if (enabled
                && required
                && !StringUtils.hasText(value)) {

            throw new ConfigurationValidationException(
                    field,
                    ConfigurationValidationException.Reason.REQUIRED
            );
        }
    }
}