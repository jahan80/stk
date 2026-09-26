package com.starterkit.auth.auth.application.service;

import com.starterkit.auth.auth.api.dto.LoginRequest;
import com.starterkit.auth.auth.api.dto.LoginResponse;
import com.starterkit.auth.auth.api.dto.RefreshTokenRequest;
import com.starterkit.auth.auth.api.dto.RegisterRequest;
import com.starterkit.auth.auth.api.dto.RegisterResponse;
import com.starterkit.auth.auth.api.dto.UserResponse;
import com.starterkit.auth.auth.application.exception.LoginException;
import com.starterkit.auth.auth.application.validator.LoginConfigurationValidator;
import com.starterkit.auth.auth.application.validator.LoginConfigurationValidator.IdentifierType;
import com.starterkit.auth.auth.application.validator.RegisterConfigurationValidator;
import com.starterkit.auth.auth.domain.entity.Permission;
import com.starterkit.auth.auth.domain.entity.Role;
import com.starterkit.auth.auth.domain.entity.User;
import com.starterkit.auth.auth.domain.repository.RoleRepository;
import com.starterkit.auth.auth.domain.repository.UserRepository;
import com.starterkit.auth.shared.api.response.ApiCode;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

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
    private final UserCreationService userCreationService;

    @Transactional
    public RegisterResponse register(RegisterRequest request) {

        registerConfigurationValidator.validate(request);

        Role defaultRole = roleRepository.findByName(DEFAULT_ROLE)
                .orElseThrow(() -> new IllegalStateException(
                        "Default role '" + DEFAULT_ROLE + "' not found in database"
                ));

        User savedUser = userCreationService.createUser(
                new UserCreationService.UserCreationData(
                        request.getUsername(),
                        request.getPassword(),
                        request.getEmail(),
                        request.getMobileNumber(),
                        request.getFirstName(),
                        request.getLastName(),
                        defaultRole.getId()
                )
        );

        return RegisterResponse.builder()
                .id(savedUser.getId())
                .username(savedUser.getUsername())
                .email(savedUser.getEmail())
                .mobileNumber(savedUser.getMobileNumber())
                .firstName(savedUser.getFirstName())
                .lastName(savedUser.getLastName())
                .role(savedUser.getRole().getName())
                .build();
    }

    @Transactional
    public LoginResponse login(LoginRequest request) {

        loginConfigurationValidator.validate(request);

        IdentifierType type = loginConfigurationValidator
                .detectIdentifierType(request.getIdentifier());

        User user = switch (type) {
            case EMAIL -> userRepository.findByEmailWithRole(request.getIdentifier())
                    .orElseThrow(() -> new LoginException(ApiCode.INVALID_CREDENTIALS));
            case MOBILE -> userRepository.findByMobileNumberWithRole(request.getIdentifier())
                    .orElseThrow(() -> new LoginException(ApiCode.INVALID_CREDENTIALS));
            case USERNAME -> userRepository.findByUsernameWithRole(request.getIdentifier())
                    .orElseThrow(() -> new LoginException(ApiCode.INVALID_CREDENTIALS));
        };

        if (!user.isEnabled()) {
            throw new LoginException(ApiCode.USER_DISABLED);
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new LoginException(ApiCode.INVALID_CREDENTIALS);
        }

        TokenService.TokenPair tokens = tokenService.generateTokens(user);

        return buildLoginResponse(user, tokens);
    }

    @Transactional
    public LoginResponse refresh(RefreshTokenRequest request) {

        TokenService.TokenPair tokens = tokenService.refresh(request.getRefreshToken());

        var claims = tokenService.getClaims(tokens.accessToken());
        Long userId = Long.parseLong(claims.getSubject());

        User user = userRepository.findByIdWithRole(userId)
                .orElseThrow(() -> new LoginException(ApiCode.INVALID_CREDENTIALS));

        return buildLoginResponse(user, tokens);
    }

    @Transactional(readOnly = true)
    public UserResponse me(Long userId) {

        User user = userRepository.findByIdWithRole(userId)
                .orElseThrow(() -> new LoginException(ApiCode.INVALID_CREDENTIALS));

        return UserResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .mobileNumber(user.getMobileNumber())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .role(user.getRole().getName())
                .enabled(user.isEnabled())
                .build();
    }

    @Transactional
    public void logout(RefreshTokenRequest request) {
        tokenService.logout(request.getRefreshToken());
    }

    private LoginResponse buildLoginResponse(User user, TokenService.TokenPair tokens) {
        List<String> permissions = user.getRole().getPermissions().stream()
                .map(Permission::getCode)
                .toList();

        return LoginResponse.builder()
                .accessToken(tokens.accessToken())
                .refreshToken(tokens.refreshToken())
                .tokenType("Bearer")
                .expiresIn(tokens.expiresInSeconds())
                .userId(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .role(user.getRole().getName())
                .permissions(permissions)
                .build();
    }
}
