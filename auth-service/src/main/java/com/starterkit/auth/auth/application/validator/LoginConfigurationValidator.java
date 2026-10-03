package com.starterkit.auth.auth.application.validator;

import com.starterkit.auth.shared.api.response.ApiCode;
import com.starterkit.auth.auth.api.dto.LoginRequest;
import com.starterkit.auth.auth.application.exception.LoginException;
import com.starterkit.auth.configuration.application.ConfigurationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
@RequiredArgsConstructor
public class LoginConfigurationValidator {

    private final ConfigurationService configurationService;

    public void validate(LoginRequest request) {

        if (!configurationService.getBoolean("AUTH.LOGIN.ENABLED")) {
            throw new LoginException(ApiCode.LOGIN_DISABLED);
        }

        if (request == null
                || !StringUtils.hasText(request.getIdentifier())
                || !StringUtils.hasText(request.getPassword())) {
            throw new LoginException(ApiCode.INVALID_CREDENTIALS);
        }

        IdentifierType type = detectIdentifierType(request.getIdentifier());

        boolean allowed = switch (type) {
            case EMAIL -> configurationService.getBoolean(
                    "AUTH.LOGIN.EMAIL.ENABLED");
            case MOBILE -> configurationService.getBoolean(
                    "AUTH.LOGIN.MOBILE_NUMBER.ENABLED");
            case USERNAME -> configurationService.getBoolean(
                    "AUTH.LOGIN.USERNAME.ENABLED");
        };

        if (!allowed) {
            throw new LoginException(ApiCode.LOGIN_IDENTIFIER_NOT_ALLOWED);
        }
    }

    public IdentifierType detectIdentifierType(String identifier) {
        if (identifier.contains("@")) {
            return IdentifierType.EMAIL;
        }
        // Mobile: 8-20 digits, optionally starting with +
        if (identifier.matches("\\+?[0-9]{8,20}")) {
            return IdentifierType.MOBILE;
        }
        return IdentifierType.USERNAME;
    }

    public enum IdentifierType {
        EMAIL, MOBILE, USERNAME
    }
}