package com.starterkit.auth.auth.application.service;

import com.starterkit.auth.auth.api.dto.LoginRequest;
import com.starterkit.auth.auth.api.dto.LoginResponse;
import com.starterkit.auth.auth.api.dto.RegisterRequest;
import com.starterkit.auth.auth.api.dto.RegisterResponse;
import com.starterkit.auth.auth.application.exception.LoginException;
import com.starterkit.auth.auth.application.exception.UserAlreadyExistsException;
import com.starterkit.auth.auth.application.validator.LoginConfigurationValidator;
import com.starterkit.auth.auth.application.validator.LoginConfigurationValidator.IdentifierType;
import com.starterkit.auth.auth.application.validator.RegisterConfigurationValidator;
import com.starterkit.auth.auth.domain.entity.Role;
import com.starterkit.auth.auth.domain.entity.User;
import com.starterkit.auth.auth.domain.repository.RoleRepository;
import com.starterkit.auth.auth.domain.repository.UserRepository;
import com.starterkit.auth.shared.api.response.ApiCode;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class AuthService {

    private static final String DEFAULT_ROLE = "USER";

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final RegisterConfigurationValidator registerConfigurationValidator;
    private final LoginConfigurationValidator loginConfigurationValidator;
    private final TokenService tokenService;

    @Transactional
    public RegisterResponse register(RegisterRequest request) {

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

        Role defaultRole = roleRepository.findByName(DEFAULT_ROLE)
                .orElseThrow(() -> new IllegalStateException(
                        "Default role '" + DEFAULT_ROLE + "' not found in database"
                ));

        User user = new User();

        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setMobileNumber(request.getMobileNumber());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setRole(defaultRole);

        User savedUser = userRepository.save(user);

        return RegisterResponse.builder()
                .id(savedUser.getId())
                .username(savedUser.getUsername())
                .email(savedUser.getEmail())
                .mobileNumber(savedUser.getMobileNumber())
                .firstName(savedUser.getFirstName())
                .lastName(savedUser.getLastName())
                .build();
    }

    @Transactional
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

        TokenService.TokenPair tokens = tokenService.generateTokens(user);

        return LoginResponse.builder()
                .accessToken(tokens.accessToken())
                .refreshToken(tokens.refreshToken())
                .tokenType("Bearer")
                .expiresIn(tokens.expiresInSeconds())
                .user(LoginResponse.UserInfo.builder()
                        .id(user.getId())
                        .username(user.getUsername())
                        .email(user.getEmail())
                        .role(user.getRole().getName())
                        .build())
                .build();
    }
}
