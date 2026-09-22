package com.starterkit.auth.auth.application.service;

import com.starterkit.auth.shared.api.response.ApiCode;
import com.starterkit.auth.auth.application.validator.LoginConfigurationValidator;
import com.starterkit.auth.auth.application.validator.LoginConfigurationValidator.IdentifierType;
import com.starterkit.auth.auth.application.dto.LoginRequest;
import com.starterkit.auth.auth.application.dto.LoginResponse;
import com.starterkit.auth.auth.application.dto.RegisterRequest;
import com.starterkit.auth.auth.application.dto.UserResponse;
import com.starterkit.auth.auth.application.exception.LoginException;
import com.starterkit.auth.auth.application.exception.UserAlreadyExistsException;
import com.starterkit.auth.auth.application.validator.RegisterConfigurationValidator;
import com.starterkit.auth.auth.domain.entity.User;
import com.starterkit.auth.auth.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final RegisterConfigurationValidator registerConfigurationValidator;
    private final LoginConfigurationValidator loginConfigurationValidator;

    @Transactional
    public UserResponse register(RegisterRequest request) {

        registerConfigurationValidator.validate(request);

        if (userRepository.existsByUsername(request.getUsername())) {
            throw new UserAlreadyExistsException("username");
        }

        if (StringUtils.hasText(request.getEmail())
                && userRepository.existsByEmail(request.getEmail())) {
            throw new UserAlreadyExistsException("email");
        }

        if (StringUtils.hasText(request.getMobileNumber())
                && userRepository.existsByMobileNumber(request.getMobileNumber())) {
            throw new UserAlreadyExistsException("mobileNumber");
        }

        User user = new User();

        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setMobileNumber(request.getMobileNumber());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());

        User savedUser = userRepository.save(user);

        return UserResponse.builder()
                .id(savedUser.getId())
                .username(savedUser.getUsername())
                .email(savedUser.getEmail())
                .mobileNumber(savedUser.getMobileNumber())
                .firstName(savedUser.getFirstName())
                .lastName(savedUser.getLastName())
                .build();
    }

    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {

        loginConfigurationValidator.validate(request);

        IdentifierType type = loginConfigurationValidator
                .detectIdentifierType(request.getIdentifier());

        User user = switch (type) {
            case EMAIL -> userRepository.findByEmail(request.getIdentifier())
                    .orElseThrow(() -> new LoginException(ApiCode.INVALID_CREDENTIALS));
            case MOBILE -> userRepository.findByMobileNumber(request.getIdentifier())
                    .orElseThrow(() -> new LoginException(ApiCode.INVALID_CREDENTIALS));
            case USERNAME -> userRepository.findByUsername(request.getIdentifier())
                    .orElseThrow(() -> new LoginException(ApiCode.INVALID_CREDENTIALS));
        };

        if (!user.isEnabled()) {
            throw new LoginException(ApiCode.USER_DISABLED);
        }

        if (!passwordEncoder.matches(
                request.getPassword(),
                user.getPassword())) {
            throw new LoginException(ApiCode.INVALID_CREDENTIALS);
        }

        return LoginResponse.builder()
                .userId(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .mobileNumber(user.getMobileNumber())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .build();
    }
}